package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentMethodRequest {

    private Integer id;

    @NotBlank(message = "Code không được để trống")
    @Size(max = 50, message = "Code tối đa 50 ký tự")
    private String code;

    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    private String description;
    private Boolean isActive;
}
