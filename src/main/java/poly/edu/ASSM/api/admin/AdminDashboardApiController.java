package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminDashboardService;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("@adminAuth.has('DASHBOARD_VIEW')")
public class AdminDashboardApiController {

    @Autowired
    private AdminDashboardService adminDashboardService;

    @GetMapping
    public Map<String, Object> stats(@RequestParam(defaultValue = "12") int months) {
        return adminDashboardService.getDashboard(months);
    }

    @GetMapping("/charts")
    public Map<String, Object> charts(@RequestParam(defaultValue = "12") int months) {
        return adminDashboardService.getCharts(months);
    }
}
