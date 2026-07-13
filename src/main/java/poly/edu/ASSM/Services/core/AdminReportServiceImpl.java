package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ShipmentRepository;

@Service
public class AdminReportServiceImpl implements AdminReportService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(new Locale("vi"));
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MM/yyyy").withLocale(new Locale("vi"));
    private static final LocalDate ALL_TIME_START = LocalDate.of(2000, 1, 1);

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(String from, String to) {
        FilteredOrders filtered = resolveFilteredOrders(from, to);
        List<Orders> orders = filtered.invoices();
        BigDecimal revenue = sumTotal(orders);
        BigDecimal subTotal = orders.stream()
                .map(o -> o.getSubTotal() != null ? o.getSubTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidCount = orders.stream().filter(o -> "PAID".equalsIgnoreCase(o.getPaymentStatus())).count();
        BigDecimal avg = orders.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(orders.size()), 0, RoundingMode.HALF_UP);

        Map<String, Object> body = new HashMap<>();
        body.put("from", filtered.from());
        body.put("to", filtered.to());
        body.put("orderCount", orders.size());
        body.put("invoiceCount", orders.size());
        body.put("totalRevenue", revenue);
        body.put("subTotal", subTotal);
        body.put("paidCount", paidCount);
        body.put("unpaidCount", orders.size() - paidCount);
        body.put("averageOrderValue", avg);
        body.put("totalOrdersAll", filtered.allOrders().size());
        body.put("excludedCancelled", filtered.cancelledCount());
        body.put("excludedOutOfRange", filtered.outOfRange());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueSeries(String period, String from, String to) {
        String bucket = period == null ? "month" : period.trim().toLowerCase(Locale.ROOT);
        FilteredOrders filtered = resolveFilteredOrders(from, to);
        List<Orders> orders = filtered.invoices();

        LocalDate rangeStart = LocalDate.parse(filtered.from());
        LocalDate rangeEnd = LocalDate.parse(filtered.to());
        Map<String, BucketAgg> grouped = initBuckets(bucket, rangeStart, rangeEnd);

        for (Orders order : orders) {
            LocalDate date = orderInstant(order);
            if (date == null) {
                continue;
            }
            String key = bucketKey(bucket, date);
            grouped.computeIfAbsent(key, k -> new BucketAgg()).add(order);
        }

        List<String> labels = new ArrayList<>(grouped.keySet());
        sortLabels(bucket, labels);

        List<Number> revenueData = labels.stream().map(k -> grouped.get(k).revenue).collect(Collectors.toList());
        List<Integer> countData = labels.stream().map(k -> grouped.get(k).count).collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("period", bucket);
        body.put("labels", labels);
        body.put("revenue", revenueData);
        body.put("orderCounts", countData);
        body.put("from", filtered.from());
        body.put("to", filtered.to());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getStatusBreakdown(String from, String to) {
        FilteredOrders filtered = resolveFilteredOrders(from, to);
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        Map<String, Integer> paymentCounts = new LinkedHashMap<>();

        for (Orders order : filtered.invoices()) {
            String status = order.getOrderStatus() != null ? order.getOrderStatus() : "UNKNOWN";
            String payment = order.getPaymentStatus() != null ? order.getPaymentStatus() : "UNKNOWN";
            statusCounts.merge(status, 1, Integer::sum);
            paymentCounts.merge(payment, 1, Integer::sum);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("orderStatus", statusCounts);
        body.put("paymentStatus", paymentCounts);
        body.put("from", filtered.from());
        body.put("to", filtered.to());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listInvoices(String from, String to, int page, int size) {
        FilteredOrders filtered = resolveFilteredOrders(from, to);
        List<Orders> orders = filtered.invoices().stream()
                .sorted(Comparator.comparing(Orders::getCreateDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Orders::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());

        int safeSize = Math.max(1, Math.min(size, 500));
        int safePage = Math.max(0, page);
        int fromIndex = Math.min(safePage * safeSize, orders.size());
        int toIndex = Math.min(fromIndex + safeSize, orders.size());
        List<Map<String, Object>> items = orders.subList(fromIndex, toIndex).stream()
                .map(this::toInvoiceRow)
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("page", safePage);
        body.put("size", safeSize);
        body.put("totalElements", orders.size());
        body.put("totalPages", orders.isEmpty() ? 0 : (int) Math.ceil(orders.size() / (double) safeSize));
        body.put("totalRevenue", sumTotal(orders));
        body.put("totalOrdersAll", filtered.allOrders().size());
        body.put("excludedCancelled", filtered.cancelledCount());
        body.put("excludedOutOfRange", filtered.outOfRange());
        body.put("from", filtered.from());
        body.put("to", filtered.to());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getInvoicesByUser(String from, String to) {
        FilteredOrders filtered = resolveFilteredOrders(from, to);
        Map<String, UserAgg> byUser = new LinkedHashMap<>();

        for (Orders order : filtered.invoices()) {
            String key = accountKey(order);
            byUser.computeIfAbsent(key, k -> new UserAgg(order)).addInvoice(toInvoiceRow(order));
        }

        List<Map<String, Object>> users = byUser.values().stream()
                .sorted(Comparator.comparing(UserAgg::totalSpent).reversed()
                        .thenComparing(UserAgg::invoiceCount, Comparator.reverseOrder()))
                .map(UserAgg::toMap)
                .collect(Collectors.toList());

        int totalInvoices = filtered.invoices().size();
        Map<String, Object> body = new HashMap<>();
        body.put("users", users);
        body.put("totalUsers", users.size());
        body.put("totalInvoices", totalInvoices);
        body.put("totalRevenue", sumTotal(filtered.invoices()));
        body.put("from", filtered.from());
        body.put("to", filtered.to());
        return body;
    }

    private FilteredOrders resolveFilteredOrders(String from, String to) {
        List<Orders> allOrders = ordersRepository.findAll();
        LocalDate startDate = parseFromDate(from);
        LocalDate endDate = parseToDate(to);
        Instant start = startDate.atStartOfDay(ZONE).toInstant();
        Instant end = endDate.plusDays(1).atStartOfDay(ZONE).toInstant();

        long cancelledCount = allOrders.stream()
                .filter(o -> "CANCELLED".equalsIgnoreCase(o.getOrderStatus()))
                .count();

        List<Orders> activeOrders = allOrders.stream()
                .filter(o -> o.getOrderStatus() == null || !"CANCELLED".equalsIgnoreCase(o.getOrderStatus()))
                .collect(Collectors.toList());

        List<Orders> inRange = activeOrders.stream()
                .filter(o -> isWithinRange(o, start, end))
                .collect(Collectors.toList());

        long outOfRange = activeOrders.size() - inRange.size();

        return new FilteredOrders(
                allOrders,
                inRange,
                cancelledCount,
                outOfRange,
                startDate.toString(),
                endDate.toString());
    }

    private Map<String, Object> toInvoiceRow(Orders order) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", order.getId());
        row.put("invoiceCode", "HD-" + String.format("%06d", order.getId()));
        row.put("accountId", order.getAccount() != null ? order.getAccount().getId() : null);
        row.put("username", order.getAccount() != null ? order.getAccount().getUsername() : null);
        row.put("email", order.getAccount() != null ? order.getAccount().getEmail() : null);
        row.put("createDate", order.getCreateDate());
        row.put("orderStatus", order.getOrderStatus());
        row.put("paymentStatus", order.getPaymentStatus());
        row.put("subTotal", order.getSubTotal());
        row.put("discountAmount", order.getDiscountAmount());
        row.put("totalAmount", order.getTotalAmount());
        shipmentRepository.findByOrder_Id(order.getId()).ifPresent(shipment -> {
            row.put("shippingFee", shipment.getShippingFee());
            if (shipment.getCarrier() != null) {
                row.put("carrierName", shipment.getCarrier().getName());
            }
        });
        return row;
    }

    private String accountKey(Orders order) {
        if (order.getAccount() != null && order.getAccount().getId() != null) {
            return "id:" + order.getAccount().getId();
        }
        if (order.getAccount() != null && order.getAccount().getUsername() != null) {
            return "user:" + order.getAccount().getUsername();
        }
        return "order:" + order.getId();
    }

    private LocalDate orderInstant(Orders order) {
        if (order.getCreateDate() == null) {
            return null;
        }
        return order.getCreateDate().atZone(ZONE).toLocalDate();
    }

    private boolean isWithinRange(Orders order, Instant start, Instant end) {
        if (order.getCreateDate() == null) {
            return true;
        }
        Instant created = order.getCreateDate();
        return !created.isBefore(start) && created.isBefore(end);
    }

    private LocalDate parseFromDate(String from) {
        if (from == null || from.isBlank()) {
            return ALL_TIME_START;
        }
        return LocalDate.parse(from.trim());
    }

    private LocalDate parseToDate(String to) {
        if (to == null || to.isBlank()) {
            return LocalDate.now(ZONE);
        }
        return LocalDate.parse(to.trim());
    }

    private Map<String, BucketAgg> initBuckets(String bucket, LocalDate start, LocalDate end) {
        Map<String, BucketAgg> grouped = new LinkedHashMap<>();
        switch (bucket) {
            case "day", "ngay" -> {
                LocalDate cursor = start;
                while (!cursor.isAfter(end)) {
                    grouped.put(DAY_FMT.format(cursor), new BucketAgg());
                    cursor = cursor.plusDays(1);
                }
            }
            case "year", "nam" -> {
                for (int year = start.getYear(); year <= end.getYear(); year++) {
                    grouped.put(String.valueOf(year), new BucketAgg());
                }
            }
            default -> {
                YearMonth cursor = YearMonth.from(start);
                YearMonth last = YearMonth.from(end);
                while (!cursor.isAfter(last)) {
                    grouped.put(MONTH_FMT.format(cursor), new BucketAgg());
                    cursor = cursor.plusMonths(1);
                }
            }
        }
        return grouped;
    }

    private String bucketKey(String bucket, LocalDate date) {
        return switch (bucket) {
            case "day", "ngay" -> DAY_FMT.format(date);
            case "year", "nam" -> String.valueOf(date.getYear());
            default -> MONTH_FMT.format(YearMonth.from(date));
        };
    }

    private void sortLabels(String bucket, List<String> labels) {
        if ("year".equals(bucket) || "nam".equals(bucket)) {
            labels.sort(Comparator.comparingInt(Integer::parseInt));
            return;
        }
        if ("day".equals(bucket) || "ngay".equals(bucket)) {
            labels.sort(Comparator.comparing(label -> LocalDate.parse(label, DAY_FMT)));
            return;
        }
        labels.sort(Comparator.comparing(label -> YearMonth.parse(label, MONTH_FMT)));
    }

    private BigDecimal sumTotal(List<Orders> orders) {
        return orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static final class BucketAgg {
        private BigDecimal revenue = BigDecimal.ZERO;
        private int count;

        void add(Orders order) {
            count++;
            if (order.getTotalAmount() != null) {
                revenue = revenue.add(order.getTotalAmount());
            }
        }
    }

    private static final class UserAgg {
        private final Long accountId;
        private final String username;
        private final String email;
        private int invoiceCount;
        private BigDecimal totalSpent = BigDecimal.ZERO;
        private final List<Map<String, Object>> invoices = new ArrayList<>();

        UserAgg(Orders order) {
            if (order.getAccount() != null) {
                this.accountId = order.getAccount().getId();
                this.username = order.getAccount().getUsername();
                this.email = order.getAccount().getEmail();
            } else {
                this.accountId = null;
                this.username = null;
                this.email = null;
            }
        }

        void addInvoice(Map<String, Object> invoice) {
            invoiceCount++;
            Object amount = invoice.get("totalAmount");
            if (amount instanceof BigDecimal bigDecimal) {
                totalSpent = totalSpent.add(bigDecimal);
            } else if (amount instanceof Number number) {
                totalSpent = totalSpent.add(BigDecimal.valueOf(number.doubleValue()));
            }
            invoices.add(invoice);
        }

        int invoiceCount() {
            return invoiceCount;
        }

        BigDecimal totalSpent() {
            return totalSpent;
        }

        Map<String, Object> toMap() {
            invoices.sort(Comparator.comparing(
                    (Map<String, Object> row) -> (Instant) row.get("createDate"),
                    Comparator.nullsLast(Comparator.reverseOrder())));

            Map<String, Object> row = new HashMap<>();
            row.put("accountId", accountId);
            row.put("username", username);
            row.put("email", email);
            row.put("invoiceCount", invoiceCount);
            row.put("totalSpent", totalSpent);
            row.put("invoices", invoices);
            return row;
        }
    }

    private record FilteredOrders(
            List<Orders> allOrders,
            List<Orders> invoices,
            long cancelledCount,
            long outOfRange,
            String from,
            String to) {
    }
}
