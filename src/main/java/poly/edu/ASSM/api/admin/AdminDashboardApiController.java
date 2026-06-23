package poly.edu.ASSM.api.admin;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductRepository;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("@adminAuth.has('DASHBOARD_VIEW')")
public class AdminDashboardApiController {

    @Autowired
    private AccountRepository accountRepo;
    @Autowired
    private ProductRepository productRepo;
    @Autowired
    private OrdersRepository orderRepo;

    @GetMapping
    public Map<String, Object> stats() {
        LocalDate today = LocalDate.now();
        Instant weekAgo = today.minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return Map.of(
                "totalUsers", accountRepo.count(),
                "totalProducts", productRepo.count(),
                "totalOrders", orderRepo.count(),
                "todayOrders", orderRepo.countTodayOrders(today),
                "newProducts", productRepo.countNewProducts(weekAgo));
    }
}
