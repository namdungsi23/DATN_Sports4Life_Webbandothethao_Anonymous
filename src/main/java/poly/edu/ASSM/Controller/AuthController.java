package poly.edu.ASSM.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Repository.RoleRepository;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.util.AuthService;

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
	/* ==Xử lý Register== */
	@PostMapping("/register0")
	public String register0(@RequestParam String username, @RequestParam String email, @RequestParam String password,
			@RequestParam String confirm, Model model) {
		if (account.findByUsername(username) != null) {
		    model.addAttribute("error", "Tên đăng nhập đã tồn tại!");
		    model.addAttribute("showRegister", true);
		    return "page/login";
		}

		if (!password.equals(confirm)) {
			model.addAttribute("error", "Mật khẩu không khớp!");
			model.addAttribute("showRegister", true);
			return "page/login";
		}
		
		// Tạo tài khoản
		Accounts acc = new Accounts();
		acc.setUsername(username);
		acc.setPasswordHash(password);
		acc.setEmail(email);
		acc.setIsActive(true);
		acc.setRole(roleRepository.findByName("ROLE_ADMIN").orElseThrow());
		account.update(acc);
		model.addAttribute("sucess", "Đăng nhập thành công!Hãy đăng nhập");
		model.addAttribute("showRegister", false);
		return "page/login";
		// Logout

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
		
		if (account.findByUsername(username) != null) {
		    redirect.addFlashAttribute("error", "Tên đăng nhập đã tồn tại!");
		    //redirect.addFlashAttribute("showRegister", true); To be continue
		    return "redirect:/login"; //To be continue
		}
		// Tạo tài khoản
			Accounts acc = new Accounts();
			acc.setUsername(username);
			acc.setPasswordHash(passwordEncoder.encode(password));
			acc.setEmail(email);
			acc.setIsActive(true);
			acc.setRole(roleRepository.findByName("ROLE_USER").orElseThrow());
			account.update(acc);
			account.updateCustomerProfile(username, fullname, email, null, true);
		return "page/login"; // To be continue
		
	}
}
