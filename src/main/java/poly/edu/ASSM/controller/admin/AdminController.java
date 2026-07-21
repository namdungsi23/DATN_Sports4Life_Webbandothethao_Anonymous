package poly.edu.ASSM.Controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // TRANG MẶC ĐỊNH
    @GetMapping("")
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

 
    public String dashboard() {
        return "admin/dashboard";
    }

 
    public String category() {
        return "admin/category";
    }

   
    public String product() {
        return "admin/product";
    }
    public String user() {
        return "admin/user";
    }
    public String order() {
    	return "admin/order";
    }
}


