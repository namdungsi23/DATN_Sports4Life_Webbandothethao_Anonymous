package poly.edu.ASSM.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import poly.edu.ASSM.Services.util.AuthServiceImpl;
import poly.edu.ASSM.Services.util.ShoppingCartServiceImpl;

@Controller
@RequestMapping("/cart")
public class CartController {
	@Autowired
	ShoppingCartServiceImpl cartService;
	
	@Autowired
	AuthServiceImpl authService;
	
	@Autowired
	HttpSession session;
	
	@GetMapping("/checkout")
	public String checkout() {
		return "redirect:/checkout";
	}
	
	@PostMapping("/add")
	public String addToCart(@RequestParam int id,
							RedirectAttributes redirect,
							Model model) {
		//Accounts user = (Accounts)session.getAttribute("USER_SESSION");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Object user = auth.getPrincipal();
		
		if(!authService.isLogin() && user == null) {
			redirect.addFlashAttribute("message", "Vui lòng tiến hành đăng nhập để thực hiện chức năng");
			return "redirect:/product";
		}
		
		cartService.add(id);
		
		return "redirect:/cart";
	}
	
	@PostMapping("/remove")
	public String delete(@RequestParam int id,
						 RedirectAttributes redirect) {
		cartService.remove(id);
		
		return "redirect:/cart";
	}
	
	@PostMapping("/update")
	public String update(@RequestParam int id,
						 @RequestParam int qty,
						 RedirectAttributes redirect) {
		cartService.update(id, qty);

		return "redirect:/cart";
	}
	
	@PostMapping("/clear")
	public String clear() {
		cartService.clear();
		
		return "redirect:/cart";
	}
}
