package poly.edu.ASSM.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.repository.RoleRepository;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.util.AuthService;
import poly.edu.ASSM.security.PasswordPolicy;

@Controller
public class AuthController {
	@Autowired
	AuthService auth;
	
	@Autowired
	AccountService account;
	
	@Autowired
    PasswordEncoder passwordEncoder;

	@Autowired
	RoleRepository roleRepository;

/*
	@PostMapping("/login")
	public String login(Model model, @RequestParam String username, @RequestParam String password) {
		Accounts user = auth.login(username, password);
		if (user == null) {
			model.addAttribute("error", "Sai tên tài khoản và mật khẩu!");
			model.addAttribute("showRegister", false);
			return "page/login";
		}
		if (auth.isAdmin()) {
			return "redirect:/admin";
		}
		return "redirect:/";
	}
*/
	/* register0 đã vô hiệu — tạo ADMIN + lưu mật khẩu thô là lỗ hổng nghiêm trọng */
	@PostMapping("/register0")
	public String register0Disabled(Model model) {
		model.addAttribute("error", "Endpoint đăng ký này đã bị vô hiệu hóa.");
		model.addAttribute("showRegister", true);
		return "page/login";
	}

	@PostMapping("/register")
	public String register(
			@RequestParam String username,
	        @RequestParam String fullname,
	        @RequestParam String email,
	        @RequestParam String password,
	        @RequestParam(required = false) MultipartFile photo,
	        RedirectAttributes redirect,
	        Model model){

		String pwdError = PasswordPolicy.validate(password);
		if (pwdError != null) {
			redirect.addFlashAttribute("error", pwdError);
			return "redirect:/login";
		}
		
		if (account.findByUsername(username) != null) {
		    redirect.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
		    return "redirect:/login";
		}
		Accounts acc = new Accounts();
		acc.setUsername(username);
		acc.setPasswordHash(passwordEncoder.encode(password));
		acc.setEmail(email);
		acc.setIsActive(true);
		acc.setRole(roleRepository.findByName("ROLE_USER").orElseThrow());
		account.update(acc);
		account.updateCustomerProfile(username, fullname, email, null, true);
		return "page/login";
		
	}
}
