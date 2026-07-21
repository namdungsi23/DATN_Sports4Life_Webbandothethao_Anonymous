package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

public interface CustomerOrderService {
    List<Map<String, Object>> listMyOrders(String username);

    List<Map<String, Object>> listPendingPayments(String username);

    Map<String, Object> getMyOrderDetail(String username, int orderId);
}
