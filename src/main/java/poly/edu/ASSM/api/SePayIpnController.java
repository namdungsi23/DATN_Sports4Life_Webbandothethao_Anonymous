package poly.edu.ASSM.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.dto.response.SePayPaymentStatusResponse;
import poly.edu.ASSM.Services.core.SePayService;

@RestController
@RequestMapping({"/api/public/sepay", "/api/sepay"})
public class SePayIpnController {

    private static final Logger log = LoggerFactory.getLogger(SePayIpnController.class);

    @Autowired
    private SePayService sePayService;

    /** SePay / dashboard có thể ping GET để kiểm tra URL public. */
    @GetMapping({"/ipn", "/ipn/"})
    public Map<String, Object> ipnHealth() {
        return Map.of("success", true, "service", "sepay-ipn");
    }

    @PostMapping({"/ipn", "/ipn/"})
    public ResponseEntity<Map<String, Object>> ipn(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String secret = resolveSecret(secretKey, authorization);
        return processCallback("IPN", payload, () -> sePayService.handleIpn(payload, secret));
    }

    /** Webhook chuyển khoản QR ngân hàng (SePay → merchant khi có giao dịch vào). */
    @GetMapping({"/webhook", "/webhook/"})
    public Map<String, Object> webhookHealth() {
        return Map.of("success", true, "service", "sepay-webhook");
    }

    @PostMapping({"/webhook", "/webhook/"})
    public ResponseEntity<Map<String, Object>> webhook(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String secret = resolveSecret(secretKey, authorization);
        return processCallback("webhook", payload, () -> sePayService.handleBankWebhook(payload, secret));
    }

    /** Hoàn tất thanh toán khi quay về từ SePay — không cần JWT. */
    @PostMapping("/gateway-complete/{orderId}")
    public SePayPaymentStatusResponse gatewayComplete(
            @PathVariable int orderId,
            @RequestParam("token") String token) {
        return sePayService.completePaymentFromGateway(orderId, token);
    }

    private ResponseEntity<Map<String, Object>> processCallback(
            String kind,
            Map<String, Object> payload,
            Runnable handler) {
        try {
            if (payload == null || payload.isEmpty()) {
                log.warn("SePay {} ping with empty body", kind);
                return ResponseEntity.ok(Map.of("success", true, "message", kind + "-ready"));
            }
            log.info("SePay {} received: keys={}", kind, payload.keySet());
            if ("IPN".equals(kind)) {
                log.info("SePay IPN notification_type={}, invoice={}",
                        payload.get("notification_type"),
                        extractInvoice(payload));
            } else {
                log.info("SePay webhook transferType={}, code={}, amount={}",
                        payload.get("transferType"),
                        payload.get("code"),
                        payload.get("transferAmount"));
            }
            handler.run();
            log.info("SePay {} processed OK", kind);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (ResponseStatusException ex) {
            log.warn("SePay {} rejected: {} {}", kind, ex.getStatusCode(), ex.getReason());
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("success", false, "message", ex.getReason()));
        } catch (Exception ex) {
            log.error("SePay {} error", kind, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "internal-error"));
        }
    }

    private String resolveSecret(String secretKey, String authorization) {
        if (secretKey != null && !secretKey.isBlank()) {
            return secretKey;
        }
        return authorization;
    }

    @SuppressWarnings("unchecked")
    private String extractInvoice(Map<String, Object> payload) {
        Object order = payload.get("order");
        if (order instanceof Map<?, ?> orderMap) {
            Object invoice = orderMap.get("order_invoice_number");
            return invoice != null ? String.valueOf(invoice) : null;
        }
        return null;
    }
}
