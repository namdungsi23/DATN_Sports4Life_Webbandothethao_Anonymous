package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.PaymentMethods;
import poly.edu.ASSM.dto.request.PaymentMethodRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.PaymentMethodResponse;

@Component
public class PaymentMethodMapper {

    public PaymentMethodResponse toResponse(PaymentMethods entity) {
        if (entity == null) {
            return null;
        }
        return PaymentMethodResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<PaymentMethodResponse> toResponseList(Collection<PaymentMethods> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PaymentMethods toEntity(PaymentMethodRequest request) {
        PaymentMethods entity = new PaymentMethods();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(PaymentMethods entity, PaymentMethodRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<PaymentMethodResponse> toPageResponse(Page<PaymentMethods> page) {
        return PageResponse.<PaymentMethodResponse>builder()
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
