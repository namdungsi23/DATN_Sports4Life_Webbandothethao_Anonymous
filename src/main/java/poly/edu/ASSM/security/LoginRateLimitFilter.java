package poly.edu.ASSM.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

	@Autowired
	private LoginAttemptService loginAttemptService;

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return !(path != null && path.endsWith("/login/validate") && "POST".equalsIgnoreCase(request.getMethod()));
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (loginAttemptService.isBlocked(request)) {
			response.setStatus(429);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(
					"{\"ok\":false,\"message\":\"Quá nhiều lần đăng nhập. Vui lòng thử lại sau vài phút.\",\"errors\":{}}");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
