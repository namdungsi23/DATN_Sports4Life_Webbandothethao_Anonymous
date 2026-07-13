package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutCartItemRequest {

    @NotNull(message = "Mã sản phẩm không được để trống")
    private Long productId;

    /** Nếu null, hệ thống dùng biến thể mặc định của sản phẩm */
    private Long variantId;

    @NotNull(message = "Số lượng không được để trống")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
