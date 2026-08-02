package poly.edu.ASSM.mapper;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.dto.response.CheckoutConfirmResponse;
import poly.edu.ASSM.dto.response.CustomerAddressResponse;
import poly.edu.ASSM.dto.response.OrderAddressResponse;
import poly.edu.ASSM.dto.response.SePayCheckoutFormResponse;
import poly.edu.ASSM.dto.response.VoucherPreviewResponse;

@Component
public class CheckoutMapper {

    public CheckoutConfirmResponse toConfirmResponse(
            String message,
            Integer orderId,
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String voucherCode,
            String discountType,
            OrderAddressResponse orderAddress,
            CustomerAddressResponse savedAddress,
            String paymentMethod,
            Boolean paymentPending,
            SePayCheckoutFormResponse sepay,
            String paymentCompletionToken) {
        return CheckoutConfirmResponse.builder()
                .message(message)
                .orderId(orderId)
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .voucherCode(voucherCode)
                .discountType(discountType)
                .orderAddress(orderAddress)
                .savedAddress(savedAddress)
                .paymentMethod(paymentMethod)
                .paymentPending(paymentPending)
                .sepay(sepay)
                .paymentCompletionToken(paymentCompletionToken)
                .build();
    }

    public CheckoutConfirmResponse toConfirmResponse(
            String message,
            Integer orderId,
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            BigDecimal totalAmount,
            String voucherCode,
            String discountType,
            OrderAddressResponse orderAddress,
            CustomerAddressResponse savedAddress,
            String paymentMethod,
            Boolean paymentPending,
            SePayCheckoutFormResponse sepay) {
        return toConfirmResponse(
                message, orderId, subTotal, discountAmount, shippingFee, totalAmount,
                voucherCode, discountType, orderAddress, savedAddress,
                paymentMethod, paymentPending, sepay, null);
    }

    public VoucherPreviewResponse toVoucherPreviewResponse(
            String voucherCode,
            String voucherName,
            String discountType,
            BigDecimal subTotal,
            BigDecimal discountAmount,
            BigDecimal subtotalDiscount,
            BigDecimal shippingDiscount,
            BigDecimal shippingFee,
            BigDecimal totalAmount) {
        return VoucherPreviewResponse.builder()
                .valid(true)
                .voucherCode(voucherCode)
                .voucherName(voucherName)
                .discountType(discountType)
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .subtotalDiscount(subtotalDiscount)
                .shippingDiscount(shippingDiscount)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .build();
    }

    public CheckoutConfirmResponse fromMap(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        CheckoutConfirmResponse.CheckoutConfirmResponseBuilder builder = CheckoutConfirmResponse.builder()
                .message(stringValue(body.get("message")))
                .orderId(intValue(body.get("orderId")))
                .subTotal(decimalValue(body.get("subTotal")))
                .discountAmount(decimalValue(body.get("discountAmount")))
                .shippingFee(decimalValue(body.get("shippingFee")))
                .totalAmount(decimalValue(body.get("totalAmount")))
                .voucherCode(stringValue(body.get("voucherCode")))
                .discountType(stringValue(body.get("discountType")))
                .paymentMethod(stringValue(body.get("paymentMethod")))
                .paymentPending(boolValue(body.get("paymentPending")));

        Object orderAddress = body.get("orderAddress");
        if (orderAddress instanceof OrderAddressResponse response) {
            builder.orderAddress(response);
        }

        Object savedAddress = body.get("savedAddress");
        if (savedAddress instanceof CustomerAddressResponse response) {
            builder.savedAddress(response);
        }

        Object sepay = body.get("sepay");
        if (sepay instanceof SePayCheckoutFormResponse response) {
            builder.sepay(response);
        } else if (sepay instanceof Map<?, ?> map) {
            builder.sepay(toSePayCheckoutForm(map));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private SePayCheckoutFormResponse toSePayCheckoutForm(Map<?, ?> map) {
        Object fields = map.get("fields");
        Map<String, String> fieldMap = fields instanceof Map<?, ?> raw
                ? (Map<String, String>) raw
                : null;
        return SePayCheckoutFormResponse.builder()
                .action(stringValue(map.get("action")))
                .method(stringValue(map.get("method")))
                .fields(fieldMap)
                .invoiceNumber(stringValue(map.get("invoiceNumber")))
                .amount(longValue(map.get("amount")))
                .env(stringValue(map.get("env")))
                .ipnUrl(stringValue(map.get("ipnUrl")))
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
