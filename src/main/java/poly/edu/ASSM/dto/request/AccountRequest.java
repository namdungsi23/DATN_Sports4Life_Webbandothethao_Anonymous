package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequest {

    private Long id;

    @NotBlank(message = "Username không được để trống")
    @Size(max = 30, message = "Username tối đa 30 ký tự")
    private String username;

    @Size(max = 100, message = "Mật khẩu tối đa 100 ký tự")
    private String password;

    @Size(max = 50, message = "Họ tên tối đa 50 ký tự")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @Size(max = 100, message = "Avatar tối đa 100 ký tự")
    private String avatar;

    private Boolean isActive;
    private Boolean admin;
    private Boolean superAdmin;
}
