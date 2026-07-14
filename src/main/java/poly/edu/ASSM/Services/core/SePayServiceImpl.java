package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
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

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.config.SePayProperties;

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
    public void handleIpn(Map<String, Object> payload, String secretHeader) {
        if (!sePayProperties.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "SePay chưa được cấu hình");
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

        order.setPaymentStatus("PAID");
        order.setUpdateDate(Instant.now());
        ordersRepository.save(order);
        log.info("SePay IPN marked order #{} as PAID (invoice={})", orderId, invoiceNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPaymentStatus(int orderId, String username) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getAccount() == null
                || order.getAccount().getUsername() == null
                || !order.getAccount().getUsername().equalsIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền xem đơn hàng này");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("orderId", order.getId());
        body.put("paymentStatus", order.getPaymentStatus());
        body.put("orderStatus", order.getOrderStatus());
        body.put("totalAmount", order.getTotalAmount());
        body.put("invoiceNumber", buildInvoiceNumber(order.getId()));
        body.put("paid", "PAID".equalsIgnoreCase(order.getPaymentStatus()));
        return body;
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
