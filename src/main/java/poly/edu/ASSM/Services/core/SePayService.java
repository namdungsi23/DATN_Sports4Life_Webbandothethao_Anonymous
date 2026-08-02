package poly.edu.ASSM.Services.core;

import java.util.Map;

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.dto.response.SePayCheckoutFormResponse;
import poly.edu.ASSM.dto.response.SePayPaymentStatusResponse;

public interface SePayService {

    String buildInvoiceNumber(int orderId);

    Integer parseOrderId(String invoiceNumber);

    SePayCheckoutFormResponse buildCheckoutForm(Orders order, String customerId, String returnBaseUrl);

    String createPaymentCompletionToken(int orderId);

    void handleIpn(Map<String, Object> payload, String secretHeader);

    /** Webhook chuyển khoản ngân hàng (QR) — SePay gửi khi có tiền vào. */
    void handleBankWebhook(Map<String, Object> payload, String authHeader);

    SePayPaymentStatusResponse getPaymentStatus(int orderId, String username);

    SePayPaymentStatusResponse completePayment(int orderId, String username, boolean fromGatewayReturn);

    /** Hoàn tất thanh toán khi quay về từ SePay (không cần đăng nhập). */
    SePayPaymentStatusResponse completePaymentFromGateway(int orderId, String completionToken);
}
