package poly.edu.ASSM.mapper;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.dto.response.SePayCheckoutFormResponse;
import poly.edu.ASSM.dto.response.SePayPaymentStatusResponse;

@Component
public class SePayMapper {

    @SuppressWarnings("unchecked")
    public SePayCheckoutFormResponse toCheckoutFormResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object fields = body.get("fields");
        Map<String, String> fieldMap = fields instanceof Map<?, ?> raw
                ? (Map<String, String>) raw
                : null;
        return SePayCheckoutFormResponse.builder()
                .action(stringValue(body.get("action")))
                .method(stringValue(body.get("method")))
                .fields(fieldMap)
                .invoiceNumber(stringValue(body.get("invoiceNumber")))
                .amount(longValue(body.get("amount")))
                .env(stringValue(body.get("env")))
                .ipnUrl(stringValue(body.get("ipnUrl")))
                .build();
    }

    public SePayCheckoutFormResponse toCheckoutFormResponse(
            String action,
            String method,
            Map<String, String> fields,
            String invoiceNumber,
            long amount,
            String env,
            String ipnUrl) {
        return SePayCheckoutFormResponse.builder()
                .action(action)
                .method(method)
                .fields(fields)
                .invoiceNumber(invoiceNumber)
                .amount(amount)
                .env(env)
                .ipnUrl(ipnUrl)
                .build();
    }

    public SePayPaymentStatusResponse toPaymentStatusResponse(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        return SePayPaymentStatusResponse.builder()
                .orderId(intValue(body.get("orderId")))
                .paymentStatus(stringValue(body.get("paymentStatus")))
                .orderStatus(stringValue(body.get("orderStatus")))
                .totalAmount(decimalValue(body.get("totalAmount")))
                .invoiceNumber(stringValue(body.get("invoiceNumber")))
                .paid(boolValue(body.get("paid")))
                .build();
    }

    public SePayPaymentStatusResponse toPaymentStatusResponse(
            Integer orderId,
            String paymentStatus,
            String orderStatus,
            BigDecimal totalAmount,
            String invoiceNumber,
            boolean paid) {
        return SePayPaymentStatusResponse.builder()
                .orderId(orderId)
                .paymentStatus(paymentStatus)
                .orderStatus(orderStatus)
                .totalAmount(totalAmount)
                .invoiceNumber(invoiceNumber)
                .paid(paid)
                .build();
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

    private static Boolean boolValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
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
}
