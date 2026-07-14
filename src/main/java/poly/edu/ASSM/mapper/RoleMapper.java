package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.Roles;
import poly.edu.ASSM.dto.request.RoleRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.RoleResponse;

@Component
public class RoleMapper {

    public RoleResponse toResponse(Roles entity) {
        if (entity == null) {
            return null;
        }
        return RoleResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .build();
    }

    public List<RoleResponse> toResponseList(Collection<Roles> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Roles toEntity(RoleRequest request) {
        Roles entity = new Roles();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(Roles entity, RoleRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
    }

    public PageResponse<RoleResponse> toPageResponse(Page<Roles> page) {
        return PageResponse.<RoleResponse>builder()
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
