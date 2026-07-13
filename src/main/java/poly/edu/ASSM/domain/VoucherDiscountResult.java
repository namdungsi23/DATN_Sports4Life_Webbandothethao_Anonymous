package poly.edu.ASSM.domain;

import java.math.BigDecimal;

import poly.edu.ASSM.Entity.Voucher;

public record VoucherDiscountResult(
        Voucher voucher,
        BigDecimal subtotalDiscount,
        BigDecimal shippingDiscount,
        BigDecimal totalDiscount,
        String discountType) {

    public static VoucherDiscountResult none() {
        return new VoucherDiscountResult(null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "NONE");
    }
}
