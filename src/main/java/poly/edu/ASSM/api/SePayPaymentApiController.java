package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.SePayService;

@RestController
@RequestMapping("/api/checkout/sepay")
public class SePayPaymentApiController {

    @Autowired
    private SePayService sePayService;

    @GetMapping("/status/{orderId}")
    public Map<String, Object> status(Principal principal, @PathVariable int orderId) {
        return sePayService.getPaymentStatus(orderId, principal.getName());
    }

    /** Tiếp tục thanh toán SePay cho đơn UNPAID đã tạo trước đó. */
    @PostMapping("/pay/{orderId}")
    public Map<String, Object> resume(Principal principal, @PathVariable int orderId) {
        return sePayService.resumeCheckout(orderId, principal.getName());
    }

    /** Kiểm tra lại trạng thái thanh toán trực tiếp với API SePay. */
    @PostMapping("/sync/{orderId}")
    public Map<String, Object> sync(Principal principal, @PathVariable int orderId) {
        return sePayService.syncPaymentStatus(orderId, principal.getName());
    }
}
