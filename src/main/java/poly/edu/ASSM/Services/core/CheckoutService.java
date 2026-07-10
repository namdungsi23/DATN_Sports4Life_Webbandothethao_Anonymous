package poly.edu.ASSM.Services.core;

import java.util.Map;

import poly.edu.ASSM.dto.request.CheckoutConfirmRequest;

public interface CheckoutService {

    Map<String, Object> confirmCheckout(String username, CheckoutConfirmRequest request);
}
