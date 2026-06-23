package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Voucher;
import poly.edu.ASSM.dto.request.VoucherRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.VoucherResponse;

@Component
public class VoucherMapper {

    public VoucherResponse toResponse(Voucher entity) {
        if (entity == null) {
            return null;
        }
        return VoucherResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .discountPercent(entity.getDiscountPercent())
                .discountAmount(entity.getDiscountAmount())
                .minOrderValue(entity.getMinOrderValue())
                .maxDiscount(entity.getMaxDiscount())
                .quantity(entity.getQuantity())
                .usedCount(entity.getUsedCount())
                .startDate(entity.getStartDate())
                .expiredAt(entity.getExpiredAt())
                .isActive(entity.getIsActive())
                .build();
    }

    public List<VoucherResponse> toResponseList(Collection<Voucher> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Voucher toEntity(VoucherRequest request) {
        Voucher entity = new Voucher();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(Voucher entity, VoucherRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setDiscountPercent(request.getDiscountPercent());
        entity.setDiscountAmount(request.getDiscountAmount());
        entity.setMinOrderValue(request.getMinOrderValue());
        entity.setMaxDiscount(request.getMaxDiscount());
        entity.setQuantity(request.getQuantity());
        entity.setStartDate(request.getStartDate());
        entity.setExpiredAt(request.getExpiredAt());
        entity.setIsActive(request.getIsActive());
    }

    public PageResponse<VoucherResponse> toPageResponse(Page<Voucher> page) {
        return PageResponse.<VoucherResponse>builder()
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
