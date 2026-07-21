package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.config.SePayProperties;

@Service
public class SePayServiceImpl implements SePayService {

    private static final Logger log = LoggerFactory.getLogger(SePayServiceImpl.class);
    private static final String INVOICE_PREFIX = "S4L-";
    private static final Pattern INVOICE_IN_TEXT = Pattern.compile("S4L[- ]?(\\d{1,8})", Pattern.CASE_INSENSITIVE);
    private static final Pattern HD_IN_TEXT = Pattern.compile("HD[- ]?(\\d{1,8})", Pattern.CASE_INSENSITIVE);
    private static final Pattern ORDER_NUM_IN_TEXT = Pattern.compile("(?i)(?:don\\s*hang|order|ma\\s*don|#)\\s*[-#:]?\\s*(\\d{1,8})");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(15)).build();
    private static final Set<String> SIGNABLE_FIELDS = Set.of(
            "merchant", "operation", "payment_method", "order_amount", "currency",
            "order_invoice_number", "order_description", "customer_id",
            "success_url", "error_url", "cancel_url");

    @Autowired
    private SePayProperties sePayProperties;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.backend.public-url:http://localhost:8080}")
    private String backendPublicUrl;

    /** Chống xử lý trùng webhook biến động số dư (retry SePay). */
    private final Set<Long> processedBankTxIds = ConcurrentHashMap.newKeySet();

    @Override
    public String buildInvoiceNumber(int orderId) {
        return INVOICE_PREFIX + String.format("%08d", orderId);
    }

    @Override
    public Integer parseOrderId(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return null;
        }
        Matcher matcher = INVOICE_IN_TEXT.matcher(invoiceNumber.trim());
        if (matcher.find()) {
            return parsePositiveInt(matcher.group(1));
        }
        if (invoiceNumber.toUpperCase().startsWith(INVOICE_PREFIX)) {
            try {
                return parsePositiveInt(invoiceNumber.substring(INVOICE_PREFIX.length()).replaceFirst("^0+(?!$)", ""));
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> buildCheckoutForm(Orders order, String customerId) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SePay chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        String invoice = buildInvoiceNumber(order.getId());
        long amount = toVndAmount(order.getTotalAmount());
        String frontend = sePayProperties.getFrontendBaseUrl().replaceAll("/$", "");

        if (sePayProperties.hasEnvMismatch()) {
            log.warn(
                    "SePay config mismatch: merchant looks like {} but checkout-url points to {}. Using {}.",
                    sePayProperties.getEffectiveEnv(),
                    sePayProperties.getCheckoutUrl(),
                    sePayProperties.getResolvedCheckoutUrl());
        }

        String checkoutAction = sePayProperties.getResolvedCheckoutUrl();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant", sePayProperties.getMerchantId().trim());
        fields.put("currency", "VND");
        fields.put("order_amount", String.valueOf(amount));
        fields.put("operation", "PURCHASE");
        fields.put("payment_method", "BANK_TRANSFER");
        fields.put("order_description", "Thanh toan don hang Sports4Life #" + order.getId());
        fields.put("order_invoice_number", invoice);
        fields.put("customer_id", customerId != null ? customerId : "guest");
        fields.put("success_url", frontend + "/cart/payment/success?orderId=" + order.getId());
        fields.put("error_url", frontend + "/cart/payment/error?orderId=" + order.getId());
        fields.put("cancel_url", frontend + "/cart/payment/cancel?orderId=" + order.getId());

        String signature = sign(fields, sePayProperties.getSecretKey().trim());
        fields.put("signature", signature);

        log.info("SePay checkout order #{} amount {} env {} action {}",
                order.getId(), amount, sePayProperties.getEffectiveEnv(), checkoutAction);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("action", checkoutAction);
        body.put("method", "POST");
        body.put("fields", fields);
        body.put("invoiceNumber", invoice);
        body.put("amount", amount);
        body.put("env", sePayProperties.getEffectiveEnv());
        body.put("ipnUrl", sePayProperties.ipnUrl(backendPublicUrl));
        return body;
    }

    @Override
    @Transactional
    public void handleIncomingWebhook(Map<String, Object> payload, String secretHeader, String authorizationHeader) {
        if (SePayService.isBankTransferWebhook(payload)) {
            String auth = authorizationHeader != null ? authorizationHeader : secretHeader;
            handleBankWebhook(payload, auth);
            return;
        }
        handleIpn(payload, secretHeader);
    }

    @Override
    @Transactional
    public void handleIpn(Map<String, Object> payload, String secretHeader) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay chưa được cấu hình");
        }
        if (payload == null || payload.isEmpty()) {
            return;
        }
        if (SePayService.isBankTransferWebhook(payload)) {
            log.info("SePay IPN endpoint received bank webhook payload — routing to bank handler");
            handleBankWebhook(payload, secretHeader);
            return;
        }
        if (secretHeader == null || !secretHeader.equals(sePayProperties.getSecretKey().trim())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "IPN unauthorized");
        }

        log.info("SePay IPN received: type={}", payload.get("notification_type"));
        String notificationType = stringValue(payload.get("notification_type"));
        if (!"ORDER_PAID".equalsIgnoreCase(notificationType)) {
            log.info("SePay IPN ignored notification_type={}", notificationType);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = (Map<String, Object>) payload.get("order");
        if (orderData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin đơn hàng trong IPN");
        }

        String invoiceNumber = stringValue(orderData.get("order_invoice_number"));
        Integer orderId = parseOrderId(invoiceNumber);
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã hóa đơn IPN không hợp lệ");
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return;
        }

        long expectedAmount = toVndAmount(order.getTotalAmount());
        long receivedAmount = parseAmount(orderData.get("order_amount"));
        if (receivedAmount <= 0) {
            receivedAmount = parseAmount(payload.get("amount"));
        }
        if (receivedAmount > 0 && receivedAmount < expectedAmount) {
            log.warn("SePay IPN amount insufficient order #{}: expected {} received {}",
                    orderId, expectedAmount, receivedAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Số tiền IPN chưa đủ (expected=" + expectedAmount + ", received=" + receivedAmount + ")");
        }
        if (receivedAmount > expectedAmount) {
            log.info("SePay IPN overpayment order #{}: expected {} received {}",
                    orderId, expectedAmount, receivedAmount);
        }

        markOrderPaid(order, "IPN");
    }

    @Override
    @Transactional
    public void handleBankWebhook(Map<String, Object> payload, String authorizationHeader) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay chưa được cấu hình");
        }
        verifyBankWebhookAuth(authorizationHeader);

        if (payload == null || payload.isEmpty()) {
            log.warn("SePay bank webhook empty body");
            return;
        }

        log.info("SePay bank webhook received id={} code={} content={} amount={}",
                payload.get("id"),
                payload.get("code"),
                payload.get("content"),
                firstValue(payload, "transferAmount", "transfer_amount"));

        String transferType = stringValue(firstValue(payload, "transferType", "transfer_type"));
        if (transferType != null && !"in".equalsIgnoreCase(transferType)) {
            log.info("SePay bank webhook ignored transferType={}", transferType);
            return;
        }

        long txId = parseAmount(payload.get("id"));
        if (txId > 0 && !processedBankTxIds.add(txId)) {
            log.info("SePay bank webhook duplicate txId={}", txId);
            return;
        }

        long transferAmount = parseAmount(firstValue(payload, "transferAmount", "transfer_amount"));
        if (transferAmount <= 0) {
            log.warn("SePay bank webhook missing transferAmount: {}", payload);
            return;
        }

        Integer orderId = resolveOrderIdFromBankPayload(payload);
        if (orderId == null) {
            orderId = fallbackSingleUnpaidOrder(transferAmount);
        }
        if (orderId == null) {
            log.warn("SePay bank webhook could not map order — code={} content={} description={}",
                    payload.get("code"), payload.get("content"), payload.get("description"));
            return;
        }

        Orders order = ordersRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("SePay bank webhook order #{} not found (txId={})", orderId, txId);
            return;
        }
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            log.info("SePay bank webhook order #{} already PAID (txId={})", orderId, txId);
            return;
        }
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            log.warn("SePay bank webhook order #{} cancelled (txId={})", orderId, txId);
            return;
        }

        long expectedAmount = toVndAmount(order.getTotalAmount());
        if (transferAmount < expectedAmount) {
            log.warn("SePay bank webhook insufficient order #{}: expected {} received {} (txId={})",
                    orderId, expectedAmount, transferAmount, txId);
            return;
        }

        markOrderPaid(order, "BANK_WEBHOOK");
        log.info("SePay bank webhook marked order #{} PAID amount {} (txId={})",
                orderId, transferAmount, txId);
    }

    private void verifyBankWebhookAuth(String authorizationHeader) {
        String configured = sePayProperties.getBankWebhookApiKey();
        if (configured == null || configured.isBlank()) {
            return;
        }
        String expected = configured.trim();
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Thiếu Authorization webhook");
        }
        String header = authorizationHeader.trim();
        if (header.equals(expected)
                || header.equals("Bearer " + expected)
                || header.equals("Apikey " + expected)
                || header.equals(sePayProperties.getSecretKey().trim())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook unauthorized");
    }

    private Integer resolveOrderIdFromBankPayload(Map<String, Object> payload) {
        String code = stringValue(payload.get("code"));
        Integer fromCode = resolveOrderIdToken(code);
        if (fromCode != null) {
            return fromCode;
        }

        StringBuilder combined = new StringBuilder();
        for (String field : List.of("content", "description", "subAccount", "code")) {
            String value = stringValue(payload.get(field));
            if (value != null && !value.isBlank()) {
                if (combined.length() > 0) {
                    combined.append(' ');
                }
                combined.append(value);
            }
        }

        Integer fromCombined = resolveOrderIdToken(combined.toString());
        if (fromCombined != null) {
            return fromCombined;
        }

        return null;
    }

    private Integer resolveOrderIdToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();

        Integer fromInvoice = extractOrderIdByPattern(text, INVOICE_IN_TEXT);
        if (fromInvoice != null) {
            return fromInvoice;
        }

        Integer fromHd = extractOrderIdByPattern(text, HD_IN_TEXT);
        if (fromHd != null) {
            return fromHd;
        }

        Matcher orderNum = ORDER_NUM_IN_TEXT.matcher(text);
        if (orderNum.find()) {
            Integer id = parsePositiveInt(orderNum.group(1));
            if (id != null && orderExists(id)) {
                return id;
            }
        }

        if (text.matches("(?i)S4L[- ]?0*\\d+")) {
            Matcher m = INVOICE_IN_TEXT.matcher(text);
            if (m.find()) {
                return parsePositiveInt(m.group(1));
            }
        }

        if (text.matches("\\d{1,8}")) {
            Integer id = parsePositiveInt(text.replaceFirst("^0+(?!$)", ""));
            if (id != null && orderExists(id)) {
                return id;
            }
        }

        return null;
    }

    private Integer extractOrderIdByPattern(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        Integer id = parsePositiveInt(matcher.group(1));
        return id != null && orderExists(id) ? id : null;
    }

    private boolean orderExists(int orderId) {
        return ordersRepository.existsById(orderId);
    }

    private Integer fallbackSingleUnpaidOrder(long transferAmount) {
        Instant since = Instant.now().minus(java.time.Duration.ofHours(48));
        List<Orders> pending = ordersRepository.findRecentUnpaidSePay(since);
        if (pending == null || pending.isEmpty()) {
            return null;
        }

        List<Orders> affordable = pending.stream()
                .filter(o -> transferAmount >= toVndAmount(o.getTotalAmount()))
                .toList();

        if (affordable.size() == 1) {
            log.info("SePay bank webhook fallback: single unpaid SePay order #{} amount {}",
                    affordable.get(0).getId(), transferAmount);
            return affordable.get(0).getId();
        }

        if (affordable.size() > 1) {
            log.warn("SePay bank webhook fallback skipped: {} unpaid SePay orders match amount {}",
                    affordable.size(), transferAmount);
        }
        return null;
    }

    private static Integer parsePositiveInt(String digits) {
        if (digits == null || digits.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(digits.trim());
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Object firstValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            if (payload.containsKey(key) && payload.get(key) != null) {
                return payload.get(key);
            }
        }
        return null;
    }

    private static String normalizeInvoiceToken(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        Matcher matcher = INVOICE_IN_TEXT.matcher(trimmed);
        if (matcher.find()) {
            return INVOICE_PREFIX + String.format("%08d", Integer.parseInt(matcher.group(1)));
        }
        return trimmed.toUpperCase().startsWith(INVOICE_PREFIX) ? trimmed : null;
    }

    private Integer extractOrderIdFromText(String text) {
        return resolveOrderIdToken(text);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> resumeCheckout(int orderId, String username) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getAccount() == null
                || order.getAccount().getUsername() == null
                || !order.getAccount().getUsername().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền thanh toán đơn này");
        }
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã bị hủy");
        }
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Đơn hàng đã được thanh toán");
        }

        String method = order.getPaymentMethod();
        if (method != null && !"SEPAY".equalsIgnoreCase(method.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn này không dùng SePay. Vui lòng liên hệ hỗ trợ nếu cần thanh toán lại.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getId());
        body.put("totalAmount", order.getTotalAmount());
        body.put("paymentStatus", order.getPaymentStatus());
        body.put("sepay", buildCheckoutForm(order, username));
        return body;
    }

    @Override
    @Transactional
    public Map<String, Object> syncPaymentStatus(int orderId, String username) {
        Orders order = requireOwnedOrder(orderId, username);
        Map<String, Object> syncResult = syncFromSePayGateway(order);
        return buildStatusResponse(requireOwnedOrder(orderId, username), syncResult);
    }

    @Override
    @Transactional
    public Map<String, Object> getPaymentStatus(int orderId, String username) {
        Orders order = requireOwnedOrder(orderId, username);

        Map<String, Object> syncResult = Map.of();
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            syncResult = syncFromSePayGateway(order);
            if (Boolean.TRUE.equals(syncResult.get("synced"))) {
                order = requireOwnedOrder(orderId, username);
            }
        }

        return buildStatusResponse(order, syncResult);
    }

    private Orders requireOwnedOrder(int orderId, String username) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getAccount() == null
                || order.getAccount().getUsername() == null
                || !order.getAccount().getUsername().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem đơn hàng này");
        }
        return order;
    }

    private Map<String, Object> buildStatusResponse(Orders order, Map<String, Object> syncResult) {
        long amountRequired = toVndAmount(order.getTotalAmount());
        boolean paid = "PAID".equalsIgnoreCase(order.getPaymentStatus());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getId());
        body.put("paymentStatus", order.getPaymentStatus());
        body.put("orderStatus", order.getOrderStatus());
        body.put("paymentMethod", order.getPaymentMethod());
        body.put("totalAmount", order.getTotalAmount());
        body.put("amountRequired", amountRequired);
        body.put("amountPaid", paid ? amountRequired : 0);
        body.put("invoiceNumber", buildInvoiceNumber(order.getId()));
        body.put("invoiceCode", "HD-" + String.format("%06d", order.getId()));
        body.put("paid", paid);
        body.put("paymentComplete", paid);
        if (!syncResult.isEmpty()) {
            body.put("sepaySync", syncResult);
        }
        if (paid) {
            body.put("message", "Thanh toán hoàn tất");
        } else {
            body.put("message", syncResult.getOrDefault("message",
                    "Chưa nhận được xác nhận chuyển khoản đủ số tiền từ SePay"));
        }
        return body;
    }

    /** Hỏi trực tiếp API SePay — không phụ thuộc IPN/ngrok. */
    private Map<String, Object> syncFromSePayGateway(Orders order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("synced", false);

        if (!sePayProperties.isConfigured()) {
            result.put("message", "SePay chưa được cấu hình");
            return result;
        }
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            result.put("synced", true);
            result.put("message", "Đơn đã thanh toán");
            return result;
        }

        String invoice = buildInvoiceNumber(order.getId());
        try {
            JsonNode remote = fetchRemoteOrder(invoice);
            if (remote == null || remote.isMissingNode()) {
                result.put("message", "SePay Cổng TT chưa ghi nhận đơn " + invoice
                        + ". Tiền có thể đã vào TK ngân hàng — cần cấu hình Webhook biến động số dư trên my.sepay.vn"
                        + " trỏ tới " + sePayProperties.bankWebhookUrl(backendPublicUrl)
                        + " rồi Replay giao dịch.");
                return result;
            }

            String remoteStatus = textOrNull(remote, "order_status");
            result.put("sepayOrderStatus", remoteStatus);
            result.put("sepayOrderId", textOrNull(remote, "order_id"));

            long expectedAmount = toVndAmount(order.getTotalAmount());
            long remoteAmount = parseAmount(textOrNull(remote, "order_amount"));
            result.put("sepayOrderAmount", remoteAmount);

            if (isRemoteOrderPaid(remote)) {
                long paidAmount = resolvePaidAmount(remote, remoteAmount, expectedAmount);
                if (paidAmount > 0 && paidAmount < expectedAmount) {
                    result.put("message", "SePay ghi nhận " + paidAmount + "đ, chưa đủ " + expectedAmount + "đ");
                    return result;
                }
                markOrderPaid(order, "API_SYNC");
                result.put("synced", true);
                result.put("message", "SePay đã xác nhận thanh toán"
                        + (paidAmount > expectedAmount ? " (chuyển dư " + (paidAmount - expectedAmount) + "đ)" : ""));
                return result;
            }

            result.put("message", "SePay Cổng TT vẫn chờ ("
                    + (remoteStatus != null ? remoteStatus : "UNKNOWN")
                    + "). Nếu TK ngân hàng đã nhận tiền: cấu hình Webhook biến động số dư + Replay trên my.sepay.vn.");
            return result;
        } catch (Exception ex) {
            log.warn("SePay sync failed for order #{}: {}", order.getId(), ex.getMessage());
            result.put("message", "Không kết nối được API SePay: " + ex.getMessage());
            return result;
        }
    }

    private JsonNode fetchRemoteOrder(String invoiceNumber) throws Exception {
        String apiBase = sePayProperties.getResolvedApiBaseUrl();
        String encoded = URLEncoder.encode(invoiceNumber, StandardCharsets.UTF_8);

        JsonNode fromList = findOrderInList(apiBase, encoded, invoiceNumber);
        if (fromList != null) {
            String sepayOrderId = textOrNull(fromList, "order_id");
            if (sepayOrderId != null && !sepayOrderId.isBlank()) {
                JsonNode detail = httpGetJson(apiBase + "/v1/order/detail/" + URLEncoder.encode(sepayOrderId, StandardCharsets.UTF_8));
                if (detail != null && detail.has("data") && !detail.get("data").isNull()) {
                    return detail.get("data");
                }
            }
            return fromList;
        }

        JsonNode detail = httpGetJson(apiBase + "/v1/order/detail/" + encoded);
        if (detail != null && detail.has("data") && !detail.get("data").isNull()) {
            return detail.get("data");
        }
        return null;
    }

    private JsonNode findOrderInList(String apiBase, String encodedInvoice, String invoiceNumber) throws Exception {
        JsonNode list = httpGetJson(apiBase + "/v1/order?per_page=20&q=" + encodedInvoice);
        if (list == null || !list.has("data") || !list.get("data").isArray()) {
            return null;
        }
        JsonNode data = list.get("data");
        for (JsonNode row : data) {
            String inv = textOrNull(row, "order_invoice_number");
            if (invoiceNumber.equalsIgnoreCase(inv)) {
                return row;
            }
        }
        return null;
    }

    private JsonNode httpGetJson(String url) throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (sePayProperties.getMerchantId().trim() + ":" + sePayProperties.getSecretKey().trim())
                        .getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Authorization", "Basic " + auth)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "SePay API HTTP " + response.statusCode());
        }
        if (response.body() == null || response.body().isBlank()) {
            return null;
        }
        return objectMapper.readTree(response.body());
    }

    private boolean isRemoteOrderPaid(JsonNode remote) {
        String status = textOrNull(remote, "order_status");
        if ("CAPTURED".equalsIgnoreCase(status)) {
            return true;
        }
        JsonNode transactions = remote.get("transactions");
        if (transactions != null && transactions.isArray()) {
            for (JsonNode tx : transactions) {
                String type = textOrNull(tx, "transaction_type");
                String txStatus = textOrNull(tx, "transaction_status");
                if ("PAYMENT".equalsIgnoreCase(type) && "APPROVED".equalsIgnoreCase(txStatus)) {
                    return true;
                }
            }
        }
        return false;
    }

    private long resolvePaidAmount(JsonNode remote, long remoteAmount, long expectedAmount) {
        if (remoteAmount >= expectedAmount) {
            return remoteAmount;
        }
        JsonNode transactions = remote.get("transactions");
        if (transactions != null && transactions.isArray()) {
            long sum = 0;
            for (JsonNode tx : transactions) {
                if ("PAYMENT".equalsIgnoreCase(textOrNull(tx, "transaction_type"))
                        && "APPROVED".equalsIgnoreCase(textOrNull(tx, "transaction_status"))) {
                    sum += parseAmount(textOrNull(tx, "transaction_amount"));
                }
            }
            if (sum > 0) {
                return sum;
            }
        }
        return remoteAmount > 0 ? remoteAmount : expectedAmount;
    }

    private void markOrderPaid(Orders order, String source) {
        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return;
        }
        order.setPaymentStatus("PAID");
        order.setUpdateDate(Instant.now());
        ordersRepository.save(order);
        log.info("Order #{} marked PAID via {}", order.getId(), source);
    }

    private static String textOrNull(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private long parseAmount(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            String raw = String.valueOf(value).trim().replace(",", "");
            if (raw.isEmpty()) {
                return 0;
            }
            return new BigDecimal(raw).setScale(0, RoundingMode.HALF_UP).longValue();
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private long toVndAmount(BigDecimal amount) {
        if (amount == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tổng tiền không hợp lệ");
        }
        return amount.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private String sign(Map<String, String> fields, String secretKey) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String key = entry.getKey();
            if (!SIGNABLE_FIELDS.contains(key)) {
                continue;
            }
            String value = entry.getValue();
            if (value != null && !value.isBlank()) {
                parts.add(key + "=" + value);
            }
        }
        String signedString = String.join(",", parts);
        log.debug("SePay sign string: {}", signedString);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(signedString.getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(raw);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được chữ ký SePay");
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
