package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

	@NotBlank(message = "Mật khẩu hiện tại là bắt buộc.")
	@Size(max = 100, message = "Mật khẩu hiện tại không hợp lệ.")
	private String currentPassword;

	@NotBlank(message = "Mật khẩu mới là bắt buộc.")
	@Size(min = 8, max = 100, message = "Mật khẩu mới từ 8–100 ký tự.")
	private String newPassword;

	@NotBlank(message = "Xác nhận mật khẩu là bắt buộc.")
	@Size(max = 100, message = "Xác nhận mật khẩu không hợp lệ.")
	private String confirmPassword;
}
