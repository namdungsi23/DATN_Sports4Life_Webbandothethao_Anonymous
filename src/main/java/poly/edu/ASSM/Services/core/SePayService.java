package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.util.Map;

import poly.edu.ASSM.Entity.Orders;

public interface SePayService {

    String buildInvoiceNumber(int orderId);

    Integer parseOrderId(String invoiceNumber);

    Map<String, Object> buildCheckoutForm(Orders order, String customerId);

    void handleIpn(Map<String, Object> payload, String secretHeader);

    Map<String, Object> getPaymentStatus(int orderId, String username);

    Map<String, Object> resumeCheckout(int orderId, String username);

    Map<String, Object> syncPaymentStatus(int orderId, String username);

    /** Webhook biến động số dư ngân hàng (my.sepay.vn → Webhooks). */
    void handleBankWebhook(Map<String, Object> payload, String authorizationHeader);

    /** Tự nhận diện IPN cổng TT vs webhook biến động số dư. */
    void handleIncomingWebhook(Map<String, Object> payload, String secretHeader, String authorizationHeader);

    static boolean isBankTransferWebhook(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return false;
        }
        return payload.containsKey("transferAmount")
                || payload.containsKey("transfer_amount")
                || payload.containsKey("transferType")
                || payload.containsKey("transfer_type");
    }
}
