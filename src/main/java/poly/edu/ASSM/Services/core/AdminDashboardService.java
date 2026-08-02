package poly.edu.ASSM.Services.core;



import poly.edu.ASSM.dto.response.DashboardChartsResponse;

import poly.edu.ASSM.dto.response.DashboardResponse;



public interface AdminDashboardService {

    DashboardResponse getDashboard(int months);



    DashboardChartsResponse getCharts(int months);

}

