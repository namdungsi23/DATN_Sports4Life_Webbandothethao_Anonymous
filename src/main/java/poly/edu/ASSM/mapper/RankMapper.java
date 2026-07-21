package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Ranks;
import poly.edu.ASSM.dto.request.RankRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.RankResponse;

@Component
public class RankMapper {

    public RankResponse toResponse(Ranks entity) {
        if (entity == null) {
            return null;
        }
        return RankResponse.builder()
                .id(entity.getId())
                .rankName(entity.getRankName())
                .minPoint(entity.getMinPoint())
                .discountPercent(entity.getDiscountPercent())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .build();
    }

    public List<RankResponse> toResponseList(Collection<Ranks> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Ranks toEntity(RankRequest request) {
        Ranks entity = new Ranks();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(Ranks entity, RankRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setRankName(request.getRankName());
        entity.setMinPoint(request.getMinPoint());
        entity.setDiscountPercent(request.getDiscountPercent());
        entity.setDescription(request.getDescription());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
    }

    public PageResponse<RankResponse> toPageResponse(Page<Ranks> page) {
        return PageResponse.<RankResponse>builder()
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
