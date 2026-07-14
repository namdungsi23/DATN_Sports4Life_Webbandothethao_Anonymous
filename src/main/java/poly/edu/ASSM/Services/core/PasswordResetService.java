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
import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.repository.AccountRepository;
import poly.edu.ASSM.repository.UsersRepository;
import poly.edu.ASSM.Services.util.JwtService;
import poly.edu.ASSM.Services.util.SmsService;
import poly.edu.ASSM.security.OtpService;
import poly.edu.ASSM.security.PasswordPolicy;

@Service
public class PasswordResetService {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
	private static final long SMTP_WAIT_SECONDS = 8;
	private static final String OTP_PREFIX = "reset:";

	public enum Channel {
		EMAIL, SMS;

		public static Channel from(String raw) {
			if (raw == null || raw.isBlank()) {
				return EMAIL;
			}
			String v = raw.trim().toUpperCase();
			if ("SMS".equals(v) || "PHONE".equals(v)) {
				return SMS;
			}
			if ("EMAIL".equals(v) || "GMAIL".equals(v) || "MAIL".equals(v)) {
				return EMAIL;
			}
			throw InvalidInputException.of("verifyChannel", "Kênh xác minh phải là EMAIL (Gmail) hoặc SMS.");
		}
	}

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private OtpService otpService;

	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Autowired
	private SmsService smsService;

	@Value("${app.mail.from:noreply@sports4life.local}")
	private String mailFrom;

	@Value("${app.security.password-reset.dev-expose-otp:true}")
	private boolean devExposeOtp;

	/**
	 * Gửi OTP qua Gmail hoặc SMS. Message chung — không lộ tài khoản có tồn tại hay không.
	 */
	public Map<String, Object> requestReset(String channelRaw, String email, String phone) {
		Channel channel = Channel.from(channelRaw);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("verifyChannel", channel.name());
		body.put("otpTtlSeconds", otpService.getTtlSeconds());

		if (channel == Channel.EMAIL) {
			body.put("message", "Nếu email tồn tại, mã OTP đã được gửi. Vui lòng kiểm tra hộp thư.");
			String normalized = email == null ? "" : email.trim();
			if (normalized.isBlank()) {
				throw InvalidInputException.of("email", "Email là bắt buộc khi xác minh qua Gmail.");
			}
			body.put("destination", maskDestination(channel, normalized));

			Accounts account = accountRepository.findFirstByEmailIgnoreCase(normalized);
			if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
				return body;
			}

			String otpKey = otpKey(channel, normalized);
			String otp = otpService.generateAndStore(otpKey);
			boolean mailed = trySendOtpMailBounded(account.getEmail(), account.getUsername(), otp);
			if (!mailed) {
				log.info("Password reset EMAIL OTP for {} (mail not sent): {}", account.getUsername(), otp);
				if (devExposeOtp) {
					body.put("devOtp", otp);
					body.put("devNote", "SMTP chưa gửi được — dùng OTP này để demo.");
				}
			}
			return body;
		}

		// SMS
		String normalizedPhone = normalizePhone(phone);
		if (normalizedPhone.isBlank()) {
			throw InvalidInputException.of("phone", "Số điện thoại là bắt buộc khi xác minh qua SMS.");
		}
		if (!isValidVnPhone(normalizedPhone)) {
			throw InvalidInputException.of("phone", "Số điện thoại không hợp lệ (vd: 09xxxxxxxx hoặc +849xxxxxxxx).");
		}
		body.put("message", "Nếu số điện thoại tồn tại, mã OTP đã được gửi qua SMS.");
		body.put("destination", maskDestination(channel, normalizedPhone));

		Accounts account = findActiveAccountByPhone(normalizedPhone);
		if (account == null) {
			return body;
		}

