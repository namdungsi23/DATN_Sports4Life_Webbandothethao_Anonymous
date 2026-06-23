package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    private Integer id;

    @NotNull(message = "UserId không được để trống")
    private Integer userId;

    @NotNull(message = "OrderId không được để trống")
    private Integer orderId;

    private BigDecimal amount;

    @Size(max = 50, message = "Trạng thái tối đa 50 ký tự")
    private String status;

    private Instant paidAt;

    @NotNull(message = "PaymentMethodId không được để trống")
    private Integer paymentMethodId;
}
