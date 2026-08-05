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
public class SePayPaymentStatusResponse {
    private Integer orderId;
    private String paymentStatus;
    private String orderStatus;
    private BigDecimal totalAmount;
    private String invoiceNumber;
    private Boolean paid;
}
