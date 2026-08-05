package poly.edu.ASSM.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardChartsResponse {
    private List<RevenueByMonthChartPoint> revenueByMonth;
    private List<RevenueByCategoryChartPoint> revenueByCategory;
    private List<TopProductChartPoint> topProducts;
    private List<OrderStatusChartPoint> orderStatusDistribution;
    private List<TopCustomerChartPoint> topCustomers;
}
