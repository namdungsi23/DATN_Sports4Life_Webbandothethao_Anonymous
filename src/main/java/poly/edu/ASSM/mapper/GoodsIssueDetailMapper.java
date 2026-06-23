package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.GoodsIssueDetail;
import poly.edu.ASSM.dto.request.GoodsIssueDetailRequest;
import poly.edu.ASSM.dto.response.GoodsIssueDetailResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class GoodsIssueDetailMapper {

    public GoodsIssueDetailResponse toResponse(GoodsIssueDetail entity) {
        if (entity == null) {
            return null;
        }
        return GoodsIssueDetailResponse.builder()
                .id(entity.getId())
                .quantity(entity.getQuantity())
                .build();
    }

    public List<GoodsIssueDetailResponse> toResponseList(Collection<GoodsIssueDetail> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public GoodsIssueDetail toEntity(GoodsIssueDetailRequest request) {
        GoodsIssueDetail entity = new GoodsIssueDetail();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(GoodsIssueDetail entity, GoodsIssueDetailRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setQuantity(request.getQuantity());
    }

    public PageResponse<GoodsIssueDetailResponse> toPageResponse(Page<GoodsIssueDetail> page) {
        return PageResponse.<GoodsIssueDetailResponse>builder()
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
