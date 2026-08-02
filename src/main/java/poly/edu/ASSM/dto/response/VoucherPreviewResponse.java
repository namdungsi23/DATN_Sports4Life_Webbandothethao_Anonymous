package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherPreviewResponse {
    private Boolean valid;
    private String voucherCode;
    private String voucherName;
    private String discountType;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal subtotalDiscount;
    private BigDecimal shippingDiscount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
}
