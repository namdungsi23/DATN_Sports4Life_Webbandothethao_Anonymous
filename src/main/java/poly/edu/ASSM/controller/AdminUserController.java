package poly.edu.ASSM.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.repository.RoleRepository;
import poly.edu.ASSM.Services.core.AccountService;

@Controller
@RequestMapping("/admin/user")
public class AdminUserController {

    @Autowired
    AccountService accSer;
    
    @Autowired
    RoleRepository roleRepo;

    // ===== LIST + SEARCH =====
    @GetMapping
    public String index(
            Model model,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal Object principal
            ) {
        int size = 5;
        
        Accounts acc = new Accounts();
        
        List<String> authorities = roleRepo
        						.findAll()
        						.stream()
        						.map(authority -> authority.getName())
        						.toList();
        
        for(String s : authorities) {
        	System.out.println(s);
        }
        
        Page<Accounts> pages = keyword.isEmpty()
                ? accSer.findAll(page, size)
                : accSer.search(keyword, page, size);
        

        model.addAttribute("users", pages.getContent());
        model.addAttribute("authorities", authorities);
        model.addAttribute("pages", pages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("user", new Accounts());

        return "admin/user";
    }

    // ===== EDIT =====
    @GetMapping("/edit/{username}")
    public String edit(
            @PathVariable String username,
            Model model,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page
    ) {
        int size = 5;

        Page<Accounts> pages = keyword.isEmpty()
                ? accSer.findAll(page, size)
                : accSer.search(keyword, page, size);

        model.addAttribute("users", pages.getContent());
        model.addAttribute("pages", pages);
        model.addAttribute("keyword", keyword);
        model.addAttribute("user", accSer.findByUsername(username));

        return "admin/user";
    }

    // ===== UPDATE ONLY =====
    @PostMapping("/save")
    public String save(
            @ModelAttribute("user") Accounts acc,
            HttpSession session
    ) {
    	
        Accounts target = accSer.findByUsername(acc.getUsername());
        if (target == null) return "redirect:/admin/user?error=notfound";

        accSer.update(acc);
        return "redirect:/admin/user?status=success";
    }

    // ===== DELETE (ADMIN ONLY) =====
    @GetMapping("/delete/{username}")
    public String delete(
            @PathVariable String username,
            HttpSession session
    ) {

        Accounts target = accSer.findByUsername(username);
        if (target == null) return "redirect:/admin/user?error=notfound";

        accSer.delete(target.getId());
        return "redirect:/admin/user?success=deleted";
    }
}


