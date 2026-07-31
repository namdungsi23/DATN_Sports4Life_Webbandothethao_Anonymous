package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterResendOtpRequest {

	@NotBlank(message = "Tên đăng nhập là bắt buộc.")
	private String username;

	@NotBlank(message = "Kênh xác minh là bắt buộc.")
	private String verifyChannel;

	private String email;

	private String phone;
}
