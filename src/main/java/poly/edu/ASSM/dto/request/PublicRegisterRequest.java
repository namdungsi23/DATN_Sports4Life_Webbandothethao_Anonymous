package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicRegisterRequest {

	@NotBlank(message = "Tên đăng nhập là bắt buộc.")
	@Size(min = 3, max = 30, message = "Tên đăng nhập từ 3–30 ký tự.")
	private String username;

	@NotBlank(message = "Họ tên là bắt buộc.")
	@Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
	private String fullname;

	@NotBlank(message = "Email là bắt buộc.")
	@Email(message = "Email không hợp lệ.")
	private String email;

	@NotBlank(message = "Mật khẩu là bắt buộc.")
	private String password;
}
