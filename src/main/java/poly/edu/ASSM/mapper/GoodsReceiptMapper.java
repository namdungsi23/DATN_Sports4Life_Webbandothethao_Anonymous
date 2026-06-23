package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.GoodsReceipt;
import poly.edu.ASSM.dto.request.GoodsReceiptRequest;
import poly.edu.ASSM.dto.response.GoodsReceiptResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class GoodsReceiptMapper {

    public GoodsReceiptResponse toResponse(GoodsReceipt entity) {
        if (entity == null) {
            return null;
        }
        return GoodsReceiptResponse.builder()
                .id(entity.getId())
                .receiptCode(entity.getReceiptCode())
                .userId(entity.getUserId())
                .note(entity.getNote())
                .totalAmount(entity.getTotalAmount())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<GoodsReceiptResponse> toResponseList(Collection<GoodsReceipt> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public GoodsReceipt toEntity(GoodsReceiptRequest request) {
        GoodsReceipt entity = new GoodsReceipt();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(GoodsReceipt entity, GoodsReceiptRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setReceiptCode(request.getReceiptCode());
        entity.setUserId(request.getUserId());
        entity.setNote(request.getNote());
        entity.setTotalAmount(request.getTotalAmount());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<GoodsReceiptResponse> toPageResponse(Page<GoodsReceipt> page) {
        return PageResponse.<GoodsReceiptResponse>builder()
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
