package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import poly.edu.ASSM.dto.response.CustomerOrderDetailResponse;
import poly.edu.ASSM.dto.response.CustomerOrderSummaryResponse;

public interface CustomerOrderService {
    List<CustomerOrderSummaryResponse> listMyOrders(String username);

    CustomerOrderDetailResponse getMyOrderDetail(String username, int orderId);

    /** User hủy đơn (PENDING / CONFIRMED) — hoàn tồn + thông báo chuông. */
    Map<String, Object> cancelMyOrder(String username, int orderId);
}
