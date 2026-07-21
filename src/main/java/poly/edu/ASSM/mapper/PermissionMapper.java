package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Permissions;
import poly.edu.ASSM.dto.request.PermissionRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.PermissionResponse;

@Component
public class PermissionMapper {

    public PermissionResponse toResponse(Permissions entity) {
        if (entity == null) {
            return null;
        }
        return PermissionResponse.builder()
                .id(entity.getId())
                .permissionName(entity.getPermissionName())
                .description(entity.getDescription())
                .build();
    }

    public List<PermissionResponse> toResponseList(Collection<Permissions> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Permissions toEntity(PermissionRequest request) {
        Permissions entity = new Permissions();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(Permissions entity, PermissionRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPermissionName(request.getPermissionName());
        entity.setDescription(request.getDescription());
    }

    public PageResponse<PermissionResponse> toPageResponse(Page<Permissions> page) {
        return PageResponse.<PermissionResponse>builder()
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
