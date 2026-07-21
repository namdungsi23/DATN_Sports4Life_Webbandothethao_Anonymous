package poly.edu.ASSM.Services.core;

import java.util.Map;

public interface AdminReportService {

    Map<String, Object> getSummary(String from, String to);

    Map<String, Object> getRevenueSeries(String period, String from, String to);

    Map<String, Object> getStatusBreakdown(String from, String to);

    Map<String, Object> listInvoices(String from, String to, int page, int size);

    Map<String, Object> getInvoicesByUser(String from, String to);
}
