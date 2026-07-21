package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.OrderAddresses;
import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Shipments;
import poly.edu.ASSM.Repository.OrderAddressRepository;
import poly.edu.ASSM.Repository.OrderDetailsRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.Repository.ShipmentRepository;
import poly.edu.ASSM.domain.OrderStatus;

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

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMyOrders(String username) {
        requireUsername(username);
        List<Orders> orders = ordersRepository.findByAccount_UsernameOrderByCreateDateDesc(username);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Orders order : orders) {
            result.add(toSummary(order));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMyOrderDetail(String username, int orderId) {
        requireUsername(username);
        Orders order = ordersRepository.findByIdAndAccount_Username(orderId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng"));

        Map<String, Object> body = new HashMap<>();
        body.put("id", order.getId());
        body.put("createDate", order.getCreateDate());
        body.put("orderStatus", order.getOrderStatus());
        body.put("paymentStatus", order.getPaymentStatus());
        body.put("subTotal", order.getSubTotal());
        body.put("discountAmount", order.getDiscountAmount());
        body.put("totalAmount", order.getTotalAmount());
        body.put("canCancel", canCustomerCancel(order.getOrderStatus()));
        body.put("items", toItems(order));

        orderAddressRepository.findByOrder_Id(orderId)
                .ifPresent(addr -> body.put("shippingAddress", toShippingAddressMap(addr)));

        shipmentRepository.findByOrder_Id(orderId)
                .ifPresent(shipment -> body.put("shipment", toShipmentMap(shipment)));

        return body;
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

    private static boolean canCustomerCancel(String orderStatus) {
        try {
            return OrderStatus.parse(orderStatus).canTransitionTo(OrderStatus.CANCELLED);
        } catch (Exception e) {
            return false;
        }
    }

    private static void requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
    }

    private Map<String, Object> toSummary(Orders order) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", order.getId());
        m.put("createDate", order.getCreateDate());
        m.put("updateDate", order.getUpdateDate());
        m.put("orderStatus", order.getOrderStatus());
        m.put("paymentStatus", order.getPaymentStatus());
        m.put("subTotal", order.getSubTotal());
        m.put("discountAmount", order.getDiscountAmount());
        m.put("totalAmount", order.getTotalAmount());
        m.put("itemCount", order.getOrderDetails() != null ? order.getOrderDetails().size() : 0);
        m.put("canCancel", canCustomerCancel(order.getOrderStatus()));

        orderAddressRepository.findByOrder_Id(order.getId()).ifPresent(addr -> {
            m.put("address", formatAddress(addr));
        });
        return m;
    }

    private List<Map<String, Object>> toItems(Orders order) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (order.getOrderDetails() == null) {
            return items;
        }
        for (OrderDetails detail : order.getOrderDetails()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", detail.getId());
            row.put("quantity", detail.getQuantity());
            row.put("price", detail.getPrice());
            double line = (detail.getPrice() != null ? detail.getPrice() : 0)
                    * (detail.getQuantity() != null ? detail.getQuantity() : 0);
            row.put("lineTotal", line);

            ProductVariants variant = detail.getVariant();
            if (variant != null) {
                row.put("variantId", variant.getId());
                row.put("size", variant.getSize());
                row.put("color", variant.getColor());
                row.put("sku", variant.getSku());
                Products product = variant.getProduct();
                if (product != null) {
                    row.put("productId", product.getId());
                    row.put("productName", product.getName());
                }
            }
            items.add(row);
        }
        return items;
    }

    private Map<String, Object> toShippingAddressMap(OrderAddresses addr) {
        Map<String, Object> m = new HashMap<>();
        m.put("receiverName", addr.getReceiverName());
        m.put("receiverPhone", addr.getReceiverPhone());
        m.put("province", addr.getProvince());
        m.put("ward", addr.getWard());
        m.put("addressDetail", addr.getAddressDetail());
        return m;
    }

    private Map<String, Object> toShipmentMap(Shipments shipment) {
        Map<String, Object> m = new HashMap<>();
        m.put("shippingStatus", shipment.getShippingStatus());
        m.put("trackingNumber", shipment.getTrackingNumber());
        m.put("shippingFee", shipment.getShippingFee());
        m.put("notes", shipment.getNotes());
        if (shipment.getCarrier() != null) {
            m.put("carrierName", shipment.getCarrier().getName());
            m.put("carrierCode", shipment.getCarrier().getCode());
        }
        return m;
    }

    private static String formatAddress(OrderAddresses addr) {
        return String.join(", ",
                nullToEmpty(addr.getAddressDetail()),
                nullToEmpty(addr.getWard()),
                nullToEmpty(addr.getProvince()));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
