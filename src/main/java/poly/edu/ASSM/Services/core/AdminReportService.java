package poly.edu.ASSM.Services.core;

import poly.edu.ASSM.dto.response.ReportInvoicesByUserResponse;
import poly.edu.ASSM.dto.response.ReportPageResponse;
import poly.edu.ASSM.dto.response.ReportSeriesResponse;
import poly.edu.ASSM.dto.response.ReportStatusBreakdownResponse;
import poly.edu.ASSM.dto.response.ReportSummaryResponse;

public interface AdminReportService {

    ReportSummaryResponse getSummary(String from, String to);

    ReportSeriesResponse getRevenueSeries(String period, String from, String to);

    ReportStatusBreakdownResponse getStatusBreakdown(String from, String to);

    ReportPageResponse listInvoices(String from, String to, int page, int size);

    ReportInvoicesByUserResponse getInvoicesByUser(String from, String to);
}
