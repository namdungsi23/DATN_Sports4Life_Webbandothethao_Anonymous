package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.config.SePayProperties;
import poly.edu.ASSM.dto.response.SePayCheckoutFormResponse;
import poly.edu.ASSM.dto.response.SePayPaymentStatusResponse;
import poly.edu.ASSM.mapper.SePayMapper;

@Service
public class SePayServiceImpl implements SePayService {

    private static final Logger log = LoggerFactory.getLogger(SePayServiceImpl.class);
    private static final String INVOICE_PREFIX = "S4L-";
    private static final Set<String> SIGNABLE_FIELDS = Set.of(
            "merchant", "operation", "payment_method", "order_amount", "currency",
            "order_invoice_number", "order_description", "customer_id",
            "success_url", "error_url", "cancel_url");

    @Autowired
    private SePayProperties sePayProperties;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private SePayMapper sePayMapper;

    @Autowired
    private AdminNotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    @Value("${app.backend.public-url:http://localhost:8080}")
    private String backendPublicUrl;

    @Override
    public String buildInvoiceNumber(int orderId) {
        return INVOICE_PREFIX + String.format("%08d", orderId);
    }

    @Override
    public Integer parseOrderId(String invoiceNumber) {
        if (invoiceNumber == null || !invoiceNumber.startsWith(INVOICE_PREFIX)) {
            return null;
        }
        try {
            return Integer.parseInt(invoiceNumber.substring(INVOICE_PREFIX.length()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SePayCheckoutFormResponse buildCheckoutForm(Orders order, String customerId, String returnBaseUrl) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SePay chưa được cấu hình. Vui lòng liên hệ quản trị viên.");
        }

        String invoice = buildInvoiceNumber(order.getId());
        long amount = toVndAmount(order.getTotalAmount());
        String frontend = resolveFrontendBaseUrl(returnBaseUrl);

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
        fields.put("success_url", frontend + "/cart/payment/sepay?orderId=" + order.getId() + "&gateway=success");
        fields.put("error_url", frontend + "/cart/payment/error?orderId=" + order.getId());
        fields.put("cancel_url", frontend + "/cart/payment/cancel?orderId=" + order.getId());

        String signature = sign(fields, sePayProperties.getSecretKey().trim());
        fields.put("signature", signature);

        log.info("SePay checkout order #{} amount {} env {} action {}",
                order.getId(), amount, sePayProperties.getEffectiveEnv(), checkoutAction);

        return sePayMapper.toCheckoutFormResponse(
                checkoutAction,
                "POST",
                fields,
                invoice,
                amount,
                sePayProperties.getEffectiveEnv(),
                sePayProperties.ipnUrl(backendPublicUrl));
    }

    @Override
    public String createPaymentCompletionToken(int orderId) {
        return signCompletionToken(orderId);
    }

    @Override
    @Transactional
    public SePayPaymentStatusResponse completePaymentFromGateway(int orderId, String completionToken) {
        if (!verifyCompletionToken(orderId, completionToken)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Token thanh toán không hợp lệ");
        }
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            markOrderPaidBySePay(order, buildInvoiceNumber(orderId), "BANK_TRANSFER", "sepay-gateway-return");
            order = ordersRepository.findById(orderId).orElse(order);
        }
        return toStatusResponse(order);
    }

    private String resolveFrontendBaseUrl(String returnBaseUrl) {
        if (returnBaseUrl != null && !returnBaseUrl.isBlank()) {
            return returnBaseUrl.trim().replaceAll("/$", "");
        }
        return sePayProperties.getFrontendBaseUrl().replaceAll("/$", "");
    }

    private String signCompletionToken(int orderId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(completionSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(("S4L-PAY:" + orderId).getBytes(StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không tạo được token thanh toán");
        }
    }

    private boolean verifyCompletionToken(int orderId, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return signCompletionToken(orderId).equals(token.trim());
    }

    private String completionSecret() {
        String sk = sePayProperties.getSecretKey();
        if (sk != null && !sk.isBlank()) {
            return sk.trim();
        }
        return "Sports4Life-SePay-Completion-Fallback";
    }

    @Override
    @Transactional
    public void handleIpn(Map<String, Object> payload, String secretHeader) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay chưa được cấu hình");
        }
        verifyIpnSecret(secretHeader);

        String notificationType = stringValue(payload.get("notification_type"));
        log.info("SePay IPN received: type={}, invoice={}",
                notificationType,
                extractInvoiceFromPayload(payload));

        if ("TRANSACTION_VOID".equalsIgnoreCase(notificationType)) {
            handleTransactionVoid(payload);
            return;
        }
        if (!"ORDER_PAID".equalsIgnoreCase(notificationType)) {
            log.info("SePay IPN ignored notification_type={}", notificationType);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = (Map<String, Object>) payload.get("order");
        if (orderData == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu thông tin đơn hàng trong IPN");
        }

        String sepayOrderStatus = stringValue(orderData.get("order_status"));
        if (sepayOrderStatus != null && !"CAPTURED".equalsIgnoreCase(sepayOrderStatus)) {
            log.warn("SePay IPN order_status={} — bỏ qua", sepayOrderStatus);
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> transactionData = (Map<String, Object>) payload.get("transaction");
        if (transactionData != null) {
            String txStatus = stringValue(transactionData.get("transaction_status"));
            if (txStatus != null && !"APPROVED".equalsIgnoreCase(txStatus)) {
                log.warn("SePay IPN transaction_status={} — bỏ qua", txStatus);
                return;
            }
        }

        String invoiceNumber = stringValue(orderData.get("order_invoice_number"));
        Integer orderId = parseOrderId(invoiceNumber);
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã hóa đơn IPN không hợp lệ: " + invoiceNumber);
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            log.info("SePay IPN order #{} đã PAID — idempotent skip", orderId);
            return;
        }

        long expectedAmount = toVndAmount(order.getTotalAmount());
        long ipnAmount = parseIpnAmount(orderData.get("order_amount"));
        if (ipnAmount > 0 && ipnAmount != expectedAmount) {
            log.error("SePay IPN amount mismatch order #{}: expected={}, received={}",
                    orderId, expectedAmount, ipnAmount);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số tiền IPN không khớp đơn hàng");
        }

        String paymentMethod = transactionData != null
                ? stringValue(transactionData.get("payment_method"))
                : null;
        String txId = transactionData != null
                ? stringValue(transactionData.get("transaction_id"))
                : null;

        markOrderPaidBySePay(order, invoiceNumber, paymentMethod, txId);
    }

    /** SePay đã xác nhận tiền → chỉ cập nhật PAID; admin xác nhận đơn thủ công. */
    private void markOrderPaidBySePay(
            Orders order,
            String invoiceNumber,
            String paymentMethod,
            String txId) {
        order.setPaymentStatus("PAID");
        order.setUpdateDate(Instant.now());
        ordersRepository.save(order);

        log.info("SePay order #{} → PAID (invoice={}, method={}, txId={}) — chờ admin xác nhận đơn",
                order.getId(),
                invoiceNumber,
                paymentMethod,
                txId);

        notifyPaymentConfirmed(order, invoiceNumber, paymentMethod);
    }

    /** Xử lý webhook chuyển khoản QR (SePay gửi khi khách quét QR/chuyển khoản). */
    @Override
    @Transactional
    public void handleBankWebhook(Map<String, Object> payload, String authHeader) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay chưa được cấu hình");
        }
        verifyWebhookSecret(authHeader);

        if (!"in".equalsIgnoreCase(stringValue(payload.get("transferType")))) {
            log.info("SePay webhook ignored transferType={}", payload.get("transferType"));
            return;
        }

        String code = stringValue(payload.get("code"));
        if (code == null || code.isBlank()) {
            String content = stringValue(payload.get("content"));
            code = extractInvoiceFromContent(content);
        }
        Integer orderId = parseOrderId(code);
        if (orderId == null) {
            log.info("SePay webhook không khớp mã đơn: code={}", code);
            return;
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            log.info("SePay webhook order #{} đã PAID — idempotent skip", orderId);
            return;
        }

        long expectedAmount = toVndAmount(order.getTotalAmount());
        long transferAmount = parseIpnAmount(payload.get("transferAmount"));
        if (transferAmount > 0 && transferAmount < expectedAmount) {
            log.warn("SePay webhook order #{} underpaid: expected={}, received={}",
                    orderId, expectedAmount, transferAmount);
            return;
        }

        markOrderPaidBySePay(order, buildInvoiceNumber(orderId), "BANK_TRANSFER", stringValue(payload.get("id")));
    }

