package poly.edu.ASSM.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoucherApplyRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String voucherCode;

    @NotEmpty(message = "Giỏ hàng không được để trống")
    @Valid
    private List<CheckoutCartItemRequest> items;
}
