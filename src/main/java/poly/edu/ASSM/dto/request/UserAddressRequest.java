package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressRequest {

    private Long id;

    @NotNull(message = "UserId không được để trống")
    private Integer userId;

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    private String receiverName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String receiverPhone;

    @NotBlank(message = "Tỉnh/Thành không được để trống")
    @Size(max = 100, message = "Tỉnh/Thành tối đa 100 ký tự")
    private String province;

    @NotBlank(message = "Phường/Xã không được để trống")
    @Size(max = 100, message = "Phường/Xã tối đa 100 ký tự")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressDetail;

    private Boolean isDefault;
}
