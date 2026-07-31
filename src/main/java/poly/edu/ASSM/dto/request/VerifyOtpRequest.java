package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyOtpRequest {

	/** EMAIL/GMAIL hoặc SMS */
	@Size(max = 20)
	private String verifyChannel;

	private String email;

	@Size(max = 20)
	private String phone;

	@NotBlank(message = "Mã OTP là bắt buộc.")
	@Size(min = 4, max = 8, message = "Mã OTP không hợp lệ.")
	private String otp;
}
