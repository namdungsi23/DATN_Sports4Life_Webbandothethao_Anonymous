package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import poly.edu.ASSM.dto.request.CheckoutConfirmRequest;
import poly.edu.ASSM.dto.request.VoucherApplyRequest;

public interface CheckoutService {

    Map<String, Object> confirmCheckout(String username, CheckoutConfirmRequest request);

    Map<String, Object> previewVoucher(String username, VoucherApplyRequest request);

    List<Map<String, Object>> listActiveCarriers();
}
