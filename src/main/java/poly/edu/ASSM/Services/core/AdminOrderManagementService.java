package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import poly.edu.ASSM.dto.request.AdminOrderUpdateRequest;

public interface AdminOrderManagementService {

    List<Map<String, Object>> listOrderSummaries();

    Map<String, Object> getOrderDetail(int orderId);

    Map<String, Object> getPendingAlerts();

    List<Map<String, Object>> listCarriers();

    Map<String, Object> updateOrder(AdminOrderUpdateRequest request);

    Map<String, Object> confirmOrder(int orderId);
}
