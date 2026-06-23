package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.Voucher;
import poly.edu.ASSM.dto.request.OrderRequest;
import poly.edu.ASSM.dto.response.OrderResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class OrderMapper {

    private final OrderDetailMapper orderDetailMapper;

    public OrderMapper(OrderDetailMapper orderDetailMapper) {
        this.orderDetailMapper = orderDetailMapper;
    }

    public OrderResponse toResponse(Orders entity) {
        if (entity == null) {
            return null;
        }
        Voucher voucher = entity.getVoucher();
        return OrderResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .createDate(entity.getCreateDate())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .totalAmount(entity.getTotalAmount())
                .voucherId(voucher != null ? voucher.getId() : null)
                .voucherCode(voucher != null ? voucher.getCode() : null)
                .details(orderDetailMapper.toResponseList(entity.getOrderDetails()))
                .build();
    }

    public List<OrderResponse> toResponseList(Collection<Orders> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Orders toEntity(OrderRequest request, Voucher voucher) {
        Orders entity = new Orders();
        applyRequest(entity, request, voucher);
        return entity;
    }

    public void applyRequest(Orders entity, OrderRequest request, Voucher voucher) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsername(request.getUsername());
        entity.setCreateDate(request.getCreateDate());
        entity.setAddress(request.getAddress());
        entity.setStatus(request.getStatus());
        entity.setTotalAmount(request.getTotalAmount());
        entity.setVoucher(voucher);
    }

    public PageResponse<OrderResponse> toPageResponse(Page<Orders> page) {
        return PageResponse.<OrderResponse>builder()
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
