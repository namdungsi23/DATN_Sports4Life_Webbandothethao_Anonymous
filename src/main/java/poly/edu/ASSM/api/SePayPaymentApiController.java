package poly.edu.ASSM.api;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.SePayService;
import poly.edu.ASSM.dto.response.SePayPaymentStatusResponse;

@RestController
@RequestMapping("/api/checkout/sepay")
public class SePayPaymentApiController {

    @Autowired
    private SePayService sePayService;

    @GetMapping("/status/{orderId}")
    public SePayPaymentStatusResponse status(Principal principal, @PathVariable int orderId) {
        return sePayService.getPaymentStatus(orderId, principal.getName());
    }

    /** Gọi ngay khi SePay redirect success — đồng bộ và hoàn tất đơn. */
    @PostMapping("/complete/{orderId}")
    public SePayPaymentStatusResponse complete(
            Principal principal,
            @PathVariable int orderId,
            @RequestParam(name = "gateway", defaultValue = "false") boolean fromGatewayReturn) {
        return sePayService.completePayment(orderId, principal.getName(), fromGatewayReturn);
    }
}
