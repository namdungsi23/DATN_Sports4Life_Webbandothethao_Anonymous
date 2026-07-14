package poly.edu.ASSM.controller.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Category;
import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.Services.core.AccountsServiceImpl;
import poly.edu.ASSM.Services.core.CategoryServiceImpl;
import poly.edu.ASSM.Services.core.ProductServiceImpl;
import poly.edu.ASSM.Services.util.AuthServiceImpl;
import poly.edu.ASSM.Services.util.ShoppingCartServiceImpl;
import poly.edu.ASSM.domain.PaymentMethod;


@Controller
@RequestMapping("/")
public class HomeController {
	@Autowired
	ShoppingCartServiceImpl cartService;
	
	@Autowired 
	AuthServiceImpl authService;
	
	@Autowired
	AccountsServiceImpl accountService;
	
	@Autowired
	ProductServiceImpl productService;
	
	@Autowired
	CategoryServiceImpl catService;
	
	@ModelAttribute("user")
	public Accounts user(@AuthenticationPrincipal Object principal) {

	    if (principal == null) {
	        return null;
	    }

	    //OAuth2 login
	    if (principal instanceof OAuth2User oauthUser) {
	        String email = oauthUser.getAttribute("email");
	        return accountService.findByUsername(email);
	    }

	    //Normal login (UserDetails)
	    if (principal instanceof UserDetails userDetails) {
	        String username = userDetails.getUsername();
	        return accountService.findByUsername(username);
	    }

	    return null;
	}
	
	@ModelAttribute("count")
	public int cart() {
        return cartService.getCount();
    }
	
	@GetMapping
	public String index(Model model,
			            @ModelAttribute("user") Accounts user) {
		model.addAttribute("user", user);
	    return "page/index";
	}
	
	
	@GetMapping("/product")
    public String products(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "price") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            Model model
    ) {
		Sort.Direction direction = 
				dir.equalsIgnoreCase("asc")
					? Sort.Direction.ASC
				    : Sort.Direction.DESC;
		
		Pageable pageable = PageRequest.of(page, 10, Sort.by(direction, sort));
		
		Page<Products> products = productService.filterProducts(cat, keyword, min, max, pageable);
		
		List<Category> categories = catService.findAll();
		
		model.addAttribute("categories", categories);
		model.addAttribute("products", products);
        model.addAttribute("cat", cat);
        model.addAttribute("keyword", keyword);
        model.addAttribute("min", min);
        model.addAttribute("max", max);
        model.addAttribute("sort", sort);
        model.addAttribute("dir", dir);

        return "page/product";
	}
	
	@GetMapping("/contact")
	public String contact(){
		return "page/contact";
	}
	@GetMapping("/more")
	public String more(){
		return "page/more";
	}

	@GetMapping("/cart")
	public String cart(Model model){
		model.addAttribute("cartItems", cartService.getItems());
		model.addAttribute("amount", cartService.getAmount());
		
		return "page/cart/cart";
	}
	@GetMapping("/login")
	public String login(Model model){
		
		return "fragments/header";
	}
	
	@GetMapping("/register")
	public String register(){
		return "page/register";
	}
	
	@GetMapping("/checkout")
	public String checkout(Model model) {
		model.addAttribute("cartItems", cartService.getItems());
	    model.addAttribute("amount", cartService.getAmount());
		return "/page/cart/checkout";
	}
	

	@GetMapping("/payment")
	public String paymentView(@ModelAttribute("user") Accounts user,
	                          @RequestParam PaymentMethod paymentMethod,
	                          Model model) {

	    if (user == null) {
	        return "redirect:/";
	    }

	    model.addAttribute("username", user.getUsername());
	    model.addAttribute("paymentMethod", paymentMethod);
	    model.addAttribute("amount", cartService.getAmount());
	    model.addAttribute("proceeding", true);

	    return "page/cart/payment";
	}
	
	@GetMapping("/logout")
	public String logout(HttpServletRequest req, HttpServletResponse resp){
		cartService.clear();
		authService.logout();
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		if(auth != null) {
			new SecurityContextLogoutHandler().logout(req, resp, auth);
		}
		
		return "redirect:/";
	}
	
	@GetMapping("/logout/oauth2")
	public String logoutOauth2(HttpServletRequest req, HttpServletResponse resp) {
		
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		
		if(auth != null) {
			new SecurityContextLogoutHandler().logout(req, resp, auth);
		}
		
		return "redirect:/";
	}
	
}
