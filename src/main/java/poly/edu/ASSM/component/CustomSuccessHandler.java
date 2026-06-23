package poly.edu.ASSM.component;

import java.io.IOException;
import java.util.Collection;
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
	
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
		
		//Backend login success handler
		/*
		 //  LƯU VÀO SESSION
        HttpSession session = request.getSession();
        session.setAttribute("USER_SESSION", authentication.getPrincipal());
        System.out.println(session.getAttribute("USER_SESSION"));
		
		for(GrantedAuthority authority : authorities) {
			if(authority.getAuthority().equals("ROLE_ADMIN")) { 
				response.sendRedirect("/admin");
				return;
			}
		}
		
		response.sendRedirect("/product");
		*/
		
		//Send authentication details to frontend
		UserDetails user = (UserDetails) authentication.getPrincipal();
		System.out.println(user.getUsername());
		
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
		
		String accessToken = jwtService.create(user, 15 * 60);
		
		Map<String, Object> body = Map.of(
					"status", 200,
					"message", "Login successfully",
					"username", user.getUsername(),
					"role", roles,
					"roles", roles,
					"permissions", permissions,
					"panelAccess", access.panelUser(),
					"accessToken", accessToken,
					"refreshToken", ""
				);
		
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(objectMapper.writeValueAsString(body));
				
	}

}
