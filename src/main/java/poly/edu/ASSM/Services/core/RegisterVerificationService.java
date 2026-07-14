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
import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.dto.request.PublicRegisterRequest;
import poly.edu.ASSM.dto.request.RegisterVerifyOtpRequest;
import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.repository.AccountRepository;
import poly.edu.ASSM.repository.RoleRepository;
import poly.edu.ASSM.repository.UsersRepository;
import poly.edu.ASSM.security.OtpService;
import poly.edu.ASSM.security.PasswordPolicy;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.Services.util.SmsService;

/**
 * Đăng ký + xác minh danh tính qua Gmail (SMTP) hoặc SMS (OTP).
 */
@Service
public class RegisterVerificationService {

	private static final Logger log = LoggerFactory.getLogger(RegisterVerificationService.class);
	private static final long SMTP_WAIT_SECONDS = 8;
	private static final String OTP_PREFIX = "register:";

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
	private RoleRepository roleRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private AccountService accountService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private OtpService otpService;

	@Autowired(required = false)
	private JavaMailSender mailSender;

	@Autowired
	private AdminNotificationService adminNotificationService;

	@Autowired
	private CloudinaryService cloudinaryService;

	@Autowired
	private SmsService smsService;

	@Value("${app.mail.from:noreply@sports4life.local}")
	private String mailFrom;

	@Value("${app.security.password-reset.dev-expose-otp:true}")
	private boolean devExposeOtp;

	@Transactional
	public Map<String, Object> register(PublicRegisterRequest request) {
		return register(request, null);
	}

	/**
	 * Không bọc SMTP/SMS trong @Transactional — tránh giữ connection DB khi chờ mail
	 * (Hikari hết pool → mọi request đăng ký sau bị timeout).
	 */
	public Map<String, Object> register(PublicRegisterRequest request, MultipartFile photo) {
		String pwdError = PasswordPolicy.validate(request.getPassword());
		if (pwdError != null) {
			throw InvalidInputException.of("password", pwdError);
		}

		Channel channel = Channel.from(request.getVerifyChannel());
		String username = request.getUsername().trim();
		String email = request.getEmail().trim();
		String fullname = request.getFullname().trim();
		String phone = normalizePhone(request.getPhone());

		if (channel == Channel.SMS) {
			if (phone.isBlank()) {
				throw InvalidInputException.of("phone", "Số điện thoại là bắt buộc khi xác minh qua SMS.");
			}
			if (!isValidVnPhone(phone)) {
				throw InvalidInputException.of("phone", "Số điện thoại không hợp lệ (vd: 09xxxxxxxx hoặc +849xxxxxxxx).");
			}
		}

		String avatarUrl = null;
		if (photo != null && !photo.isEmpty()) {
			try {
				avatarUrl = cloudinaryService.uploadAvatar(photo, username);
			} catch (IllegalArgumentException e) {
				throw InvalidInputException.of("photo", e.getMessage());
			} catch (Exception e) {
				throw InvalidInputException.of("photo", "Không tải được ảnh đại diện lên Cloudinary.");
			}
		}

		persistPendingAccount(username, email, fullname, phone, request.getPassword(), avatarUrl, channel);
		return sendRegisterOtp(username, email, phone, channel);
	}

	/** Lưu tài khoản pending; OTP gửi sau khi method này return (không giữ TX khi chờ SMTP). */
	private void persistPendingAccount(
			String username,
			String email,
			String fullname,
			String phone,
			String rawPassword,
			String avatarUrl,
			Channel channel) {
		Accounts existing = accountService.findByUsername(username);
		if (existing != null && Boolean.TRUE.equals(existing.getIsActive())) {
			throw InvalidInputException.of("username", "Tên đăng nhập đã tồn tại.");
		}

		Accounts emailOwner = accountRepository.findFirstByEmailIgnoreCase(email);
		if (emailOwner != null && Boolean.TRUE.equals(emailOwner.getIsActive())
				&& (existing == null || !emailOwner.getId().equals(existing.getId()))) {
			throw InvalidInputException.of("email", "Email đã được sử dụng.");
		}

		Accounts acc;
		if (existing != null && !Boolean.TRUE.equals(existing.getIsActive())) {
			// Cho phép hoàn tất đăng ký dang dở
			acc = existing;
			acc.setPasswordHash(passwordEncoder.encode(rawPassword));
			acc.setEmail(email);
			acc.setIsActive(false);
			acc.setUpdatedAt(Instant.now());
			accountRepository.save(acc);
			accountService.updateCustomerProfile(username, fullname, email, avatarUrl, false);
		} else {
			acc = new Accounts();
			acc.setUsername(username);
			acc.setPasswordHash(passwordEncoder.encode(rawPassword));
			acc.setEmail(email);
			acc.setIsActive(false);
			acc.setRole(roleRepository.findByName("ROLE_USER").orElseThrow(
					() -> new InvalidInputException("Hệ thống chưa cấu hình ROLE_USER.")));
			accountService.update(acc);
			accountService.updateCustomerProfile(username, fullname, email, avatarUrl, false);
		}

		if (channel == Channel.SMS) {
			saveUserPhone(username, phone);
		}
	}

