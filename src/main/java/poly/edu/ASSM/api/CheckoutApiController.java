package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.CheckoutService;
import poly.edu.ASSM.dto.request.CheckoutConfirmRequest;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {

    @Autowired
    private CheckoutService checkoutService;

    @PostMapping("/confirm")
    public Map<String, Object> confirm(Principal principal, @Valid @RequestBody CheckoutConfirmRequest request) {
        return checkoutService.confirmCheckout(principal.getName(), request);
    }
}
