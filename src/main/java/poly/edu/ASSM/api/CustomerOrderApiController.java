package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.CustomerOrderService;

@RestController
@RequestMapping("/api/orders")
public class CustomerOrderApiController {

    @Autowired
    private CustomerOrderService customerOrderService;

    @GetMapping
    public List<Map<String, Object>> list(Principal principal) {
        return customerOrderService.listMyOrders(principal.getName());
    }

    @GetMapping("/pending-payment")
    public List<Map<String, Object>> pendingPayments(Principal principal) {
        return customerOrderService.listPendingPayments(principal.getName());
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(Principal principal, @PathVariable int id) {
        return customerOrderService.getMyOrderDetail(principal.getName(), id);
    }
}
