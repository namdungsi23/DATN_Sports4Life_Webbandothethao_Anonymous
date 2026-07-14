package poly.edu.ASSM.mapper;

import java.time.Instant;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.OrderAddresses;
import poly.edu.ASSM.entity.Orders;
import poly.edu.ASSM.dto.response.OrderAddressResponse;

@Component
public class OrderAddressMapper {

    public OrderAddressResponse toResponse(OrderAddresses entity) {
        if (entity == null) {
            return null;
        }
        Orders order = entity.getOrder();
        return OrderAddressResponse.builder()
                .id(entity.getId())
                .orderId(order != null ? order.getId() : null)
                .receiverName(entity.getReceiverName())
                .receiverPhone(entity.getReceiverPhone())
                .province(entity.getProvince())
                .ward(entity.getWard())
                .addressDetail(entity.getAddressDetail())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public OrderAddresses createSnapshot(Orders order, String receiverName, String receiverPhone,
            String province, String ward, String addressDetail) {
        OrderAddresses snapshot = new OrderAddresses();
        snapshot.setOrder(order);
        snapshot.setReceiverName(receiverName.trim());
        snapshot.setReceiverPhone(receiverPhone.trim());
        snapshot.setProvince(province.trim());
        snapshot.setWard(ward.trim());
        snapshot.setAddressDetail(addressDetail.trim());
        snapshot.setCreatedAt(Instant.now());
        return snapshot;
    }
}
