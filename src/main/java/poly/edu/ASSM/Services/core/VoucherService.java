package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import poly.edu.ASSM.domain.VoucherDiscountResult;
import poly.edu.ASSM.dto.request.CheckoutCartItemRequest;
import poly.edu.ASSM.dto.request.VoucherRequest;
import poly.edu.ASSM.dto.response.VoucherResponse;

public interface VoucherService {

    Map<String, Object> list(String keyword, Pageable pageable);

    VoucherResponse findById(int id);

    VoucherResponse create(VoucherRequest request);

    VoucherResponse update(int id, VoucherRequest request);

    void delete(int id);

    VoucherResponse toggleActive(int id, boolean active);

    VoucherDiscountResult previewApply(String code, BigDecimal subTotal, BigDecimal shippingFee);

    VoucherDiscountResult applyForCheckout(String code, BigDecimal subTotal, BigDecimal shippingFee);

    void markUsed(VoucherDiscountResult result);

    BigDecimal calculateSubTotal(List<CheckoutCartItemRequest> items);
}
