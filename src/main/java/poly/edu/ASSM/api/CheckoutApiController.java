package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.List;

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
import poly.edu.ASSM.dto.response.CarrierResponse;
import poly.edu.ASSM.dto.response.CheckoutConfirmResponse;
import poly.edu.ASSM.dto.response.VoucherPreviewResponse;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutApiController {

    @Autowired
    private CheckoutService checkoutService;

    @GetMapping("/carriers")
    public List<CarrierResponse> carriers() {
        return checkoutService.listActiveCarriers();
    }

    @PostMapping("/confirm")
    public CheckoutConfirmResponse confirm(Principal principal, @Valid @RequestBody CheckoutConfirmRequest request) {
        return checkoutService.confirmCheckout(principal.getName(), request);
    }

    @PostMapping("/vouchers/apply")
    public VoucherPreviewResponse applyVoucher(Principal principal, @Valid @RequestBody VoucherApplyRequest request) {
        return checkoutService.previewVoucher(principal.getName(), request);
    }
}
