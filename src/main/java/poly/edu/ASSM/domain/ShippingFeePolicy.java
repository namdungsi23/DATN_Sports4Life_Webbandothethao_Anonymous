package poly.edu.ASSM.domain;

import java.math.BigDecimal;

public final class ShippingFeePolicy {
    public static final BigDecimal FREE_SHIP_THRESHOLD = new BigDecimal("499000");
    public static final BigDecimal DEFAULT_FEE = new BigDecimal("30000");

    private ShippingFeePolicy() {}

    public static BigDecimal calculate(BigDecimal subTotal) {
        if (subTotal == null) return DEFAULT_FEE;
        return subTotal.compareTo(FREE_SHIP_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : DEFAULT_FEE;
    }
}