		String otpKey = otpKey(channel, normalizedPhone);
		String otp = otpService.generateAndStore(otpKey);
		boolean sent = trySendOtpSms(normalizedPhone, account.getUsername(), otp);
		if (!sent) {
			log.info("Password reset SMS OTP for {} / {} (sms not sent): {}", account.getUsername(), normalizedPhone, otp);
			if (devExposeOtp) {
				body.put("devOtp", otp);
				body.put("devNote", "SMS gateway chưa cấu hình — dùng OTP này để demo.");
			}
		}
		return body;
	}

	/** Xác minh OTP → cấp resetToken (JWT typ=reset). */
	public Map<String, Object> verifyOtp(String channelRaw, String email, String phone, String otp) {
		Channel channel = Channel.from(channelRaw);
		Accounts account;
		String destination;
		String otpKey;

		if (channel == Channel.EMAIL) {
			destination = email == null ? "" : email.trim();
			if (destination.isBlank()) {
				throw InvalidInputException.of("email", "Email là bắt buộc.");
			}
			account = accountRepository.findFirstByEmailIgnoreCase(destination);
			otpKey = otpKey(channel, destination);
		} else {
			destination = normalizePhone(phone);
			if (destination.isBlank()) {
				throw InvalidInputException.of("phone", "Số điện thoại là bắt buộc.");
			}
			account = findActiveAccountByPhone(destination);
			otpKey = otpKey(channel, destination);
		}

		if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
			throw InvalidInputException.of("otp", "Mã OTP không hợp lệ hoặc đã hết hạn.");
		}

		OtpService.VerifyResult result = otpService.verify(otpKey, otp);
		switch (result) {
			case OK -> {
				/* continue */
			}
			case EXPIRED -> throw InvalidInputException.of("otp", "Mã OTP đã hết hạn. Vui lòng gửi lại.");
			case TOO_MANY_ATTEMPTS -> throw InvalidInputException.of("otp", "Nhập sai OTP quá nhiều lần. Vui lòng gửi lại mã mới.");
			case NOT_FOUND, INVALID -> throw InvalidInputException.of("otp", "Mã OTP không đúng.");
			default -> throw InvalidInputException.of("otp", "Mã OTP không hợp lệ.");
		}

		String resetToken = jwtService.createPasswordResetToken(account.getUsername());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("message", "Xác minh OTP thành công.");
		body.put("resetToken", resetToken);
		body.put("username", account.getUsername());
		body.put("verifyChannel", channel.name());
		return body;
	}

	@Transactional
	public Map<String, Object> resetPassword(String token, String newPassword, String confirmPassword) {
		if (newPassword == null || !newPassword.equals(confirmPassword)) {
			throw InvalidInputException.of("confirmPassword", "Mật khẩu xác nhận không khớp.");
		}
		String pwdError = PasswordPolicy.validate(newPassword);
		if (pwdError != null) {
			throw InvalidInputException.of("newPassword", pwdError);
		}

		Claims claims;
		try {
			claims = jwtService.getBody(token.trim());
		} catch (Exception e) {
			throw InvalidInputException.of("token", "Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
		}
		if (!jwtService.validate(claims) || !jwtService.isPasswordResetToken(claims)) {
			throw InvalidInputException.of("token", "Phiên đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
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

	private Accounts findActiveAccountByPhone(String phone) {
		Users user = usersRepository.findFirstByPhone(phone).orElse(null);
		if (user == null && phone.startsWith("+84")) {
			user = usersRepository.findFirstByPhone("0" + phone.substring(3)).orElse(null);
		}
		if (user == null && phone.startsWith("0") && phone.length() == 10) {
			user = usersRepository.findFirstByPhone("+84" + phone.substring(1)).orElse(null);
		}
		if (user == null || user.getAccount() == null) {
			return null;
		}
		Accounts account = user.getAccount();
		return Boolean.TRUE.equals(account.getIsActive()) ? account : null;
	}

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

	private boolean trySendOtpSms(String phone, String username, String otp) {
		String content = "Sports4Life OTP dat lai MK: " + otp + ". Hieu luc "
				+ (otpService.getTtlSeconds() / 60) + " phut.";
		boolean sent = smsService.sendOtp(phone, content);
		if (!sent) {
			log.info("[SMS-OTP reset] to={} user={} code={} (chua gui that)", phone, username, otp);
		}
		return sent;
	}

	private static String otpKey(Channel channel, String destination) {
		return OTP_PREFIX + channel.name().toLowerCase() + ":" + destination.trim().toLowerCase();
	}

	private static String normalizePhone(String raw) {
		if (raw == null) {
			return "";
		}
		String p = raw.replaceAll("[\\s.\\-()]", "").trim();
		if (p.startsWith("84") && p.length() == 11) {
			p = "+" + p;
		}
		return p;
	}

	private static boolean isValidVnPhone(String phone) {
		return phone.matches("^(0\\d{9}|\\+84\\d{9})$");
	}

	private static String maskDestination(Channel channel, String destination) {
		if (destination == null || destination.isBlank()) {
			return "";
		}
		if (channel == Channel.EMAIL) {
			int at = destination.indexOf('@');
			if (at <= 1) {
				return "***" + destination.substring(Math.max(0, at));
			}
			return destination.charAt(0) + "***" + destination.substring(at);
		}
		String d = destination;
		if (d.length() <= 4) {
			return "****";
		}
		return d.substring(0, 3) + "****" + d.substring(d.length() - 3);
	}
}
