package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.jsonwebtoken.Claims;
import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Services.util.JwtService;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.security.OtpService;
import poly.edu.ASSM.security.PasswordPolicy;

@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	/** Giới hạn chờ SMTP để Vite proxy không bị socket hang up */
	private static final long SMTP_WAIT_SECONDS = 8;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private OtpService otpService;

	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Value("${app.mail.from:noreply@sports4life.local}")
	private String mailFrom;

	/** Dev/DATN: trả OTP trong JSON khi chưa gửi được email */
	@Value("${app.security.password-reset.dev-expose-otp:true}")
	private boolean devExposeOtp;

	/**
	 * Gửi OTP 6 số về email. Message chung — không lộ email có tồn tại hay không.
	 */
	public Map<String, Object> requestReset(String email) {
		String normalized = email == null ? "" : email.trim();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("message", "Nếu email tồn tại, mã OTP đã được gửi. Vui lòng kiểm tra hộp thư.");
		body.put("otpTtlSeconds", otpService.getTtlSeconds());

		Accounts account = accountRepository.findFirstByEmailIgnoreCase(normalized);
		if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
			return body;
		}

		String otp = otpService.generateAndStore(normalized);
		boolean mailed = trySendOtpMailBounded(account.getEmail(), account.getUsername(), otp);
		if (!mailed) {
			log.info("Password reset OTP for {} (mail not sent): {}", account.getUsername(), otp);
			if (devExposeOtp) {
				body.put("devOtp", otp);
				body.put("devNote", "SMTP chưa gửi được — dùng OTP này để demo (tắt app.security.password-reset.dev-expose-otp=false).");
			}
		}

		return body;
	}

	/** Xác minh OTP → cấp resetToken (JWT typ=reset) để đổi mật khẩu. */
	public Map<String, Object> verifyOtp(String email, String otp) {
		String normalized = email == null ? "" : email.trim();
		Accounts account = accountRepository.findFirstByEmailIgnoreCase(normalized);
		if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
			throw new InvalidInputException("Mã OTP không hợp lệ hoặc đã hết hạn.");
		}

		OtpService.VerifyResult result = otpService.verify(normalized, otp);
		switch (result) {
			case OK -> {
				/* continue */
			}
			case EXPIRED -> throw new InvalidInputException("Mã OTP đã hết hạn. Vui lòng gửi lại.");
			case TOO_MANY_ATTEMPTS -> throw new InvalidInputException("Nhập sai OTP quá nhiều lần. Vui lòng gửi lại mã mới.");
			case NOT_FOUND, INVALID -> throw new InvalidInputException("Mã OTP không đúng.");
			default -> throw new InvalidInputException("Mã OTP không hợp lệ.");
		}

		String resetToken = jwtService.createPasswordResetToken(account.getUsername());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("message", "Xác minh OTP thành công.");
		body.put("resetToken", resetToken);
		body.put("username", account.getUsername());
		return body;
	}

	@Transactional
	public Map<String, Object> resetPassword(String token, String newPassword, String confirmPassword) {
		if (newPassword == null || !newPassword.equals(confirmPassword)) {
			throw new InvalidInputException("Mật khẩu xác nhận không khớp.");
		}
		String pwdError = PasswordPolicy.validate(newPassword);
		if (pwdError != null) {
			throw new InvalidInputException(pwdError);
		}

		Claims claims;
		try {
			claims = jwtService.getBody(token.trim());
		} catch (Exception e) {
			throw new InvalidInputException("Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
		}
		if (!jwtService.validate(claims) || !jwtService.isPasswordResetToken(claims)) {
			throw new InvalidInputException("Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
		}

		String username = claims.getSubject();
		Accounts account = accountRepository.findByUsername(username);
		if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
			throw new InvalidInputException("Tài khoản không tồn tại hoặc đã bị khóa.");
		}

		account.setPasswordHash(passwordEncoder.encode(newPassword));
		account.setUpdatedAt(Instant.now());
		accountRepository.save(account);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("message", "Đặt lại mật khẩu thành công. Vui lòng đăng nhập.");
		return body;
	}

	/** Chờ SMTP tối đa SMTP_WAIT_SECONDS — tránh Vite proxy socket hang up. */
	private boolean trySendOtpMailBounded(String to, String username, String otp) {
		try {
			return CompletableFuture.supplyAsync(() -> trySendOtpMail(to, username, otp))
					.get(SMTP_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("SMTP gửi OTP quá {}s — bỏ qua chờ, trả về sớm", SMTP_WAIT_SECONDS);
			return false;
		} catch (Exception e) {
			log.warn("SMTP gửi OTP lỗi: {}", e.getMessage());
			return false;
		}
	}

	private boolean trySendOtpMail(String to, String username, String otp) {
		if (mailSender == null) {
			return false;
		}
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(mailFrom);
			message.setTo(to);
			message.setSubject("[Sports4Life] Mã OTP đặt lại mật khẩu");
			message.setText(
					"Xin chào " + username + ",\n\n"
							+ "Mã OTP đặt lại mật khẩu Sports4Life của bạn là:\n\n"
							+ "    " + otp + "\n\n"
							+ "Mã có hiệu lực " + otpService.getTtlSeconds() / 60 + " phút.\n"
							+ "Không chia sẻ mã này cho bất kỳ ai.\n\n"
							+ "Nếu không phải bạn yêu cầu, hãy bỏ qua email này.\n\n"
							+ "— Sports4Life");
			mailSender.send(message);
			return true;
		} catch (Exception e) {
			log.warn("Không gửi được email OTP tới {}: {}", to, e.getMessage(), e);
			return false;
		}
	}
}
