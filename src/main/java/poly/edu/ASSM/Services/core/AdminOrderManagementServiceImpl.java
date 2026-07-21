package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Carriers;
import poly.edu.ASSM.Entity.OrderAddresses;
import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Shipments;
import poly.edu.ASSM.Repository.CarrierRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.Repository.ShipmentRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.domain.OrderStatus;
import poly.edu.ASSM.domain.PaymentStatus;
import poly.edu.ASSM.domain.ShippingStatus;
import poly.edu.ASSM.dto.request.AdminOrderUpdateRequest;

@Service
public class AdminOrderManagementServiceImpl implements AdminOrderManagementService {

    @Autowired
    private OrdersService ordersService;

    @Autowired
    private OrderDetailsService orderDetailsService;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private CarrierRepository carrierRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private RankService rankService;

    @Autowired
    private UsersRepository usersRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrderSummaries() {
        return listOrderSummaries(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrderSummaries(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        List<Orders> source;
        if (kw.isEmpty()) {
            source = ordersService.findAll();
        } else {
            Integer idExact = null;
            try {
                idExact = Integer.valueOf(kw);
            } catch (NumberFormatException ignored) {
                /* not an id */
            }
            source = ordersRepository.searchByKeyword(kw, idExact, PageRequest.of(0, 200));
        }
        return source.stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(int orderId) {
        Orders order = ordersService.findById(orderId);
        Map<String, Object> body = new HashMap<>();
        body.put("order", toDetail(order));
        body.put("items", toDetailItems(orderId));
        body.put("allowedOrderStatuses", allowedOrderStatuses(order));
        body.put("allowedPaymentStatuses", allowedPaymentStatuses(order));
        OrderStatus orderStatus = OrderStatus.parse(order.getOrderStatus());
        if (orderStatus == OrderStatus.SHIPPING || orderStatus == OrderStatus.DELIVERED) {
            body.put("allowedShippingStatuses", allowedShippingStatuses());
        }
        shipmentRepository.findByOrder_Id(orderId).ifPresent(shipment ->
                body.put("shipment", toShipmentMap(shipment)));
        body.put("carriers", listCarriers());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPendingAlerts() {
        List<Orders> pending = ordersService.findByOrderStatus(OrderStatus.PENDING.name());
        List<Map<String, Object>> recent = pending.stream()
                .limit(5)
                .map(this::toSummary)
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("pendingCount", pending.size());
        body.put("recentPending", recent);
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCarriers() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Carriers carrier : carrierRepository.findByActiveTrueOrderByNameAsc()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", carrier.getId());
            m.put("name", carrier.getName());
            m.put("code", carrier.getCode());
            list.add(m);
        }
        return list;
    }

    @Override
    @Transactional
    public Map<String, Object> confirmOrder(int orderId) {
        AdminOrderUpdateRequest request = new AdminOrderUpdateRequest();
        request.setOrderId(orderId);
        request.setOrderStatus(OrderStatus.CONFIRMED.name());
        return updateOrder(request);
    }

    @Override
    @Transactional
    public Map<String, Object> updateOrder(AdminOrderUpdateRequest request) {
        if (request == null || request.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu mã đơn hàng");
        }

        Orders order = ordersService.findById(request.getOrderId());
        OrderStatus previousStatus = OrderStatus.parse(order.getOrderStatus());
        OrderStatus currentStatus = previousStatus;
        boolean statusChanged = false;

        if (request.getOrderStatus() != null && !request.getOrderStatus().isBlank()) {
            OrderStatus nextStatus = OrderStatus.parse(request.getOrderStatus());
            if (nextStatus != currentStatus) {
                validateOrderTransition(currentStatus, nextStatus);
                if (nextStatus == OrderStatus.CANCELLED) {
                    restockOrder(order.getId());
                }
                order.setOrderStatus(nextStatus.name());
                currentStatus = nextStatus;
                statusChanged = true;
            }
        }

        if (request.getPaymentStatus() != null && !request.getPaymentStatus().isBlank()) {
            PaymentStatus nextPayment = PaymentStatus.parse(request.getPaymentStatus());
            validatePaymentTransition(order, currentStatus, nextPayment);
            order.setPaymentStatus(nextPayment.name());
        }

        if (statusChanged) {
            order.setUpdateDate(Instant.now());
        }

        if (currentStatus == OrderStatus.SHIPPING
                || currentStatus == OrderStatus.DELIVERED) {
            ensureShipmentForUpdate(order, request, currentStatus);
        } else if (hasShipmentFields(request)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Chỉ cập nhật vận chuyển khi đơn ở trạng thái SHIPPING hoặc DELIVERED");
        }

        ordersService.update(order);

        // Cộng điểm hạng thành viên khi đơn giao thành công lần đầu
        if (statusChanged
                && currentStatus == OrderStatus.DELIVERED
                && previousStatus != OrderStatus.DELIVERED) {
            rankService.awardForDeliveredOrder(order);
        }

        return getOrderDetail(order.getId());
    }

    private void validateOrderTransition(OrderStatus current, OrderStatus next) {
        if (!current.allowedNext().contains(next)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không thể chuyển từ " + current.name() + " sang " + next.name());
        }
    }

    private void restockOrder(int orderId) {
        for (OrderDetails detail : orderDetailsService.findByOrder(orderId)) {
            ProductVariants variant = detail.getVariant();
            if (variant == null || detail.getQuantity() == null || detail.getQuantity() <= 0) {
                continue;
            }
            productVariantRepository.restock(variant.getId(), detail.getQuantity());
        }
    }

    private void validatePaymentTransition(Orders order, OrderStatus orderStatus, PaymentStatus nextPayment) {
        PaymentStatus currentPayment = PaymentStatus.parse(order.getPaymentStatus());

        if (currentPayment == PaymentStatus.PAID && nextPayment == PaymentStatus.UNPAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Đơn đã thanh toán trước không thể chuyển về UNPAID");
        }

        if (currentPayment == PaymentStatus.UNPAID
                && nextPayment == PaymentStatus.PAID
                && orderStatus != OrderStatus.DELIVERED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "COD chỉ được đánh dấu PAID khi đơn đã DELIVERED");
        }
    }

    private void ensureShipmentForUpdate(Orders order, AdminOrderUpdateRequest request, OrderStatus orderStatus) {
        Shipments shipment = shipmentRepository.findByOrder_Id(order.getId()).orElse(null);

        if (shipment == null) {
            if (orderStatus != OrderStatus.SHIPPING && orderStatus != OrderStatus.DELIVERED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Chưa thể tạo vận chuyển trước khi đơn chuyển sang SHIPPING");
            }
            shipment = new Shipments();
            shipment.setOrder(order);
            shipment.setShippingStatus(ShippingStatus.PENDING.name());
            shipment.setShippingFee(BigDecimal.ZERO);
            shipment.setCreatedAt(Instant.now());
        }

        applyShipmentFields(shipment, request);
        shipment.setUpdatedAt(Instant.now());
        shipmentRepository.save(shipment);
    }

    private boolean hasShipmentFields(AdminOrderUpdateRequest request) {
        return request.getCarrierId() != null
                || (request.getTrackingNumber() != null && !request.getTrackingNumber().isBlank())
                || (request.getShippingStatus() != null && !request.getShippingStatus().isBlank())
                || request.getShippingFee() != null
                || (request.getNotes() != null && !request.getNotes().isBlank());
    }

    private void applyShipmentFields(Shipments shipment, AdminOrderUpdateRequest request) {
        if (request.getCarrierId() != null) {
            Carriers carrier = carrierRepository.findById(request.getCarrierId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy đơn vị vận chuyển"));
            shipment.setCarrier(carrier);
        }

        if (request.getTrackingNumber() != null) {
            shipment.setTrackingNumber(request.getTrackingNumber().isBlank() ? null : request.getTrackingNumber().trim());
        }

        if (request.getShippingStatus() != null && !request.getShippingStatus().isBlank()) {
            ShippingStatus.parse(request.getShippingStatus());
            shipment.setShippingStatus(request.getShippingStatus().trim().toUpperCase());
        }

        if (request.getShippingFee() != null) {
            shipment.setShippingFee(request.getShippingFee());
        }

        if (request.getNotes() != null) {
            shipment.setNotes(request.getNotes().isBlank() ? null : request.getNotes().trim());
        }
    }

    private List<String> allowedOrderStatuses(Orders order) {
        OrderStatus current = OrderStatus.parse(order.getOrderStatus());
        Set<String> allowed = new LinkedHashSet<>();
        allowed.add(current.name());
        current.allowedNext().forEach(s -> allowed.add(s.name()));
        return new ArrayList<>(allowed);
    }

    private List<String> allowedPaymentStatuses(Orders order) {
        PaymentStatus current = PaymentStatus.parse(order.getPaymentStatus());
        OrderStatus orderStatus = OrderStatus.parse(order.getOrderStatus());
        List<String> allowed = new ArrayList<>();
        allowed.add(current.name());

        if (current == PaymentStatus.UNPAID && orderStatus == OrderStatus.DELIVERED) {
            allowed.add(PaymentStatus.PAID.name());
        }
        return allowed;
    }

    private List<String> allowedShippingStatuses() {
        List<String> list = new ArrayList<>();
        for (ShippingStatus status : ShippingStatus.values()) {
            list.add(status.name());
        }
        return list;
    }

    private Map<String, Object> toSummary(Orders order) {
        Map<String, Object> m = new HashMap<>();
        Accounts account = order.getAccount();
        m.put("id", order.getId());
        m.put("username", account != null ? account.getUsername() : "");
        m.put("createDate", order.getCreateDate());
        m.put("updateDate", order.getUpdateDate());
        m.put("address", formatShippingAddress(order));
        m.put("orderStatus", order.getOrderStatus());
        m.put("paymentStatus", order.getPaymentStatus());
        m.put("totalAmount", order.getTotalAmount());
        return m;
    }

    private Map<String, Object> toDetail(Orders order) {
        Map<String, Object> m = toSummary(order);
        m.put("subTotal", order.getSubTotal());
        m.put("discountAmount", order.getDiscountAmount());
        m.put("shippingAddress", toShippingAddressMap(order));
        m.put("customer", toCustomerMap(order));
        return m;
    }

    private Map<String, Object> toCustomerMap(Orders order) {
        Map<String, Object> m = new HashMap<>();
        Accounts account = order.getAccount();
        if (account == null) {
            return m;
        }
        m.put("username", account.getUsername());
        m.put("email", account.getEmail());
        usersRepository.findByAccount_Id(account.getId()).ifPresent(user -> {
            m.put("fullName", user.getFullName());
            m.put("totalPoint", user.getTotalPoint() != null ? user.getTotalPoint() : 0);
            if (user.getRank() != null) {
                m.put("rankId", user.getRank().getId());
                m.put("rankName", user.getRank().getRankName());
            }
        });
        return m;
    }

    private Map<String, Object> toShippingAddressMap(Orders order) {
        if (order.getOrderAddresses() == null || order.getOrderAddresses().isEmpty()) {
            return null;
        }
        OrderAddresses shipping = order.getOrderAddresses().iterator().next();
        Map<String, Object> m = new HashMap<>();
        m.put("receiverName", shipping.getReceiverName());
        m.put("receiverPhone", shipping.getReceiverPhone());
        m.put("province", shipping.getProvince());
        m.put("ward", shipping.getWard());
        m.put("addressDetail", shipping.getAddressDetail());
        return m;
    }

    private List<Map<String, Object>> toDetailItems(int orderId) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderDetails detail : orderDetailsService.findByOrder(orderId)) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", detail.getId());
            m.put("price", detail.getPrice());
            m.put("quantity", detail.getQuantity());
            double line = (detail.getPrice() != null ? detail.getPrice() : 0)
                    * (detail.getQuantity() != null ? detail.getQuantity() : 0);
            m.put("lineTotal", line);

            ProductVariants variant = detail.getVariant();
            if (variant != null) {
                m.put("sku", variant.getSku());
                m.put("color", variant.getColor());
                m.put("size", variant.getSize());
                Products product = variant.getProduct();
                m.put("productName", product != null ? product.getName() : "");
            } else {
                m.put("productName", "");
            }
            list.add(m);
        }
        return list;
    }

    private Map<String, Object> toShipmentMap(Shipments shipment) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", shipment.getId());
        m.put("trackingNumber", shipment.getTrackingNumber());
        m.put("shippingStatus", shipment.getShippingStatus());
        m.put("shippingFee", shipment.getShippingFee());
        m.put("notes", shipment.getNotes());
        if (shipment.getCarrier() != null) {
            m.put("carrierId", shipment.getCarrier().getId());
            m.put("carrierName", shipment.getCarrier().getName());
        }
        return m;
    }

    private String formatShippingAddress(Orders order) {
        Map<String, Object> addr = toShippingAddressMap(order);
        if (addr == null) {
            return null;
        }
        return String.format("%s, %s, %s",
                addr.get("addressDetail"),
                addr.get("ward"),
                addr.get("province"));
    }
}
