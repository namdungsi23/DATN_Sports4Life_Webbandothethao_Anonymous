package poly.edu.ASSM.mapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.OrderAddresses;
import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Shipments;
import poly.edu.ASSM.domain.OrderStatus;
import poly.edu.ASSM.dto.response.CustomerOrderDetailResponse;
import poly.edu.ASSM.dto.response.CustomerOrderItemResponse;
import poly.edu.ASSM.dto.response.CustomerOrderSummaryResponse;
import poly.edu.ASSM.dto.response.OrderAddressResponse;
import poly.edu.ASSM.dto.response.ShipmentResponse;

@Component
public class CustomerOrderMapper {

    private final OrderAddressMapper orderAddressMapper;

    public CustomerOrderMapper(OrderAddressMapper orderAddressMapper) {
        this.orderAddressMapper = orderAddressMapper;
    }

    public CustomerOrderSummaryResponse toSummary(Orders order, OrderAddresses address) {
        if (order == null) {
            return null;
        }
        return CustomerOrderSummaryResponse.builder()
                .id(order.getId())
                .createDate(order.getCreateDate())
                .updateDate(order.getUpdateDate())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .itemCount(order.getOrderDetails() != null ? order.getOrderDetails().size() : 0)
                .canCancel(canCustomerCancel(order.getOrderStatus()))
                .address(address != null ? formatAddress(address) : null)
                .build();
    }

    public CustomerOrderDetailResponse toDetail(Orders order, OrderAddresses address, Shipments shipment) {
        if (order == null) {
            return null;
        }
        return CustomerOrderDetailResponse.builder()
                .id(order.getId())
                .createDate(order.getCreateDate())
                .orderStatus(order.getOrderStatus())
                .paymentStatus(order.getPaymentStatus())
                .subTotal(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .canCancel(canCustomerCancel(order.getOrderStatus()))
                .items(toItems(order))
                .shippingAddress(orderAddressMapper.toResponse(address))
                .shipment(toShipment(shipment))
                .build();
    }

    public List<CustomerOrderSummaryResponse> toSummaryList(
            Collection<Orders> orders,
            java.util.function.Function<Integer, OrderAddresses> addressLoader) {
        if (orders == null) {
            return List.of();
        }
        return orders.stream()
                .map(o -> toSummary(o, addressLoader.apply(o.getId())))
                .collect(Collectors.toList());
    }

    public List<CustomerOrderItemResponse> toItems(Orders order) {
        List<CustomerOrderItemResponse> items = new ArrayList<>();
        if (order == null || order.getOrderDetails() == null) {
            return items;
        }
        for (OrderDetails detail : order.getOrderDetails()) {
            CustomerOrderItemResponse.CustomerOrderItemResponseBuilder b = CustomerOrderItemResponse.builder()
                    .id(detail.getId())
                    .quantity(detail.getQuantity())
                    .price(detail.getPrice());
            double line = (detail.getPrice() != null ? detail.getPrice() : 0)
                    * (detail.getQuantity() != null ? detail.getQuantity() : 0);
            b.lineTotal(line);

            ProductVariants variant = detail.getVariant();
            if (variant != null) {
                b.variantId(variant.getId())
                        .size(variant.getSize())
                        .color(variant.getColor())
                        .sku(variant.getSku());
                Products product = variant.getProduct();
                if (product != null) {
                    b.productId(product.getId()).productName(product.getName());
                }
            }
            items.add(b.build());
        }
        return items;
    }

    public ShipmentResponse toShipment(Shipments shipment) {
        if (shipment == null) {
            return null;
        }
        ShipmentResponse.ShipmentResponseBuilder b = ShipmentResponse.builder()
                .shippingStatus(shipment.getShippingStatus())
                .trackingNumber(shipment.getTrackingNumber())
                .shippingFee(shipment.getShippingFee())
                .notes(shipment.getNotes());
        if (shipment.getCarrier() != null) {
            b.carrierName(shipment.getCarrier().getName())
                    .carrierCode(shipment.getCarrier().getCode());
        }
        return b.build();
    }

    public static boolean canCustomerCancel(String orderStatus) {
        try {
            return OrderStatus.parse(orderStatus).canTransitionTo(OrderStatus.CANCELLED);
        } catch (Exception e) {
            return false;
        }
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
