package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.GoodsReceiptDetail;
import poly.edu.ASSM.dto.request.GoodsReceiptDetailRequest;
import poly.edu.ASSM.dto.response.GoodsReceiptDetailResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class GoodsReceiptDetailMapper {

    public GoodsReceiptDetailResponse toResponse(GoodsReceiptDetail entity) {
        if (entity == null) {
            return null;
        }
        return GoodsReceiptDetailResponse.builder()
                .id(entity.getId())
                .quantity(entity.getQuantity())
                .importPrice(entity.getImportPrice())
                .build();
    }

    public List<GoodsReceiptDetailResponse> toResponseList(Collection<GoodsReceiptDetail> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public GoodsReceiptDetail toEntity(GoodsReceiptDetailRequest request) {
        GoodsReceiptDetail entity = new GoodsReceiptDetail();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(GoodsReceiptDetail entity, GoodsReceiptDetailRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setQuantity(request.getQuantity());
        entity.setImportPrice(request.getImportPrice());
    }

    public PageResponse<GoodsReceiptDetailResponse> toPageResponse(Page<GoodsReceiptDetail> page) {
        return PageResponse.<GoodsReceiptDetailResponse>builder()
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
