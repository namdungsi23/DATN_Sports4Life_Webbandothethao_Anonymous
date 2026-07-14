package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {

	/** EMAIL/GMAIL hoặc SMS */
	@Size(max = 20)
	private String verifyChannel;

	private String email;

	@Size(max = 20)
	private String phone;
}
