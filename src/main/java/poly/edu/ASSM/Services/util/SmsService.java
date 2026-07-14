package poly.edu.ASSM.Services.util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gửi SMS OTP. Hỗ trợ SpeedSMS khi có API token; không cấu hình thì fail nhanh
 * để FE hiện {@code devOtp} (chế độ demo DATN).
 */
@Service
public class SmsService {

	private static final Logger log = LoggerFactory.getLogger(SmsService.class);

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	/** false = không gọi gateway, trả false để lộ OTP demo. */
	@Value("${app.sms.enabled:false}")
	private boolean enabled;

	/** speedsms | stub */
	@Value("${app.sms.provider:stub}")
	private String provider;

	@Value("${app.sms.api-token:}")
	private String apiToken;

	/** Brandname / sender SpeedSMS (để trống = dùng type tin nhắn chăm sóc KH). */
	@Value("${app.sms.brand-name:}")
	private String brandName;

	@Value("${app.sms.sender:}")
	private String sender;

	/** 2=CSKH đầu số ngẫu nhiên, 4=Notify/Verify (thường dùng OTP). */
	@Value("${app.sms.sms-type:4}")
	private int smsType;

	private volatile String lastError = "";

	public String getLastError() {
		return lastError == null ? "" : lastError;
	}

	public boolean isConfigured() {
		return enabled && apiToken != null && !apiToken.isBlank()
				&& provider != null
				&& !"stub".equalsIgnoreCase(provider.trim());
	}

	/**
	 * @return true nếu gateway xác nhận gửi thành công
	 */
	public boolean sendOtp(String phone, String message) {
		lastError = "";
		if (phone == null || phone.isBlank() || message == null || message.isBlank()) {
			lastError = "Thiếu số điện thoại hoặc nội dung SMS.";
			return false;
		}
		String msisdn = toMsisdn(phone);
		if (msisdn == null) {
			lastError = "Số điện thoại không hợp lệ.";
			log.warn("SMS: số không hợp lệ {}", phone);
			return false;
		}
		if (!isConfigured()) {
			lastError = "Chưa bật app.sms.enabled hoặc thiếu api-token.";
			log.info("[SMS-stub] to={} msg={} (bật app.sms.enabled + api-token để gửi thật)", msisdn, message);
			return false;
		}
		try {
			if ("speedsms".equalsIgnoreCase(provider.trim())) {
				return sendSpeedSms(msisdn, message);
			}
			lastError = "SMS provider không hỗ trợ: " + provider;
			log.warn(lastError);
			return false;
		} catch (Exception e) {
			lastError = e.getMessage() != null ? e.getMessage() : "Lỗi gọi SMS gateway.";
			log.warn("Gửi SMS thất bại tới {}: {}", msisdn, e.getMessage());
			return false;
		}
	}

	private boolean sendSpeedSms(String msisdn, String content) throws Exception {
		String brand = brandName != null ? brandName.trim() : "";
		String sendAs = sender != null ? sender.trim() : "";
		if (!brand.isBlank()) {
			sendAs = brand;
		}

		int type = smsType > 0 ? smsType : 2;
		// Tài khoản SpeedSMS thường bắt buộc sender/brandname đã đăng ký
		if (sendAs.isBlank()) {
			lastError = "Thiếu brandname/sender. Đăng ký Brandname trên connect.speedsms.vn rồi điền app.sms.brand-name=...";
			log.warn("SpeedSMS: {}", lastError);
			return false;
		}
		if (type == 2 || type == 4) {
			// Có brand thì dùng type 3 (gửi bằng brandname)
			type = 3;
		}

		String json = "{\"to\":[\"" + msisdn + "\"],\"content\":"
				+ toJsonString(content)
				+ ",\"sms_type\":" + type
				+ ",\"sender\":" + toJsonString(sendAs) + "}";

		String basic = Base64.getEncoder()
				.encodeToString((apiToken.trim() + ":").getBytes(StandardCharsets.UTF_8));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://api.speedsms.vn/index.php/sms/send"))
				.timeout(Duration.ofSeconds(15))
				.header("Authorization", "Basic " + basic)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
				.build();
		HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		String body = response.body() == null ? "" : response.body();
		log.info("SpeedSMS status={} body={}", response.statusCode(), body);
		boolean httpOk = response.statusCode() >= 200 && response.statusCode() < 300;
		boolean apiOk = body.contains("\"status\":\"success\"")
				|| body.contains("\"status\": \"success\"");
		if (!apiOk) {
			lastError = body.contains("sender not found")
					? "SpeedSMS: sender/brandname chưa đăng ký hoặc sai tên (sender not found)."
					: ("SpeedSMS lỗi: " + body);
		}
		return httpOk && apiOk;
	}

	private static String toJsonString(String s) {
		if (s == null) {
			return "\"\"";
		}
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	/** Chuẩn hóa về 84xxxxxxxxx (SpeedSMS). */
	static String toMsisdn(String raw) {
		if (raw == null) {
			return null;
		}
		String p = raw.replaceAll("[\\s.\\-()]", "").trim();
		if (p.startsWith("+84")) {
			p = "84" + p.substring(3);
		} else if (p.startsWith("0") && p.length() == 10) {
			p = "84" + p.substring(1);
		} else if (p.startsWith("84") && p.length() == 11) {
			/* ok */
		} else {
			return null;
		}
		return p.matches("^84\\d{9}$") ? p : null;
	}
}
