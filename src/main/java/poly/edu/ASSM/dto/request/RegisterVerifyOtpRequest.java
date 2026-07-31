package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterVerifyOtpRequest {

	@NotBlank(message = "Tên đăng nhập là bắt buộc.")
	private String username;

	/** EMAIL / GMAIL hoặc SMS */
	@NotBlank(message = "Kênh xác minh là bắt buộc.")
	private String verifyChannel;

	private String email;

	private String phone;

	@NotBlank(message = "Mã OTP là bắt buộc.")
	@Size(min = 4, max = 8, message = "Mã OTP không hợp lệ.")
	private String otp;
}
