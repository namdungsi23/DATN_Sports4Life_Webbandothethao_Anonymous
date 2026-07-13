package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.CheckoutService;
import poly.edu.ASSM.dto.request.CheckoutConfirmRequest;
import poly.edu.ASSM.dto.request.VoucherApplyRequest;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {

    @Autowired
    private CheckoutService checkoutService;

    @GetMapping("/carriers")
    public List<Map<String, Object>> carriers() {
        return checkoutService.listActiveCarriers();
    }

    @PostMapping("/confirm")
    public Map<String, Object> confirm(Principal principal, @Valid @RequestBody CheckoutConfirmRequest request) {
        return checkoutService.confirmCheckout(principal.getName(), request);
    }

    @PostMapping("/vouchers/apply")
    public Map<String, Object> applyVoucher(Principal principal, @Valid @RequestBody VoucherApplyRequest request) {
        return checkoutService.previewVoucher(principal.getName(), request);
    }
}
