package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminOrderManagementService;
import poly.edu.ASSM.Services.core.OrdersService;
import poly.edu.ASSM.dto.request.AdminOrderUpdateRequest;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("@adminAuth.has('ORDER_VIEW')")
public class AdminOrderApiController {

    @Autowired
    private AdminOrderManagementService orderManagementService;

    @Autowired
    private OrdersService ordersService;

    @GetMapping
    public Map<String, Object> list() {
        return Map.of(
                "orders", orderManagementService.listOrderSummaries(),
                "todayCount", ordersService.countTodayOrders(),
                "pendingCount", ordersService.countPendingOrders());
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        return orderManagementService.getPendingAlerts();
    }

    @GetMapping("/carriers")
    public Map<String, Object> carriers() {
        return Map.of("carriers", orderManagementService.listCarriers());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable int id) {
        return orderManagementService.getOrderDetail(id);
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("@adminAuth.has('ORDER_UPDATE')")
    public ResponseEntity<Map<String, Object>> confirm(@PathVariable int id) {
        return ResponseEntity.ok(orderManagementService.confirmOrder(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@adminAuth.has('ORDER_UPDATE')")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable int id,
            @RequestBody AdminOrderUpdateRequest body) {
        if (body == null) {
            body = new AdminOrderUpdateRequest();
        }
        body.setOrderId(id);
        return ResponseEntity.ok(orderManagementService.updateOrder(body));
    }

    /** @deprecated use PUT /{id} */
    @PostMapping("/update-status")
    @PreAuthorize("@adminAuth.has('ORDER_UPDATE')")
    public ResponseEntity<Map<String, Object>> updateStatusLegacy(@RequestBody Map<String, Object> body) {
        AdminOrderUpdateRequest request = new AdminOrderUpdateRequest();
        if (body != null) {
            Object orderId = body.get("orderId");
            if (orderId instanceof Number number) {
                request.setOrderId(number.intValue());
            }
            Object status = body.get("status");
            if (status != null) {
                request.setOrderStatus(String.valueOf(status));
            }
        }
        if (request.getOrderId() == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Thiếu orderId"));
        }
        orderManagementService.updateOrder(request);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
