package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

	@NotBlank(message = "Mật khẩu hiện tại là bắt buộc.")
	private String currentPassword;

	@NotBlank(message = "Mật khẩu mới là bắt buộc.")
	private String newPassword;

	@NotBlank(message = "Xác nhận mật khẩu là bắt buộc.")
	private String confirmPassword;
}
