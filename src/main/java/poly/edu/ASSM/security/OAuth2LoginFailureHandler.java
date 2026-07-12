package poly.edu.ASSM.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

	@Value("${app.frontend.url:http://localhost:5173}")
	private String frontendUrl;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String msg = exception.getMessage() != null && !exception.getMessage().isBlank()
				? exception.getMessage()
				: "Đăng nhập Google thất bại.";
		String base = frontendUrl.trim().replaceAll("/$", "");
		String q = "oauth_error=" + URLEncoder.encode(msg, StandardCharsets.UTF_8);
		response.sendRedirect(base + "/login?" + q);
	}
}
