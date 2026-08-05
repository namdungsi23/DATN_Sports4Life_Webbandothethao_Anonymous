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

import poly.edu.ASSM.Entity.Carriers;
import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Shipments;
import poly.edu.ASSM.Repository.CarrierRepository;
import poly.edu.ASSM.Repository.OrdersRepository;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.Repository.ShipmentRepository;
import poly.edu.ASSM.domain.OrderStatus;
import poly.edu.ASSM.domain.PaymentStatus;
import poly.edu.ASSM.domain.ShippingStatus;
import poly.edu.ASSM.dto.request.AdminOrderUpdateRequest;
import poly.edu.ASSM.dto.response.CarrierResponse;
import poly.edu.ASSM.mapper.AdminOrderMapper;
import poly.edu.ASSM.mapper.CarrierMapper;

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
    private AdminOrderMapper adminOrderMapper;

    @Autowired
    private CarrierMapper carrierMapper;

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
        return source.stream().map(adminOrderMapper::toSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(int orderId) {
        Orders order = ordersService.findById(orderId);
        Map<String, Object> body = new HashMap<>();
        body.put("order", adminOrderMapper.toDetail(order));
        body.put("items", adminOrderMapper.toDetailItems(orderDetailsService.findByOrder(orderId)));
        body.put("allowedOrderStatuses", allowedOrderStatuses(order));
        body.put("allowedPaymentStatuses", allowedPaymentStatuses(order));
        OrderStatus orderStatus = OrderStatus.parse(order.getOrderStatus());
        if (orderStatus == OrderStatus.SHIPPING || orderStatus == OrderStatus.DELIVERED) {
            body.put("allowedShippingStatuses", allowedShippingStatuses());
        }
        shipmentRepository.findByOrder_Id(orderId).ifPresent(shipment ->
                body.put("shipment", adminOrderMapper.toShipmentMap(shipment)));
        body.put("carriers", listCarriers());
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPendingAlerts() {
        List<Orders> pending = ordersService.findByOrderStatus(OrderStatus.PENDING.name());
        List<Map<String, Object>> recent = pending.stream()
                .limit(5)
                .map(adminOrderMapper::toSummary)
                .collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("pendingCount", pending.size());
        body.put("recentPending", recent);
        return body;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarrierResponse> listCarriers() {
        return carrierMapper.toResponseList(carrierRepository.findByActiveTrueOrderByNameAsc());
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
}
