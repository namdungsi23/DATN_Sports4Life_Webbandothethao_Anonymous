package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDetailRequest {

    private Integer id;

    @NotNull(message = "OrderId không được để trống")
    private Integer orderId;

    @NotNull(message = "Giá không được để trống")
    private Double price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
