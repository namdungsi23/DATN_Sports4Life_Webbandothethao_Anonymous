package poly.edu.ASSM.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import poly.edu.ASSM.Services.util.AuthService;

@Component
public class AdminInterceptor implements HandlerInterceptor{
	
	@Autowired
	AuthService authService;
	
	@Override
	public boolean preHandle(HttpServletRequest req,
							 HttpServletResponse resp,
							 Object handler) throws Exception{
		if(!authService.isLogin() || !authService.isAdmin()) {
			resp.sendRedirect("/");
			return false;
		}

		return true;
	}
}
