package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerAddressRequest {

    @Size(max = 50, message = "Nhãn địa chỉ tối đa 50 ký tự")
    private String label;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100, message = "Thành phố tối đa 100 ký tự")
    private String province;

    @NotBlank(message = "Phường không được để trống")
    @Size(max = 100, message = "Phường tối đa 100 ký tự")
    private String ward;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String addressDetail;

    private Boolean isDefault;
}
