package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Repository.OrderAddressRepository;
import poly.edu.ASSM.Repository.OrderDetailsRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.Repository.ShipmentRepository;
import poly.edu.ASSM.domain.OrderStatus;
import poly.edu.ASSM.dto.response.CustomerOrderDetailResponse;
import poly.edu.ASSM.dto.response.CustomerOrderSummaryResponse;
import poly.edu.ASSM.mapper.CustomerOrderMapper;

@Service
public class CustomerOrderServiceImpl implements CustomerOrderService {

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private OrderAddressRepository orderAddressRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private OrderDetailsRepository orderDetailsRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private AdminNotificationService notificationService;

    @Autowired
    private CustomerOrderMapper customerOrderMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CustomerOrderSummaryResponse> listMyOrders(String username) {
        requireUsername(username);
        List<Orders> orders = ordersRepository.findByAccount_UsernameOrderByCreateDateDesc(username);
        return customerOrderMapper.toSummaryList(orders,
                id -> orderAddressRepository.findByOrder_Id(id).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerOrderDetailResponse getMyOrderDetail(String username, int orderId) {
        requireUsername(username);
        Orders order = ordersRepository.findByIdAndAccount_Username(orderId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        return customerOrderMapper.toDetail(
                order,
                orderAddressRepository.findByOrder_Id(orderId).orElse(null),
                shipmentRepository.findByOrder_Id(orderId).orElse(null));
    }

    @Override
    @Transactional
    public Map<String, Object> cancelMyOrder(String username, int orderId) {
        requireUsername(username);
        Orders order = ordersRepository.findByIdAndAccount_Username(orderId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        OrderStatus current;
        try {
            current = OrderStatus.parse(order.getOrderStatus());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        if (!current.canTransitionTo(OrderStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể hủy đơn ở trạng thái «" + current.getLabel() + "». "
                            + "Chỉ hủy được khi đơn còn Chờ xác nhận hoặc Đã xác nhận.");
        }

        restockOrder(orderId);
        order.setOrderStatus(OrderStatus.CANCELLED.name());
        order.setUpdateDate(Instant.now());
        ordersRepository.save(order);

        String title = "Đã hủy đơn hàng";
        String message = "Bạn đã hủy đơn #" + orderId + " thành công. Tồn kho đã được hoàn.";
        String link = "/profile?tab=orders";
        try {
            notificationService.notifyUser(username, title, message, link);
            notificationService.notifyPanelUsers(
                    "Khách hủy đơn #" + orderId,
                    "Tài khoản «" + username + "» vừa hủy đơn #" + orderId + ".",
                    "/admin/order/" + orderId);
        } catch (Exception ignored) {
            // không làm fail hủy đơn nếu notify lỗi
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("message", "Đã hủy đơn hàng #" + orderId + ".");
        body.put("order", getMyOrderDetail(username, orderId));
        return body;
    }

    private void restockOrder(int orderId) {
        for (OrderDetails detail : orderDetailsRepository.findByOrders_Id(orderId)) {
            ProductVariants variant = detail.getVariant();
            if (variant == null || detail.getQuantity() == null || detail.getQuantity() <= 0) {
                continue;
            }
            productVariantRepository.restock(variant.getId(), detail.getQuantity());
        }
    }

    private static void requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
    }
}
