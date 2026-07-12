package poly.edu.ASSM.security;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import poly.edu.ASSM.Services.core.AdminAccessService;
import poly.edu.ASSM.Services.util.JwtService;

@Component
public class CustomSuccessHandler implements AuthenticationSuccessHandler {
	@Autowired
	JwtService jwtService;

	@Autowired
	AdminAccessService adminAccessService;

	@Autowired
	LoginAttemptService loginAttemptService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		UserDetails user = (UserDetails) authentication.getPrincipal();

		List<String> roles = user.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(a -> a != null && a.startsWith("ROLE_"))
				.toList();

		List<String> permissions = user.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.filter(a -> a != null && !a.startsWith("ROLE_"))
				.distinct()
				.toList();

		AdminAccessService.AdminAccess access = adminAccessService.resolve(user.getUsername());

		String accessToken = jwtService.createAccessToken(user);
		String refreshToken = jwtService.createRefreshToken(user);

		loginAttemptService.recordSuccess(request);

		Map<String, Object> body = new java.util.HashMap<>();
		body.put("status", 200);
		body.put("message", "Login successfully");
		body.put("username", user.getUsername());
		body.put("role", roles);
		body.put("roles", roles);
		body.put("permissions", permissions);
		body.put("panelAccess", access.panelUser());
		body.put("canWriteCatalog", adminAccessService.canWriteCatalog(access));
		body.put("isAdmin", adminAccessService.isAdminRole(access));
		body.put("isStaff", adminAccessService.isStaffOnly(access));
		body.put("accessToken", accessToken);
		body.put("refreshToken", refreshToken);

		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
