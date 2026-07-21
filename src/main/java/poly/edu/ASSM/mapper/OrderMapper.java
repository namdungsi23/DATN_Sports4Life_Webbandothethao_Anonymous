package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.OrderAddresses;
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
        Accounts account = entity.getAccount();
        return OrderResponse.builder()
                .id(entity.getId())
                .username(account != null ? account.getUsername() : null)
                .createDate(toLocalDate(entity.getCreateDate()))
                .address(formatShippingAddress(entity))
                .status(entity.getOrderStatus())
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

    public Orders toEntity(OrderRequest request, Voucher voucher, Accounts account) {
        Orders entity = new Orders();
        applyRequest(entity, request, voucher, account);
        return entity;
    }

    public void applyRequest(Orders entity, OrderRequest request, Voucher voucher, Accounts account) {
        if (entity == null || request == null) {
            return;
        }
        entity.setAccount(account);
        if (request.getCreateDate() != null) {
            entity.setCreateDate(request.getCreateDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        if (request.getStatus() != null) {
            entity.setOrderStatus(request.getStatus());
        }
        if (request.getTotalAmount() != null) {
            entity.setTotalAmount(request.getTotalAmount());
            if (entity.getSubTotal() == null) {
                entity.setSubTotal(request.getTotalAmount());
            }
        }
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

    private LocalDate toLocalDate(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private String formatShippingAddress(Orders entity) {
        if (entity.getOrderAddresses() == null || entity.getOrderAddresses().isEmpty()) {
            return null;
        }
        OrderAddresses shipping = entity.getOrderAddresses().iterator().next();
        return String.format("%s, %s, %s",
                shipping.getAddressDetail(),
                shipping.getWard(),
                shipping.getProvince());
    }
}
