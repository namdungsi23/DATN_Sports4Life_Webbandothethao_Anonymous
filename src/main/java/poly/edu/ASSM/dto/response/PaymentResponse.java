package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

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
public class PaymentResponse {
    private Integer id;
    private Long userId;
    private Integer orderId;
    private BigDecimal amount;
    private String status;
    private Instant paidAt;
    private Instant createdAt;
    private Integer paymentMethodId;
    private String paymentMethodCode;
    private String paymentMethodName;
}
