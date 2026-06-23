package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Entity.PaymentMethods;
import poly.edu.ASSM.Entity.Payments;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.PaymentRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.PaymentResponse;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payments entity) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUsers();
        Orders order = entity.getOrder();
        PaymentMethods method = entity.getPaymentMethods();

        return PaymentResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .orderId(order != null ? order.getId() : null)
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .paidAt(entity.getPaidAt())
                .createdAt(entity.getCreatedAt())
                .paymentMethodId(method != null ? method.getId() : null)
                .paymentMethodCode(method != null ? method.getCode() : null)
                .paymentMethodName(method != null ? method.getName() : null)
                .build();
    }

    public List<PaymentResponse> toResponseList(Collection<Payments> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Payments toEntity(PaymentRequest request, Users user, Orders order, PaymentMethods method) {
        Payments entity = new Payments();
        applyRequest(entity, request, user, order, method);
        return entity;
    }

    public void applyRequest(Payments entity, PaymentRequest request, Users user, Orders order, PaymentMethods method) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsers(user);
        entity.setOrder(order);
        entity.setPaymentMethods(method);
        entity.setAmount(request.getAmount());
        entity.setStatus(request.getStatus());
        entity.setPaidAt(request.getPaidAt());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<PaymentResponse> toPageResponse(Page<Payments> page) {
        return PageResponse.<PaymentResponse>builder()
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
