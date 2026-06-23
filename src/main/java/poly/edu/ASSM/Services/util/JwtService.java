package poly.edu.ASSM.Services.util;

import java.security.Key;
import java.util.Date;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.security.SpringRoleNames;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	private static final String SECRET_KEY = "0123456789.0123456789.0123456789";
	/**
	* Tạo JWT (JSON Web Token)
	*
	* @param user là UserDetails chứa thông tin để tạo token
	* @param expirySeconds là thời hạn có hiệu lực (tính bằng giây)
	*/
	public String create(UserDetails user, long expirySeconds) {
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
				.setIssuedAt(new Date(now))
				.setExpiration(new Date(now + 1000 * expirySeconds))
				.signWith(this.getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public Key getSigningKey() {
		return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
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
}
