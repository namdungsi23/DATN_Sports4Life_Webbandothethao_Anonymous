package poly.edu.ASSM.mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.dto.response.DashboardChartsResponse;
import poly.edu.ASSM.dto.response.DashboardResponse;
import poly.edu.ASSM.dto.response.OrderStatusChartPoint;
import poly.edu.ASSM.dto.response.RevenueByCategoryChartPoint;
import poly.edu.ASSM.dto.response.RevenueByMonthChartPoint;
import poly.edu.ASSM.dto.response.TopCustomerChartPoint;
import poly.edu.ASSM.dto.response.TopProductChartPoint;

@Component
public class AdminDashboardMapper {

    public DashboardResponse toDashboardResponse(
            long totalUsers,
            long totalProducts,
            long totalOrders,
            long todayOrders,
            long newProducts,
            int months,
            DashboardChartsResponse charts) {
        return DashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .todayOrders(todayOrders)
                .newProducts(newProducts)
                .months(months)
                .charts(charts)
                .build();
    }

    public DashboardResponse fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object charts = body.get("charts");
        DashboardChartsResponse chartsResponse = charts instanceof DashboardChartsResponse response
                ? response
                : charts instanceof Map<?, ?> map
                        ? toChartsResponse((Map<String, Object>) map)
                        : null;
        return DashboardResponse.builder()
                .totalUsers(longValue(body.get("totalUsers")))
                .totalProducts(longValue(body.get("totalProducts")))
                .totalOrders(longValue(body.get("totalOrders")))
                .todayOrders(longValue(body.get("todayOrders")))
                .newProducts(longValue(body.get("newProducts")))
                .months(intValue(body.get("months")))
                .charts(chartsResponse)
                .build();
    }

    @SuppressWarnings("unchecked")
    public DashboardChartsResponse toChartsResponse(Map<String, Object> charts) {
        if (charts == null) {
            return null;
        }
        return DashboardChartsResponse.builder()
                .revenueByMonth(toRevenueByMonthList((List<Map<String, Object>>) charts.get("revenueByMonth")))
                .revenueByCategory(toRevenueByCategoryList((List<Map<String, Object>>) charts.get("revenueByCategory")))
                .topProducts(toTopProductList((List<Map<String, Object>>) charts.get("topProducts")))
                .orderStatusDistribution(toOrderStatusList((List<Map<String, Object>>) charts.get("orderStatusDistribution")))
                .topCustomers(toTopCustomerList((List<Map<String, Object>>) charts.get("topCustomers")))
                .build();
    }

    public DashboardChartsResponse wrapCharts(
            List<RevenueByMonthChartPoint> revenueByMonth,
            List<RevenueByCategoryChartPoint> revenueByCategory,
            List<TopProductChartPoint> topProducts,
            List<OrderStatusChartPoint> orderStatusDistribution,
            List<TopCustomerChartPoint> topCustomers) {
        return DashboardChartsResponse.builder()
                .revenueByMonth(revenueByMonth != null ? revenueByMonth : List.of())
                .revenueByCategory(revenueByCategory != null ? revenueByCategory : List.of())
                .topProducts(topProducts != null ? topProducts : List.of())
                .orderStatusDistribution(orderStatusDistribution != null ? orderStatusDistribution : List.of())
                .topCustomers(topCustomers != null ? topCustomers : List.of())
                .build();
    }

    public List<RevenueByMonthChartPoint> toRevenueByMonthList(List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        List<RevenueByMonthChartPoint> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (row == null) {
                continue;
            }
            list.add(RevenueByMonthChartPoint.builder()
                    .month(stringValue(row.get("month")))
                    .label(stringValue(row.get("label")))
                    .revenue(decimalValue(row.get("revenue")))
                    .build());
        }
        return list;
    }

    public List<RevenueByCategoryChartPoint> toRevenueByCategoryList(List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null)
                .map(row -> RevenueByCategoryChartPoint.builder()
                        .categoryName(stringValue(row.get("categoryName")))
                        .revenue(decimalValue(row.get("revenue")))
                        .build())
                .collect(Collectors.toList());
    }

    public List<TopProductChartPoint> toTopProductList(List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null)
                .map(row -> TopProductChartPoint.builder()
                        .productId(row.get("productId"))
                        .productName(stringValue(row.get("productName")))
                        .totalQuantity(longValue(row.get("totalQuantity")))
                        .totalRevenue(decimalValue(row.get("totalRevenue")))
                        .build())
                .collect(Collectors.toList());
    }

    public List<OrderStatusChartPoint> toOrderStatusList(List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null)
                .map(row -> OrderStatusChartPoint.builder()
                        .status(stringValue(row.get("status")))
                        .label(stringValue(row.get("label")))
                        .orderCount(longValue(row.get("orderCount")))
                        .build())
                .collect(Collectors.toList());
    }

    public List<TopCustomerChartPoint> toTopCustomerList(List<Map<String, Object>> rows) {
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(row -> row != null)
                .map(row -> TopCustomerChartPoint.builder()
                        .accountId(row.get("accountId"))
                        .username(stringValue(row.get("username")))
                        .displayName(stringValue(row.get("displayName")))
                        .totalSpending(decimalValue(row.get("totalSpending")))
                        .orderCount(longValue(row.get("orderCount")))
                        .build())
                .collect(Collectors.toList());
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }
}
