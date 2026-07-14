package poly.edu.ASSM.Services.core;

import java.util.Map;

public interface AdminDashboardService {
    Map<String, Object> getDashboard(int months);

    Map<String, Object> getCharts(int months);
}
