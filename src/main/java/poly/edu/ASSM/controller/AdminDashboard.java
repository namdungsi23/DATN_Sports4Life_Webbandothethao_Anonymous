package poly.edu.ASSM.controller;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import poly.edu.ASSM.repository.AccountRepository;
import poly.edu.ASSM.repository.OrdersRepository;
import poly.edu.ASSM.repository.ProductRepository;
import poly.edu.ASSM.Services.core.OrdersService;

@Controller
public class AdminDashboard {

	 @Autowired
	    AccountRepository accountRepo;

	    @Autowired
	    ProductRepository productRepo;

	    @Autowired
	    OrdersRepository orderRepo;

	    @Autowired
	    OrdersService ordersService;
	    @GetMapping("/admin/dashboard")
	    public String dashboard(Model model) {

	        model.addAttribute("totalUsers", accountRepo.count());
	        model.addAttribute("totalProducts", productRepo.count());
	        model.addAttribute("totalOrders", orderRepo.count());

	        model.addAttribute("todayOrders", ordersService.countTodayOrders());

        model.addAttribute("newProducts",
                productRepo.countNewProducts(
                        LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant()));

	        return "admin/dashboard";
	    }
}
