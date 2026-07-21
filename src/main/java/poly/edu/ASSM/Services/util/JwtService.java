package poly.edu.ASSM.Services.util;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.security.SpringRoleNames;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Lớp 3 — Authentication: phát hành / xác thực JWT (secret từ cấu hình).
 */
@Service
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);
	private static final String DEV_FALLBACK_SECRET =
			"Sports4Life-Dev-Secret-Key-Change-In-Production-Min-32-Chars!!";

	@Value("${app.security.jwt.secret:" + DEV_FALLBACK_SECRET + "}")
	private String secretKey;

	@Value("${app.security.jwt.access-ttl-seconds:900}")
	private long accessTtlSeconds;

	@Value("${app.security.jwt.refresh-ttl-seconds:604800}")
	private long refreshTtlSeconds;

	@Value("${app.security.jwt.reset-ttl-seconds:900}")
	private long resetTtlSeconds;

	@PostConstruct
	void warnIfUsingDevSecret() {
		if (DEV_FALLBACK_SECRET.equals(secretKey)) {
			log.warn("JWT đang dùng secret mặc định — đặt app.security.jwt.secret trong application.properties trước production.");
		}
	}

	public long getAccessTtlSeconds() {
		return accessTtlSeconds;
	}

	public long getRefreshTtlSeconds() {
		return refreshTtlSeconds;
	}

	public long getResetTtlSeconds() {
		return resetTtlSeconds;
	}

	public String createAccessToken(UserDetails user) {
		return create(user, accessTtlSeconds, "access");
	}

	public String createRefreshToken(UserDetails user) {
		return create(user, refreshTtlSeconds, "refresh");
	}

	/** Token chỉ dùng cho quên mật khẩu — không có roles/permissions. */
	public String createPasswordResetToken(String username) {
		long now = System.currentTimeMillis();
		return Jwts.builder()
				.setSubject(username)
				.claim("typ", "reset")
				.setIssuedAt(new Date(now))
				.setExpiration(new Date(now + 1000 * resetTtlSeconds))
				.signWith(this.getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	/**
	 * @param user          UserDetails
	 * @param expirySeconds TTL giây
	 */
	public String create(UserDetails user, long expirySeconds) {
		return create(user, expirySeconds, "access");
	}

	public String create(UserDetails user, long expirySeconds, String tokenType) {
		long now = System.currentTimeMillis();

		List<String> roles = user
				.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.map(SpringRoleNames::normalize)
				.filter(s -> !s.isEmpty() && s.startsWith("ROLE_"))
				.toList();

		List<String> permissions = user
				.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.filter(s -> s != null && !s.startsWith("ROLE_"))
				.distinct()
				.toList();

		return Jwts.builder()
				.setSubject(user.getUsername())
				.claim("roles", roles)
				.claim("permissions", permissions)
				.claim("typ", tokenType)
				.setIssuedAt(new Date(now))
				.setExpiration(new Date(now + 1000 * expirySeconds))
				.signWith(this.getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public Key getSigningKey() {
		byte[] bytes = secretKey.getBytes(StandardCharsets.UTF_8);
		if (bytes.length < 32) {
			// HS256 cần >= 256 bit; pad an toàn cho môi trường dev nếu secret ngắn
			byte[] padded = new byte[32];
			System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
			for (int i = bytes.length; i < 32; i++) {
				padded[i] = (byte) (i * 31);
			}
			bytes = padded;
		}
		return Keys.hmacShaKeyFor(bytes);
	}

	public Claims getBody(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(this.getSigningKey())
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public boolean validate(Claims claims) {
		if (claims == null) {
			return false;
		}
		Date exp = claims.getExpiration();
		return exp != null && exp.after(new Date());
	}

	public boolean isRefreshToken(Claims claims) {
		if (claims == null) {
			return false;
		}
		Object typ = claims.get("typ");
		return typ != null && "refresh".equalsIgnoreCase(typ.toString());
	}

	public boolean isAccessToken(Claims claims) {
		if (claims == null) {
			return false;
		}
		Object typ = claims.get("typ");
		// Token cũ không có typ → coi như access
		return typ == null || "access".equalsIgnoreCase(typ.toString());
	}

	public boolean isPasswordResetToken(Claims claims) {
		if (claims == null) {
			return false;
		}
		Object typ = claims.get("typ");
		return typ != null && "reset".equalsIgnoreCase(typ.toString());
	}
}
