package poly.edu.ASSM.Controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Services.core.AccountsServiceImpl;
import poly.edu.ASSM.Services.core.InventoryServiceImpl;
import poly.edu.ASSM.Services.core.OrderDetailsServiceImpl;
import poly.edu.ASSM.Services.core.OrdersServiceImpl;
import poly.edu.ASSM.Services.core.ProductServiceImpl;
import poly.edu.ASSM.Services.util.AuthServiceImpl;
import poly.edu.ASSM.Services.util.ShoppingCartServiceImpl;
import poly.edu.ASSM.domain.CartItem;
import poly.edu.ASSM.domain.PaymentMethod;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {
    @Autowired
    ShoppingCartServiceImpl cartService;

    @Autowired
    OrdersServiceImpl orderService;

    @Autowired
    AccountsServiceImpl accountService;

    @Autowired
    AuthServiceImpl authService;

    @Autowired
    ProductServiceImpl productService;

    @Autowired
    OrderDetailsServiceImpl odService;

    @Autowired
    InventoryServiceImpl inventoryService;

    @PostMapping("/pay")
    public String proceedPayment(@RequestParam PaymentMethod paymentMethod, RedirectAttributes redirect) {
        redirect.addAttribute("paymentMethod", paymentMethod);
        return "redirect:/payment";
    }

    @Transactional
    @PostMapping("/confirm")
    public String confirm(@RequestParam String username, @RequestParam String address, Model model) {
        Orders order = new Orders();
        order.setAddress(address);
        order.setUsername(username);
        order.setCreateDate(LocalDate.now());
        order.setStatus("NEW");
        order.setTotalAmount(java.math.BigDecimal.valueOf(cartService.getAmount()));
        orderService.create(order);

        for (CartItem item : cartService.getItems()) {
            OrderDetails orderDetail = new OrderDetails();
            orderDetail.setOrders(order);
            orderDetail.setPrice(item.getPrice());
            orderDetail.setQuantity(item.getQuantity());
            odService.create(orderDetail);
            try {
                inventoryService.checkAndUpdateInventory(item.getProductId(), item.getQuantity());
            } catch (DataAccessException e) {
                model.addAttribute("success", false);
                model.addAttribute("erMsg", e.getMessage());
            }
        }

        cartService.clear();
        model.addAttribute("success", true);
        return "page/cart/checkout-success";
    }
}
