package poly.edu.ASSM.Services.core;

import java.util.List;

import poly.edu.ASSM.dto.request.CheckoutConfirmRequest;
import poly.edu.ASSM.dto.request.VoucherApplyRequest;
import poly.edu.ASSM.dto.response.CarrierResponse;
import poly.edu.ASSM.dto.response.CheckoutConfirmResponse;
import poly.edu.ASSM.dto.response.VoucherPreviewResponse;

public interface CheckoutService {

    CheckoutConfirmResponse confirmCheckout(String username, CheckoutConfirmRequest request);

    VoucherPreviewResponse previewVoucher(String username, VoucherApplyRequest request);

    List<CarrierResponse> listActiveCarriers();
}
