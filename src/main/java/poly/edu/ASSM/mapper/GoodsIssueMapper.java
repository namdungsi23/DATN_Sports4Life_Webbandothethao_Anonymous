package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.GoodsIssue;
import poly.edu.ASSM.dto.request.GoodsIssueRequest;
import poly.edu.ASSM.dto.response.GoodsIssueResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class GoodsIssueMapper {

    public GoodsIssueResponse toResponse(GoodsIssue entity) {
        if (entity == null) {
            return null;
        }
        return GoodsIssueResponse.builder()
                .id(entity.getId())
                .issueCode(entity.getIssueCode())
                .userId(entity.getUserId())
                .note(entity.getNote())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<GoodsIssueResponse> toResponseList(Collection<GoodsIssue> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public GoodsIssue toEntity(GoodsIssueRequest request) {
        GoodsIssue entity = new GoodsIssue();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(GoodsIssue entity, GoodsIssueRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setIssueCode(request.getIssueCode());
        entity.setUserId(request.getUserId());
        entity.setNote(request.getNote());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<GoodsIssueResponse> toPageResponse(Page<GoodsIssue> page) {
        return PageResponse.<GoodsIssueResponse>builder()
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
