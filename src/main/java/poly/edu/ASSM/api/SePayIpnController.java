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

@RestController
@RequestMapping("/api/public/sepay")
public class SePayIpnController {

    private static final Logger log = LoggerFactory.getLogger(SePayIpnController.class);

    @Autowired
    private SePayService sePayService;

    /** SePay / dashboard có thể ping GET để kiểm tra URL public. */
    @GetMapping("/ipn")
    public Map<String, Object> ipnHealth() {
        return Map.of("success", true, "service", "sepay-ipn");
    }

    @PostMapping("/ipn")
    public ResponseEntity<Map<String, Object>> ipn(
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-Secret-Key", required = false) String secretKey) {
        try {
            if (payload == null || payload.isEmpty()) {
                log.warn("SePay IPN ping with empty body");
                return ResponseEntity.ok(Map.of("success", true, "message", "ipn-ready"));
            }
            sePayService.handleIpn(payload, secretKey);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (ResponseStatusException ex) {
            log.warn("SePay IPN rejected: {} {}", ex.getStatusCode(), ex.getReason());
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("success", false, "message", ex.getReason()));
        } catch (Exception ex) {
            log.error("SePay IPN error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "internal-error"));
        }
    }
}
