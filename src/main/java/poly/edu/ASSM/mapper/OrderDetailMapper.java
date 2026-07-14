package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.OrderDetails;
import poly.edu.ASSM.entity.Orders;
import poly.edu.ASSM.dto.request.OrderDetailRequest;
import poly.edu.ASSM.dto.response.OrderDetailResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class OrderDetailMapper {

    public OrderDetailResponse toResponse(OrderDetails entity) {
        if (entity == null) {
            return null;
        }
        Orders order = entity.getOrders();
        return OrderDetailResponse.builder()
                .id(entity.getId())
                .orderId(order != null ? order.getId() : null)
                .price(entity.getPrice())
                .quantity(entity.getQuantity())
                .build();
    }

    public List<OrderDetailResponse> toResponseList(Collection<OrderDetails> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public OrderDetails toEntity(OrderDetailRequest request, Orders order) {
        OrderDetails entity = new OrderDetails();
        applyRequest(entity, request, order);
        return entity;
    }

    public void applyRequest(OrderDetails entity, OrderDetailRequest request, Orders order) {
        if (entity == null || request == null) {
            return;
        }
        entity.setOrders(order);
        entity.setPrice(request.getPrice());
        entity.setQuantity(request.getQuantity());
    }

    public PageResponse<OrderDetailResponse> toPageResponse(Page<OrderDetails> page) {
        return PageResponse.<OrderDetailResponse>builder()
                .content(toResponseList(page.getContent()))
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .number(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
