package poly.edu.ASSM.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Services.core.SePayService;

/**
 * Webhook biến động số dư ngân hàng (khác IPN Cổng thanh toán).
 * Cấu hình trên my.sepay.vn → Webhooks → URL public + /api/public/sepay/bank-webhook
 */
@RestController
@RequestMapping("/api/public/sepay")
public class SePayBankWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SePayBankWebhookController.class);

    @Autowired
    private SePayService sePayService;

    @GetMapping("/bank-webhook")
    public Map<String, Object> health() {
        return Map.of(
                "success", true,
                "service", "sepay-bank-webhook",
                "hint", "POST JSON giao dịch tiền vào từ SePay Webhooks");
    }

    @PostMapping("/bank-webhook")
    public ResponseEntity<Map<String, Object>> receive(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey) {
        try {
            sePayService.handleIncomingWebhook(payload, secretKey, authorization);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (ResponseStatusException ex) {
            log.warn("SePay bank webhook rejected: {} {}", ex.getStatusCode(), ex.getReason());
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("success", false, "message", ex.getReason()));
        } catch (Exception ex) {
            log.error("SePay bank webhook error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "internal-error"));
        }
    }
}
