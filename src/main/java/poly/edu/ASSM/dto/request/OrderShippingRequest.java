package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderShippingRequest {

    @NotBlank(message = "Họ và tên người nhận không được để trống")
    @Size(max = 100, message = "Họ và tên tối đa 100 ký tự")
    private String receiverName;

    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String receiverPhone;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 100, message = "Thành phố tối đa 100 ký tự")
    private String province;

    @NotBlank(message = "Phường không được để trống")
    @Size(max = 100, message = "Phường tối đa 100 ký tự")
    private String ward;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String addressDetail;
}
