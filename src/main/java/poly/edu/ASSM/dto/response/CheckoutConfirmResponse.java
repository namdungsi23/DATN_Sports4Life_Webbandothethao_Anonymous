package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutConfirmResponse {
    private String message;
    private Integer orderId;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String voucherCode;
    private String discountType;
    private OrderAddressResponse orderAddress;
    private CustomerAddressResponse savedAddress;
    private String paymentMethod;
    private Boolean paymentPending;
    private SePayCheckoutFormResponse sepay;
    /** Token xác nhận thanh toán SePay khi redirect (không cần JWT). */
    private String paymentCompletionToken;
}
