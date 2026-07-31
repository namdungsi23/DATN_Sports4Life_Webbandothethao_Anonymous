package poly.edu.ASSM.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.PasswordResetService;
import poly.edu.ASSM.dto.request.ForgotPasswordRequest;
import poly.edu.ASSM.dto.request.ResetPasswordRequest;
import poly.edu.ASSM.dto.request.VerifyOtpRequest;

@RestController
@RequestMapping("/api/public")
public class PasswordResetApiController {

	@Autowired
	private PasswordResetService passwordResetService;

	@PostMapping("/forgot-password")
	public ResponseEntity<Map<String, Object>> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
		return ResponseEntity.ok(passwordResetService.requestReset(
				request.getVerifyChannel(),
				request.getEmail(),
				request.getPhone()));
	}

	@PostMapping("/verify-otp")
	public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
		return ResponseEntity.ok(passwordResetService.verifyOtp(
				request.getVerifyChannel(),
				request.getEmail(),
				request.getPhone(),
				request.getOtp()));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, Object>> reset(@Valid @RequestBody ResetPasswordRequest request) {
		return ResponseEntity.ok(passwordResetService.resetPassword(
				request.getToken(),
				request.getNewPassword(),
				request.getConfirmPassword()));
	}
}
