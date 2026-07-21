package poly.edu.ASSM.security;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * OTP in-memory (DATN / single-instance). Production nên dùng Redis + hash OTP.
 */
@Service
public class OtpService {

	private record Entry(String code, long expiresAtEpochSec, int attempts) {
	}

	private final Map<String, Entry> store = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();

	@Value("${app.security.otp.length:6}")
	private int length;

	@Value("${app.security.otp.ttl-seconds:300}")
	private int ttlSeconds;

	@Value("${app.security.otp.max-attempts:5}")
	private int maxAttempts;

	public String generateAndStore(String key) {
		String normalized = normalizeKey(key);
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(random.nextInt(10));
		}
		String code = sb.toString();
		long exp = Instant.now().getEpochSecond() + ttlSeconds;
		store.put(normalized, new Entry(code, exp, 0));
		return code;
	}

	public enum VerifyResult {
		OK, EXPIRED, INVALID, TOO_MANY_ATTEMPTS, NOT_FOUND
	}

	public VerifyResult verify(String key, String code) {
		String normalized = normalizeKey(key);
		Entry entry = store.get(normalized);
		if (entry == null) {
			return VerifyResult.NOT_FOUND;
		}
		long now = Instant.now().getEpochSecond();
		if (now > entry.expiresAtEpochSec()) {
			store.remove(normalized);
			return VerifyResult.EXPIRED;
		}
		if (entry.attempts() >= maxAttempts) {
			store.remove(normalized);
			return VerifyResult.TOO_MANY_ATTEMPTS;
		}
		if (code == null || !entry.code().equals(code.trim())) {
			store.put(normalized, new Entry(entry.code(), entry.expiresAtEpochSec(), entry.attempts() + 1));
			return VerifyResult.INVALID;
		}
		store.remove(normalized);
		return VerifyResult.OK;
	}

	public void invalidate(String key) {
		store.remove(normalizeKey(key));
	}

	public int getTtlSeconds() {
		return ttlSeconds;
	}

	private static String normalizeKey(String key) {
		return key == null ? "" : key.trim().toLowerCase();
	}
}
