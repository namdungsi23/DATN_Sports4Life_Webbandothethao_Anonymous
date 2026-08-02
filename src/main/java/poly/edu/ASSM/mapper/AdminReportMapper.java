package poly.edu.ASSM.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.dto.response.ReportInvoiceRowResponse;
import poly.edu.ASSM.dto.response.ReportInvoicesByUserResponse;
import poly.edu.ASSM.dto.response.ReportPageResponse;
import poly.edu.ASSM.dto.response.ReportSeriesResponse;
import poly.edu.ASSM.dto.response.ReportStatusBreakdownResponse;
import poly.edu.ASSM.dto.response.ReportSummaryResponse;
import poly.edu.ASSM.dto.response.ReportUserInvoicesResponse;

@Component
public class AdminReportMapper {

    public ReportSummaryResponse toSummaryResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return ReportSummaryResponse.builder()
                .from(stringValue(body.get("from")))
                .to(stringValue(body.get("to")))
                .orderCount(intValue(body.get("orderCount")))
                .invoiceCount(intValue(body.get("invoiceCount")))
                .totalRevenue(decimalValue(body.get("totalRevenue")))
                .subTotal(decimalValue(body.get("subTotal")))
                .paidCount(longValue(body.get("paidCount")))
                .unpaidCount(longValue(body.get("unpaidCount")))
                .averageOrderValue(decimalValue(body.get("averageOrderValue")))
                .totalOrdersAll(intValue(body.get("totalOrdersAll")))
                .excludedCancelled(longValue(body.get("excludedCancelled")))
                .excludedOutOfRange(longValue(body.get("excludedOutOfRange")))
                .build();
    }

    @SuppressWarnings("unchecked")
    public ReportSeriesResponse toSeriesResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object labels = body.get("labels");
        Object revenue = body.get("revenue");
        Object orderCounts = body.get("orderCounts");
        return ReportSeriesResponse.builder()
                .period(stringValue(body.get("period")))
                .labels(labels instanceof List<?> list
                        ? list.stream().map(String::valueOf).collect(Collectors.toList())
                        : List.of())
                .revenue(revenue instanceof List<?> list
                        ? list.stream().map(v -> (Number) v).collect(Collectors.toList())
                        : List.of())
                .orderCounts(orderCounts instanceof List<?> list
                        ? list.stream().map(AdminReportMapper::intValue).collect(Collectors.toList())
                        : List.of())
                .from(stringValue(body.get("from")))
                .to(stringValue(body.get("to")))
                .build();
    }

    @SuppressWarnings("unchecked")
    public ReportStatusBreakdownResponse toStatusBreakdownResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return ReportStatusBreakdownResponse.builder()
                .orderStatus(asIntegerMap(body.get("orderStatus")))
                .paymentStatus(asIntegerMap(body.get("paymentStatus")))
                .from(stringValue(body.get("from")))
                .to(stringValue(body.get("to")))
                .build();
    }

    @SuppressWarnings("unchecked")
    public ReportPageResponse toPageResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object items = body.get("items");
        List<ReportInvoiceRowResponse> rows = new ArrayList<>();
        if (items instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(toInvoiceRow((Map<String, Object>) map));
                } else if (item instanceof ReportInvoiceRowResponse response) {
                    rows.add(response);
                }
            }
        }
        return ReportPageResponse.builder()
                .items(rows)
                .page(intValue(body.get("page")))
                .size(intValue(body.get("size")))
                .totalElements(intValue(body.get("totalElements")))
                .totalPages(intValue(body.get("totalPages")))
                .totalRevenue(decimalValue(body.get("totalRevenue")))
                .totalOrdersAll(intValue(body.get("totalOrdersAll")))
                .excludedCancelled(longValue(body.get("excludedCancelled")))
                .excludedOutOfRange(longValue(body.get("excludedOutOfRange")))
                .from(stringValue(body.get("from")))
                .to(stringValue(body.get("to")))
                .build();
    }

    @SuppressWarnings("unchecked")
    public ReportInvoicesByUserResponse toInvoicesByUserResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object users = body.get("users");
        List<ReportUserInvoicesResponse> userRows = new ArrayList<>();
        if (users instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    userRows.add(toUserInvoices((Map<String, Object>) map));
                } else if (item instanceof ReportUserInvoicesResponse response) {
                    userRows.add(response);
                }
            }
        }
        return ReportInvoicesByUserResponse.builder()
                .users(userRows)
                .totalUsers(intValue(body.get("totalUsers")))
                .totalInvoices(intValue(body.get("totalInvoices")))
                .totalRevenue(decimalValue(body.get("totalRevenue")))
                .from(stringValue(body.get("from")))
                .to(stringValue(body.get("to")))
                .build();
    }

    @SuppressWarnings("unchecked")
    public ReportUserInvoicesResponse toUserInvoices(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object invoices = body.get("invoices");
        List<ReportInvoiceRowResponse> rows = new ArrayList<>();
        if (invoices instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    rows.add(toInvoiceRow((Map<String, Object>) map));
                } else if (item instanceof ReportInvoiceRowResponse response) {
                    rows.add(response);
                }
            }
        }
        return ReportUserInvoicesResponse.builder()
                .accountId(longValue(body.get("accountId")))
                .username(stringValue(body.get("username")))
                .email(stringValue(body.get("email")))
                .invoiceCount(intValue(body.get("invoiceCount")))
                .totalSpent(decimalValue(body.get("totalSpent")))
                .invoices(rows)
                .build();
    }

    public ReportInvoiceRowResponse toInvoiceRow(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return ReportInvoiceRowResponse.builder()
                .id(intValue(row.get("id")))
                .invoiceCode(stringValue(row.get("invoiceCode")))
                .accountId(longValue(row.get("accountId")))
                .username(stringValue(row.get("username")))
                .email(stringValue(row.get("email")))
                .createDate(instantValue(row.get("createDate")))
                .orderStatus(stringValue(row.get("orderStatus")))
                .paymentStatus(stringValue(row.get("paymentStatus")))
                .subTotal(decimalValue(row.get("subTotal")))
                .discountAmount(decimalValue(row.get("discountAmount")))
                .totalAmount(decimalValue(row.get("totalAmount")))
                .shippingFee(decimalValue(row.get("shippingFee")))
                .carrierName(stringValue(row.get("carrierName")))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> asIntegerMap(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        return raw.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> e.getValue() instanceof Number number
                                ? number.intValue()
                                : Integer.parseInt(String.valueOf(e.getValue()))));
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer intValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
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
            return null;
        }
    }

    private static Instant instantValue(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        return null;
    }
}
