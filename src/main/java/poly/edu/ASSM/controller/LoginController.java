package poly.edu.ASSM.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.ASSM.Services.util.AuthServiceImpl;
import poly.edu.ASSM.Services.web.CookieService;

@Controller
@RequestMapping("/login")
public class LoginController {
	@Autowired
	AuthServiceImpl authService;
	
	@Autowired
	CookieService cookieService;
	
	@PostMapping("/validate")
	public String validate(@RequestParam String username,
						   @RequestParam String pwd,
						   @RequestParam(defaultValue="false") boolean chk,
						   RedirectAttributes redirect) {
		
		authService.login(username, pwd);
		
		if(!authService.isLogin()) {
			redirect.addFlashAttribute("error", "Username hoặc password không hợp lệ, vui lòng thử lại!");
			return "redirect:/";
		}
		
		if(chk) { 
			cookieService.createCookie("rememberedUsername", username, 30);
		}else {
			cookieService.removeCookie("rememberedUsername");
		}
		
		if(authService.isAdmin()) return "redirect:/admin";
		
		return "redirect:/product";
	}
}