	public Map<String, Object> resendOtp(String username, String channelRaw, String email, String phone) {
		Channel channel = Channel.from(channelRaw);
		Accounts acc = requirePendingAccount(username);
		String mail = email != null && !email.isBlank() ? email.trim() : acc.getEmail();
		String normalizedPhone = normalizePhone(phone);
		if (channel == Channel.SMS) {
			if (normalizedPhone.isBlank()) {
				Users user = usersRepository.findByAccount_Id(acc.getId()).orElse(null);
				normalizedPhone = user != null && user.getPhone() != null ? normalizePhone(user.getPhone()) : "";
			}
			if (normalizedPhone.isBlank()) {
				throw InvalidInputException.of("phone", "Thiếu số điện thoại để gửi lại OTP SMS.");
			}
		}
		return sendRegisterOtp(acc.getUsername(), mail, normalizedPhone, channel);
	}

	public Map<String, Object> verifyOtp(RegisterVerifyOtpRequest request) {
		Channel channel = Channel.from(request.getVerifyChannel());
		Accounts acc = requirePendingAccount(request.getUsername());

		String destination = channel == Channel.SMS
				? normalizePhone(request.getPhone())
				: (request.getEmail() != null && !request.getEmail().isBlank()
						? request.getEmail().trim()
						: acc.getEmail());

		if (channel == Channel.SMS && destination.isBlank()) {
			Users user = usersRepository.findByAccount_Id(acc.getId()).orElse(null);
			destination = user != null && user.getPhone() != null ? normalizePhone(user.getPhone()) : "";
		}
		if (destination.isBlank()) {
			throw InvalidInputException.of(
					channel == Channel.SMS ? "phone" : "email",
					channel == Channel.SMS ? "Thiếu số điện thoại xác minh." : "Thiếu email xác minh.");
		}

		String otpKey = otpKey(channel, destination);
		OtpService.VerifyResult result = otpService.verify(otpKey, request.getOtp());
		switch (result) {
			case OK -> {
				/* ok */
			}
			case EXPIRED -> throw InvalidInputException.of("otp", "Mã OTP đã hết hạn. Vui lòng gửi lại.");
			case TOO_MANY_ATTEMPTS -> throw InvalidInputException.of("otp", "Nhập sai OTP quá nhiều lần. Vui lòng gửi lại mã mới.");
			case NOT_FOUND, INVALID -> throw InvalidInputException.of("otp", "Mã OTP không đúng.");
			default -> throw InvalidInputException.of("otp", "Mã OTP không hợp lệ.");
		}

		activatePendingAccount(acc, channel, destination);

		// Không chặn response đăng ký nếu notify admin chậm/lock DB
		CompletableFuture.runAsync(() -> {
			try {
				adminNotificationService.notifyPanelUsers(
						"User mới đăng ký",
						"Tài khoản mới (đã xác minh " + channel.name() + "): " + acc.getUsername()
								+ " (" + acc.getEmail() + ")",
						"/admin/user?keyword="
								+ java.net.URLEncoder.encode(acc.getUsername(), java.nio.charset.StandardCharsets.UTF_8));
			} catch (Exception ex) {
				log.warn("Gửi thông báo đăng ký thất bại: {}", ex.getMessage());
			}
		});

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("message", "Xác minh thành công. Bạn có thể đăng nhập.");
		body.put("username", acc.getUsername());
		return body;
	}

	private void activatePendingAccount(Accounts acc, Channel channel, String destination) {
		acc.setIsActive(true);
		acc.setUpdatedAt(Instant.now());
		accountRepository.save(acc);

		if (channel == Channel.SMS && !destination.isBlank()) {
			saveUserPhone(acc.getUsername(), destination);
		}
	}

