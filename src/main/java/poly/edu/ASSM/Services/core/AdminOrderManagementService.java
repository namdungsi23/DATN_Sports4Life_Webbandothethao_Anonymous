package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import poly.edu.ASSM.dto.request.AdminOrderUpdateRequest;
import poly.edu.ASSM.dto.response.CarrierResponse;

public interface AdminOrderManagementService {

    List<Map<String, Object>> listOrderSummaries();

    List<Map<String, Object>> listOrderSummaries(String keyword);

    Map<String, Object> getOrderDetail(int orderId);

    Map<String, Object> getPendingAlerts();

    List<CarrierResponse> listCarriers();

    Map<String, Object> updateOrder(AdminOrderUpdateRequest request);

    Map<String, Object> confirmOrder(int orderId);
}
