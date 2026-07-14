package poly.edu.ASSM.controller;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import poly.edu.ASSM.Services.core.OrderDetailsService;
import poly.edu.ASSM.Services.core.OrdersService;

@Controller
@RequestMapping("/admin/order")
public class AdminOrderController {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailsService orderDetailsService;

    // ================== LIST ==================
    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", ordersService.findAll());
        model.addAttribute("todayCount", ordersService.countTodayOrders());
        return "admin/order";
    }

    // ================== DETAIL ==================
    @GetMapping("/{id}")
    public String detail(@PathVariable int id, Model model) {
        model.addAttribute("orders", ordersService.findAll());
        model.addAttribute("orderDetails", orderDetailsService.findByOrder(id));
        model.addAttribute("selectedOrderId", id);
        model.addAttribute("todayCount", ordersService.countTodayOrders());
        return "admin/order";
    }

    // ================== SEARCH BY DATE ==================
    @GetMapping("/search/date")
    public String searchByDate(@RequestParam("date") String date, Model model) {
        LocalDate localDate = LocalDate.parse(date);
        model.addAttribute("orders", ordersService.findByDate(localDate));
        model.addAttribute("todayCount", ordersService.countTodayOrders());
        return "admin/order";
    }

    // ================== SEARCH BY RANGE ==================
    @GetMapping("/search/range")
    public String searchByRange(
            @RequestParam("from") String from,
            @RequestParam("to") String to,
            Model model) {

        model.addAttribute("orders",
                ordersService.findByDateRange(
                        LocalDate.parse(from),
                        LocalDate.parse(to)));
        model.addAttribute("todayCount", ordersService.countTodayOrders());
        return "admin/order";
    }

    // ================== UPDATE STATUS ==================
    @PostMapping("/update-status")
    public String updateStatus(
            @RequestParam int orderId,
            @RequestParam String status) {

        ordersService.updateStatus(orderId, status);

        // 🔥 FIX LỖI CHÍNH Ở ĐÂY
        return "redirect:/admin/order/" + orderId;
    }

    // ================== DELETE ==================
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable int id) {
        orderDetailsService.deleteByOrder(id);
        ordersService.delete(id);
        return "redirect:/admin/order";
    }
    
}
