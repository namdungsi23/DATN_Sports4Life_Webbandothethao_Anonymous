package poly.edu.ASSM.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Lớp 3 — đếm lần đăng nhập thất bại theo IP (in-memory, phù hợp DATN/single-instance).
 */
@Service
public class LoginAttemptService {

	private record Window(AtomicInteger attempts, long windowStartEpochSec) {
	}

	private final Map<String, Window> windows = new ConcurrentHashMap<>();

	@Value("${app.security.login.max-attempts:5}")
	private int maxAttempts;

	@Value("${app.security.login.window-seconds:300}")
	private int windowSeconds;

	public boolean isBlocked(HttpServletRequest request) {
		return currentAttempts(clientKey(request)) >= maxAttempts;
	}

	public void recordFailure(HttpServletRequest request) {
		String key = clientKey(request);
		long now = Instant.now().getEpochSecond();
		Window window = windows.compute(key, (k, prev) -> {
			if (prev == null || now - prev.windowStartEpochSec() >= windowSeconds) {
				return new Window(new AtomicInteger(0), now);
			}
			return prev;
		});
		window.attempts().incrementAndGet();
	}

	public void recordSuccess(HttpServletRequest request) {
		windows.remove(clientKey(request));
	}

	private int currentAttempts(String key) {
		Window window = windows.get(key);
		if (window == null) {
			return 0;
		}
		long now = Instant.now().getEpochSecond();
		if (now - window.windowStartEpochSec() >= windowSeconds) {
			windows.remove(key);
			return 0;
		}
		return window.attempts().get();
	}

	private static String clientKey(HttpServletRequest request) {
		String xf = request.getHeader("X-Forwarded-For");
		if (xf != null && !xf.isBlank()) {
			return xf.split(",")[0].trim();
		}
		return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
	}
}
