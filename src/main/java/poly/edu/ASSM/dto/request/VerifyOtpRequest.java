package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

	@NotBlank(message = "Email là bắt buộc.")
	@Email(message = "Email không hợp lệ.")
	private String email;

	@NotBlank(message = "Mã OTP là bắt buộc.")
	@Size(min = 4, max = 8, message = "Mã OTP không hợp lệ.")
	private String otp;
}
