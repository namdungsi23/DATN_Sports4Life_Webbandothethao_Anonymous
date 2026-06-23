package poly.edu.ASSM.api.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Services.core.OrderDetailsService;
import poly.edu.ASSM.Services.core.OrdersService;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("@adminAuth.has('ORDER_VIEW')")
public class AdminOrderApiController {

    @Autowired
    private OrdersService ordersService;
    @Autowired
    private OrderDetailsService orderDetailsService;

    public record UpdateStatusBody(int orderId, String status) {
    }

    private List<Map<String, Object>> toSummaries(List<Orders> orders) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Orders o : orders) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("username", o.getUsername() != null ? o.getUsername() : "");
            m.put("createDate", o.getCreateDate());
            m.put("address", o.getAddress());
            m.put("status", o.getStatus());
            m.put("totalAmount", o.getTotalAmount());
            list.add(m);
        }
        return list;
    }

    private List<Map<String, Object>> detailRows(int orderId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderDetails d : orderDetailsService.findByOrder(orderId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.getId());
            m.put("productName", "");
            m.put("price", d.getPrice());
            m.put("quantity", d.getQuantity());
            double line = (d.getPrice() != null ? d.getPrice() : 0) * (d.getQuantity() != null ? d.getQuantity() : 0);
            m.put("lineTotal", line);
            list.add(m);
        }
        return list;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of(
                "orders", toSummaries(ordersService.findAll()),
                "todayCount", ordersService.countTodayOrders());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable int id) {
        return Map.of(
                "orders", toSummaries(ordersService.findAll()),
                "todayCount", ordersService.countTodayOrders(),
                "selectedOrderId", id,
                "orderDetails", detailRows(id));
    }

    @PostMapping("/update-status")
    public ResponseEntity<Map<String, Object>> updateStatus(@RequestBody UpdateStatusBody body) {
        if (body == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false));
        }
        ordersService.updateStatus(body.orderId(), body.status());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
