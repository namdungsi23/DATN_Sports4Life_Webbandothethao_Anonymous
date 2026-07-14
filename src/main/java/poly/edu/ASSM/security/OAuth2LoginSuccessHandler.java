package poly.edu.ASSM.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.util.JwtService;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	@Autowired
	private AccountService accountService;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private JwtService jwtService;

	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {

		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
		OAuth2User oauth2User = token.getPrincipal();

		String email = oauth2User.getAttribute("email");
		String name = oauth2User.getAttribute("name");
		String picture = oauth2User.getAttribute("picture");

		if (email == null || email.isBlank()) {
			redirectError(response, "Không lấy được email từ Google.");
			return;
		}

		Accounts account = accountService.saveOAuthLogin(email, name, picture);

		if (account == null || !Boolean.TRUE.equals(account.getIsActive())) {
			redirectError(response, "Tài khoản bị khóa hoặc không hợp lệ.");
			return;
		}

		try {
			UserDetails ud = userDetailsService.loadUserByUsername(email);
			String accessToken = jwtService.createAccessToken(ud);
			String refreshToken = jwtService.createRefreshToken(ud);
			List<String> roles = ud.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.toList();

			String rolesJson = objectMapper.writeValueAsString(roles);
			String base = frontendUrl.trim().replaceAll("/$", "");
			String hash = "oauth=1"
					+ "&username=" + URLEncoder.encode(ud.getUsername(), StandardCharsets.UTF_8)
					+ "&accessToken=" + URLEncoder.encode(accessToken, StandardCharsets.UTF_8)
					+ "&refreshToken=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
					+ "&roles=" + URLEncoder.encode(rolesJson, StandardCharsets.UTF_8);
			response.sendRedirect(base + "/login#" + hash);
		} catch (UsernameNotFoundException e) {
			redirectError(response, "Không tìm thấy người dùng trong hệ thống.");
		}
	}

	private static String buildQuery(Map<String, String> params) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Map.Entry<String, String> e : params.entrySet()) {
			if (!first) {
				sb.append('&');
			}
			first = false;
			sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
			sb.append('=');
			sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
		}
		return sb.toString();
	}

	private void redirectError(HttpServletResponse response, String message) throws IOException {
		String base = frontendUrl.trim().replaceAll("/$", "");
		String url = base + "/login?" + buildQuery(Map.of(
				"oauth_error", message));
		response.sendRedirect(url);
	}
}
