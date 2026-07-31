package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

public interface CustomerOrderService {
    List<Map<String, Object>> listMyOrders(String username);

    Map<String, Object> getMyOrderDetail(String username, int orderId);

    /** User hủy đơn (PENDING / CONFIRMED) — hoàn tồn + thông báo chuông. */
    Map<String, Object> cancelMyOrder(String username, int orderId);
}