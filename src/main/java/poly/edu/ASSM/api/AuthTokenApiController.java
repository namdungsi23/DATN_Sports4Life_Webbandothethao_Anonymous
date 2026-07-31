package poly.edu.ASSM.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import io.jsonwebtoken.Claims;
import poly.edu.ASSM.Services.core.AdminAccessService;
import poly.edu.ASSM.Services.util.JwtService;
import poly.edu.ASSM.exception.InvalidInputException;

@RestController
@RequestMapping("/api/auth")
public class AuthTokenApiController {

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private AdminAccessService adminAccessService;

	public record RefreshBody(String refreshToken) {
	}

	@PostMapping("/refresh")
	public ResponseEntity<Map<String, Object>> refresh(@RequestBody RefreshBody body) {
		if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
			throw InvalidInputException.of("refreshToken", "Thiếu refresh token.");
		}
		try {
			Claims claims = jwtService.getBody(body.refreshToken().trim());
			if (!jwtService.validate(claims) || !jwtService.isRefreshToken(claims)) {
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ.");
			}
			String username = claims.getSubject();
			UserDetails user = userDetailsService.loadUserByUsername(username);
			String accessToken = jwtService.createAccessToken(user);
			String refreshToken = jwtService.createRefreshToken(user);

			AdminAccessService.AdminAccess access = adminAccessService.resolve(username);
			List<String> roles = access.roles().stream().toList();
			List<String> permissions = access.permissions().stream().toList();

			Map<String, Object> payload = new LinkedHashMap<>();
			payload.put("ok", true);
			payload.put("accessToken", accessToken);
			payload.put("refreshToken", refreshToken);
			payload.put("username", username);
			payload.put("roles", roles);
			payload.put("permissions", permissions);
			payload.put("panelAccess", access.panelUser());
			payload.put("canWriteCatalog", adminAccessService.canWriteCatalog(access));
			payload.put("isAdmin", adminAccessService.isAdminRole(access));
			payload.put("isStaff", adminAccessService.isStaffOnly(access));
			return ResponseEntity.ok(payload);
		} catch (ResponseStatusException | InvalidInputException e) {
			throw e;
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ.");
		}
	}
}
