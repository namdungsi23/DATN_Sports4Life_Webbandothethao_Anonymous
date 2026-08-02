package poly.edu.ASSM.api.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminReportService;
import poly.edu.ASSM.dto.response.ReportInvoicesByUserResponse;
import poly.edu.ASSM.dto.response.ReportPageResponse;
import poly.edu.ASSM.dto.response.ReportSeriesResponse;
import poly.edu.ASSM.dto.response.ReportStatusBreakdownResponse;
import poly.edu.ASSM.dto.response.ReportSummaryResponse;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("@adminAuth.has('ORDER_VIEW')")
public class AdminReportApiController {

    @Autowired
    private AdminReportService adminReportService;

    @GetMapping("/summary")
    public ReportSummaryResponse summary(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getSummary(from, to);
    }

    @GetMapping("/revenue")
    public ReportSeriesResponse revenue(
            @RequestParam(defaultValue = "month") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getRevenueSeries(period, from, to);
    }

    @GetMapping("/status-breakdown")
    public ReportStatusBreakdownResponse statusBreakdown(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getStatusBreakdown(from, to);
    }

    @GetMapping("/invoices")
    public ReportPageResponse invoices(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminReportService.listInvoices(from, to, page, size);
    }

    @GetMapping("/by-user")
    public ReportInvoicesByUserResponse byUser(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return adminReportService.getInvoicesByUser(from, to);
    }
}
