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
}
