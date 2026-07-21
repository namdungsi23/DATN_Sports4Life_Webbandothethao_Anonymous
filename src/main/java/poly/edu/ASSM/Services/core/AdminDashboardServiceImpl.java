package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.OrderDetailsRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductRepository;
import poly.edu.ASSM.domain.OrderStatus;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MM/yyyy", Locale.forLanguageTag("vi-VN"));

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private OrdersService ordersService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(int months) {
        int safeMonths = clampMonths(months);
        LocalDate today = LocalDate.now(ZONE);
        Instant weekAgo = today.minusDays(7).atStartOfDay(ZONE).toInstant();

        Map<String, Object> body = new HashMap<>();
        body.put("totalUsers", accountRepository.count());
        body.put("totalProducts", productRepository.count());
        body.put("totalOrders", ordersRepository.count());
        body.put("todayOrders", ordersService.countTodayOrders());
        body.put("newProducts", productRepository.countNewProducts(weekAgo));
        body.put("months", safeMonths);
        body.put("charts", getCharts(safeMonths));
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getCharts(int months) {
        int safeMonths = clampMonths(months);
        Instant start = YearMonth.now(ZONE).minusMonths(safeMonths - 1L)
                .atDay(1)
                .atStartOfDay(ZONE)
                .toInstant();

        Map<String, Object> charts = new HashMap<>();
        charts.put("revenueByMonth", buildRevenueByMonth(start, safeMonths));
        charts.put("revenueByCategory", toNamedMaps(
                orderDetailsRepository.sumRevenueByCategorySince(start, OrderStatus.CANCELLED.name()),
                "categoryName", "revenue"));
        charts.put("topProducts", toTopProductMaps(
                orderDetailsRepository.topProductsSince(start, OrderStatus.CANCELLED.name())));
        charts.put("orderStatusDistribution", toStatusMaps(
                ordersRepository.countGroupByOrderStatusSince(start)));
        charts.put("topCustomers", toCustomerMaps(
                ordersRepository.topCustomersSince(start, OrderStatus.CANCELLED.name())));
        return charts;
    }

    private List<Map<String, Object>> buildRevenueByMonth(Instant start, int months) {
        Map<String, BigDecimal> bucket = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.now(ZONE).minusMonths(months - 1L);
        for (int i = 0; i < months; i++) {
            bucket.put(cursor.format(MONTH_KEY), BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }

        List<Object[]> rows = ordersRepository.sumRevenueByMonthSince(start, OrderStatus.CANCELLED.name());
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String key = String.valueOf(row[0]);
            BigDecimal revenue = toBigDecimal(row[1]);
            if (bucket.containsKey(key)) {
                bucket.put(key, revenue);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : bucket.entrySet()) {
            YearMonth ym = YearMonth.parse(entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("month", entry.getKey());
            item.put("label", ym.format(MONTH_LABEL));
            item.put("revenue", entry.getValue());
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> toNamedMaps(List<Object[]> rows, String nameKey, String valueKey) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put(nameKey, row[0] != null ? String.valueOf(row[0]) : "Khác");
            m.put(valueKey, toBigDecimal(row[1]));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> toTopProductMaps(List<Object[]> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 4) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("productId", row[0]);
            m.put("productName", row[1] != null ? String.valueOf(row[1]) : "Sản phẩm");
            m.put("totalQuantity", toLong(row[2]));
            m.put("totalRevenue", toBigDecimal(row[3]));
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> toStatusMaps(List<Object[]> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status.name(), 0L);
        }
        if (rows != null) {
            for (Object[] row : rows) {
                if (row == null || row.length < 2 || row[0] == null) {
                    continue;
                }
                String status = String.valueOf(row[0]).trim().toUpperCase();
                counts.put(status, toLong(row[1]));
            }
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("status", entry.getKey());
            try {
                m.put("label", OrderStatus.parse(entry.getKey()).getLabel());
            } catch (IllegalArgumentException ex) {
                m.put("label", entry.getKey());
            }
            m.put("orderCount", entry.getValue());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> toCustomerMaps(List<Object[]> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 4) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("accountId", row[0]);
            m.put("username", row[1] != null ? String.valueOf(row[1]) : "");
            String fullName = row[2] != null ? String.valueOf(row[2]).trim() : "";
            m.put("displayName", !fullName.isBlank() ? fullName : m.get("username"));
            m.put("totalSpending", toBigDecimal(row[3]));
            m.put("orderCount", row.length > 4 ? toLong(row[4]) : 0L);
            list.add(m);
        }
        return list;
    }

    private static int clampMonths(int months) {
        if (months <= 6) {
            return 6;
        }
        if (months <= 12) {
            return 12;
        }
        return 24;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
