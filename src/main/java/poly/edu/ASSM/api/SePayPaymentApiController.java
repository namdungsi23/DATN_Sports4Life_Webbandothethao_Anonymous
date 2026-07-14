package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}
