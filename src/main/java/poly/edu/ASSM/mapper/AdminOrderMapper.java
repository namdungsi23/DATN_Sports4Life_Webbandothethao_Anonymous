package poly.edu.ASSM.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.OrderAddresses;
import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Shipments;
import poly.edu.ASSM.Repository.UsersRepository;

/**
 * Map đơn hàng admin → payload FE (giữ key Map hiện tại).
 */
@Component
public class AdminOrderMapper {

    @Autowired
    private UsersRepository usersRepository;

    public Map<String, Object> toSummary(Orders order) {
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

    public Map<String, Object> toDetail(Orders order) {
        Map<String, Object> m = toSummary(order);
        m.put("subTotal", order.getSubTotal());
        m.put("discountAmount", order.getDiscountAmount());
        m.put("shippingAddress", toShippingAddressMap(order));
        m.put("customer", toCustomerMap(order));
        return m;
    }

    public Map<String, Object> toCustomerMap(Orders order) {
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

    public Map<String, Object> toShippingAddressMap(Orders order) {
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

    public List<Map<String, Object>> toDetailItems(List<OrderDetails> details) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OrderDetails detail : details) {
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

    public Map<String, Object> toShipmentMap(Shipments shipment) {
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

    public String formatShippingAddress(Orders order) {
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