	private Map<String, Object> sendRegisterOtp(String username, String email, String phone, Channel channel) {
		String destination = channel == Channel.SMS ? phone : email;
		String otpKey = otpKey(channel, destination);
		String otp = otpService.generateAndStore(otpKey);

		boolean delivered;
		if (channel == Channel.EMAIL) {
			delivered = trySendOtpMailBounded(email, username, otp);
			if (!delivered) {
				log.info("Register EMAIL OTP for {} (mail not sent): {}", username, otp);
			}
		} else {
			delivered = trySendOtpSms(phone, username, otp);
			if (!delivered) {
				log.info("Register SMS OTP for {} / {} (sms not sent): {}", username, phone, otp);
			}
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("ok", true);
		body.put("needsVerification", true);
		body.put("verifyChannel", channel.name());
		body.put("destination", maskDestination(channel, destination));
		body.put("otpTtlSeconds", otpService.getTtlSeconds());
		body.put("message", channel == Channel.EMAIL
				? (delivered
						? "Đã gửi mã OTP tới Gmail của bạn. Vui lòng nhập mã để kích hoạt tài khoản."
						: "Không gửi được email OTP. Dùng mã demo bên dưới (hoặc kiểm tra SMTP).")
				: (delivered
						? "Đã gửi mã OTP tới số điện thoại của bạn. Vui lòng nhập mã để kích hoạt tài khoản."
						: "SMS chưa gửi được. Dùng mã OTP demo bên dưới."));
		if (!delivered && channel == Channel.SMS) {
			String smsErr = smsService.getLastError();
			if (smsErr != null && !smsErr.isBlank()) {
				body.put("smsError", smsErr);
			}
		}
		if (!delivered && devExposeOtp) {
			body.put("devOtp", otp);
			body.put("devNote", channel == Channel.EMAIL
					? "SMTP chưa gửi được — dùng OTP này để demo."
					: (smsService.isConfigured()
							? "SpeedSMS cần Brandname đã duyệt trên connect.speedsms.vn, rồi điền app.sms.brand-name=TÊN_BRAND. Tạm dùng OTP demo."
							: "Chưa cấu hình app.sms — dùng OTP demo."));
		}
		return body;
	}

	private Accounts requirePendingAccount(String username) {
		if (username == null || username.isBlank()) {
			throw InvalidInputException.of("username", "Tên đăng nhập là bắt buộc.");
		}
		Accounts acc = accountService.findByUsername(username.trim());
		if (acc == null) {
			throw InvalidInputException.of("username", "Không tìm thấy tài khoản đăng ký.");
		}
		if (Boolean.TRUE.equals(acc.getIsActive())) {
			throw InvalidInputException.of("username", "Tài khoản đã được kích hoạt. Vui lòng đăng nhập.");
		}
		return acc;
	}

	private void saveUserPhone(String username, String phone) {
		Accounts acc = accountService.findByUsername(username);
		if (acc == null) {
			return;
		}
		Users user = usersRepository.findByAccount_Id(acc.getId()).orElse(null);
		if (user == null) {
			return;
		}
		user.setPhone(phone);
		user.setUpdatedAt(Instant.now());
		usersRepository.save(user);
	}

	private boolean trySendOtpMailBounded(String to, String username, String otp) {
		try {
			return CompletableFuture.supplyAsync(() -> trySendOtpMail(to, username, otp))
					.get(SMTP_WAIT_SECONDS, TimeUnit.SECONDS);
		} catch (TimeoutException e) {
			log.warn("SMTP gửi OTP đăng ký quá {}s", SMTP_WAIT_SECONDS);
			return false;
		} catch (Exception e) {
			log.warn("SMTP gửi OTP đăng ký lỗi: {}", e.getMessage());
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
			message.setSubject("[Sports4Life] Mã OTP xác minh đăng ký");
			message.setText(
					"Xin chào " + username + ",\n\n"
							+ "Mã OTP xác minh tài khoản Sports4Life của bạn là:\n\n"
							+ "    " + otp + "\n\n"
							+ "Mã có hiệu lực " + otpService.getTtlSeconds() / 60 + " phút.\n"
							+ "Không chia sẻ mã này cho bất kỳ ai.\n\n"
							+ "— Sports4Life");
			mailSender.send(message);
			return true;
		} catch (Exception e) {
			log.warn("Không gửi được email OTP đăng ký tới {}: {}", to, e.getMessage());
			return false;
		}
	}

	/**
	 * SMS thật qua {@link SmsService} (SpeedSMS khi có token).
	 */
	private boolean trySendOtpSms(String phone, String username, String otp) {
		String content = "Sports4Life OTP: " + otp + ". Hieu luc "
				+ (otpService.getTtlSeconds() / 60) + " phut. Khong chia se ma nay.";
		boolean sent = smsService.sendOtp(phone, content);
		if (!sent) {
			log.info("[SMS-OTP] to={} user={} code={} (chua gui that)", phone, username, otp);
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
