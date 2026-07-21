package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminReportService;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("@adminAuth.has('ORDER_VIEW')")
public class AdminReportApiController {

    @Autowired
    private AdminReportService adminReportService;

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getSummary(from, to);
    }

    @GetMapping("/revenue")
    public Map<String, Object> revenue(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getRevenueSeries(period, from, to);
    }

    @GetMapping("/status-breakdown")
    public Map<String, Object> statusBreakdown(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getStatusBreakdown(from, to);
    }

    @GetMapping("/invoices")
    public Map<String, Object> invoices(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminReportService.listInvoices(from, to, page, size);
    }

    @GetMapping("/by-user")
    public Map<String, Object> byUser(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getInvoicesByUser(from, to);
    }
}