    private void handleTransactionVoid(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = (Map<String, Object>) payload.get("order");
        if (orderData == null) {
            return;
        }
        String invoiceNumber = stringValue(orderData.get("order_invoice_number"));
        Integer orderId = parseOrderId(invoiceNumber);
        if (orderId == null) {
            return;
        }
        ordersRepository.findById(orderId).ifPresent(order -> {
            if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                log.warn("SePay TRANSACTION_VOID for paid order #{} — cần xử lý thủ công", orderId);
                return;
            }
            order.setPaymentStatus("UNPAID");
            order.setUpdateDate(Instant.now());
            ordersRepository.save(order);
            log.info("SePay void reverted order #{} to UNPAID", orderId);
        });
    }

    private void notifyPaymentConfirmed(
            Orders order,
            String invoiceNumber,
            String paymentMethod) {
        Accounts account = order.getAccount();
        String username = account != null ? account.getUsername() : null;
        String amountLabel = order.getTotalAmount() != null
                ? order.getTotalAmount().toPlainString()
                : "0";
        String methodLabel = paymentMethod != null && !paymentMethod.isBlank()
                ? paymentMethod
                : "SePay QR";
        String adminMsg = "Đơn #" + order.getId() + " (" + invoiceNumber + ") đã thanh toán "
                + amountLabel + "đ qua " + methodLabel + " — vui lòng xác nhận đơn.";
        String userMsg = "Đơn #" + order.getId() + " đã thanh toán thành công — shop sẽ xác nhận đơn sớm.";

        try {
            notificationService.notifyPanelUsers(
                    "Thanh toán SePay #" + order.getId(),
                    adminMsg,
                    "/admin/order/" + order.getId());
            if (username != null) {
                notificationService.notifyUser(
                        username,
                        "Thanh toán thành công",
                        userMsg,
                        "/profile?tab=orders");
            }
        } catch (Exception ex) {
            log.warn("Không gửi được thông báo thanh toán order #{}: {}", order.getId(), ex.getMessage());
        }
    }

    private void verifyIpnSecret(String secretHeader) {
        verifyCallbackSecret(secretHeader, sePayProperties.getSecretKey().trim(), "IPN");
    }

    private void verifyWebhookSecret(String authHeader) {
        String gatewaySecret = sePayProperties.getSecretKey().trim();
        try {
            verifyCallbackSecret(authHeader, gatewaySecret, "webhook");
            return;
        } catch (ResponseStatusException ignored) {
            /* thử API key webhook ngân hàng */
        }
        String webhookKey = sePayProperties.getWebhookApiKey();
        if (webhookKey != null && !webhookKey.isBlank()) {
            verifyCallbackSecret(authHeader, webhookKey.trim(), "webhook");
            return;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook unauthorized — secret không khớp");
    }

    private void verifyCallbackSecret(String secretHeader, String configured, String kind) {
        if (secretHeader == null || secretHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, kind + " unauthorized — thiếu header xác thực");
        }
        String received = secretHeader.trim();
        if (received.regionMatches(true, 0, "Bearer ", 0, 7)) {
            received = received.substring(7).trim();
        }
        if (received.regionMatches(true, 0, "Apikey ", 0, 7)) {
            received = received.substring(7).trim();
        }
        if (!configured.equals(received)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, kind + " unauthorized — secret không khớp");
        }
    }

    private String extractInvoiceFromPayload(Map<String, Object> payload) {
        @SuppressWarnings("unchecked")
        Map<String, Object> orderData = (Map<String, Object>) payload.get("order");
        return orderData != null ? stringValue(orderData.get("order_invoice_number")) : null;
    }

    private String extractInvoiceFromContent(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("S4L-\\d{8}", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(content);
        return matcher.find() ? matcher.group().toUpperCase() : null;
    }

    private long parseIpnAmount(Object value) {
        if (value == null) {
            return -1;
        }
        try {
            return new BigDecimal(String.valueOf(value))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    @Override
    @Transactional
    public SePayPaymentStatusResponse completePayment(int orderId, String username, boolean fromGatewayReturn) {
        Orders order = requireOwnedOrder(orderId, username);
        String invoice = buildInvoiceNumber(orderId);

        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            syncPaymentFromSePayApi(order, true);
            order = ordersRepository.findById(orderId).orElse(order);
        }

        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus()) && fromGatewayReturn) {
            log.info("SePay gateway redirect success order #{} — hoàn tất đơn (API sync chưa kịp)", orderId);
            markOrderPaidBySePay(order, invoice, "BANK_TRANSFER", "sepay-gateway-success");
            order = ordersRepository.findById(orderId).orElse(order);
        }

        return toStatusResponse(order);
    }

    @Override
    @Transactional
    public SePayPaymentStatusResponse getPaymentStatus(int orderId, String username) {
        Orders order = requireOwnedOrder(orderId, username);
        if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            syncPaymentFromSePayApi(order, false);
            order = ordersRepository.findById(orderId).orElse(order);
        }
        return toStatusResponse(order);
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

    private SePayPaymentStatusResponse toStatusResponse(Orders order) {
        return sePayMapper.toPaymentStatusResponse(
                order.getId(),
                order.getPaymentStatus(),
                order.getOrderStatus(),
                order.getTotalAmount(),
                buildInvoiceNumber(order.getId()),
                "PAID".equalsIgnoreCase(order.getPaymentStatus()));
    }

    /**
     * Fallback khi IPN chưa tới (vd. dev localhost): hỏi trực tiếp SePay API
     * theo mã hóa đơn S4L-XXXXXXXX.
     */
    private void syncPaymentFromSePayApi(Orders order, boolean aggressive) {
        if (!sePayProperties.isConfigured()) {
            return;
        }
        String invoice = buildInvoiceNumber(order.getId());
        try {
            JsonNode sepayOrder = fetchSePayOrderByInvoice(invoice, order.getId(), aggressive);
            if (sepayOrder == null || sepayOrder.isNull() || sepayOrder.isMissingNode()) {
                log.info("SePay sync order #{}: chưa thấy CAPTURED trên gateway (invoice={})", order.getId(), invoice);
                return;
            }
            if (!isSePayOrderPaid(sepayOrder)) {
                log.info("SePay sync order #{}: status={} (chưa thanh toán)",
                        order.getId(), stringValue(sepayOrder.get("order_status")));
                return;
            }

            Orders fresh = ordersRepository.findById(order.getId()).orElse(order);
            if ("PAID".equalsIgnoreCase(fresh.getPaymentStatus())) {
                return;
            }

            long expectedAmount = toVndAmount(fresh.getTotalAmount());
            long sepayAmount = parseIpnAmount(stringValue(sepayOrder.get("order_amount")));
            if (sepayAmount > 0 && sepayAmount != expectedAmount) {
                log.warn("SePay sync order #{} amount mismatch: expected={}, sepay={}",
                        fresh.getId(), expectedAmount, sepayAmount);
                return;
            }

            String paymentMethod = null;
            String txId = null;
            JsonNode transactions = sepayOrder.get("transactions");
            if (transactions != null && transactions.isArray() && !transactions.isEmpty()) {
                JsonNode tx = transactions.get(0);
                paymentMethod = stringValue(tx.get("payment_method"));
                txId = stringValue(tx.get("transaction_id"));
                if (txId == null) {
                    txId = stringValue(tx.get("id"));
                }
            }

            log.info("SePay sync order #{}: CAPTURED — cập nhật PAID", fresh.getId());
            markOrderPaidBySePay(fresh, invoice, paymentMethod, txId);
        } catch (Exception ex) {
            log.warn("SePay sync order #{} failed: {}", order.getId(), ex.getMessage(), ex);
        }
    }

    private boolean isSePayOrderPaid(JsonNode sepayOrder) {
        if ("CAPTURED".equalsIgnoreCase(stringValue(sepayOrder.get("order_status")))) {
            return true;
        }
        JsonNode transactions = sepayOrder.get("transactions");
        if (transactions != null && transactions.isArray()) {
            for (JsonNode tx : transactions) {
                if ("APPROVED".equalsIgnoreCase(stringValue(tx.get("transaction_status")))) {
                    return true;
                }
            }
        }
        return false;
    }

    private JsonNode fetchSePayOrderByInvoice(String invoice, int orderId, boolean aggressive) throws Exception {
        List<String> candidates = new ArrayList<>();
        candidates.add(invoice);
        candidates.add(invoice.toUpperCase());
        candidates.add(String.valueOf(orderId));
        candidates.add("S4L-" + orderId);
        candidates.add("S4L-" + String.format("%08d", orderId));
        if (aggressive) {
            candidates.add("Sports4Life");
            candidates.add("#" + orderId);
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            JsonNode fromDetail = fetchSePayOrderDetail(candidate);
            if (fromDetail != null && isSePayOrderPaid(fromDetail)) {
                return fromDetail;
            }
        }

        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            JsonNode fromList = searchSePayOrders(candidate, invoice, orderId);
            if (fromList != null) {
                return fromList;
            }
        }

        if (aggressive) {
            JsonNode recent = searchSePayOrders(null, invoice, orderId);
            if (recent != null) {
                return recent;
            }
        }
        return null;
    }

    private JsonNode fetchSePayOrderDetail(String idOrInvoice) throws Exception {
        String encoded = URLEncoder.encode(idOrInvoice, StandardCharsets.UTF_8);
        String detailUrl = sePayProperties.getApiBaseUrl() + "/v1/order/detail/" + encoded;
        JsonNode detail = sepayApiGet(detailUrl);
        JsonNode data = detail != null ? detail.get("data") : null;
        if (data != null && !data.isNull() && !data.isMissingNode()) {
            return data;
        }
        return null;
    }

    private JsonNode searchSePayOrders(String keyword, String matchInvoice, int orderId) throws Exception {
        String base = sePayProperties.getApiBaseUrl() + "/v1/order?per_page=30";
        String listUrl = keyword == null || keyword.isBlank()
                ? base + "&sort=created_at:desc"
                : base + "&q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
        JsonNode list = sepayApiGet(listUrl);
        JsonNode rows = list != null ? list.get("data") : null;
        if (rows == null || !rows.isArray()) {
            return null;
        }
        for (JsonNode row : rows) {
            if (!isSePayOrderPaid(row) || !matchesLocalOrder(row, matchInvoice, orderId)) {
                continue;
            }
            return row;
        }
        return null;
    }

    private boolean matchesLocalOrder(JsonNode row, String matchInvoice, int orderId) {
        String rowInvoice = stringValue(row.get("order_invoice_number"));
        if (matchInvoice != null && matchInvoice.equalsIgnoreCase(rowInvoice)) {
            return true;
        }
        Integer parsed = parseOrderId(rowInvoice);
        if (parsed != null && parsed == orderId) {
            return true;
        }
        String desc = stringValue(row.get("order_description"));
        return desc != null && desc.contains("#" + orderId);
    }

    private JsonNode sepayApiGet(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(12))
                .header("Authorization", sePayProperties.basicAuthHeaderValue())
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            return null;
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "SePay API HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
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
