package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.Permissions;
import poly.edu.ASSM.entity.RolePermissionId;
import poly.edu.ASSM.entity.RolePermissions;
import poly.edu.ASSM.entity.Roles;
import poly.edu.ASSM.dto.request.RolePermissionRequest;
import poly.edu.ASSM.dto.response.RolePermissionResponse;

@Component
public class RolePermissionMapper {

    public RolePermissionResponse toResponse(RolePermissions entity) {
        if (entity == null) {
            return null;
        }
        Roles role = entity.getRole();
        RolePermissionId embeddedId = entity.getId();

        return RolePermissionResponse.builder()
                .roleId(role != null ? role.getId() : embeddedId != null ? embeddedId.getRoleId() : null)
                .roleName(role != null ? role.getName() : null)
                .permissionId(embeddedId != null ? embeddedId.getPermissionId() : null)
                .build();
    }

    public RolePermissionResponse toResponse(RolePermissions entity, Permissions permission) {
        RolePermissionResponse response = toResponse(entity);
        if (response != null && permission != null) {
            response.setPermissionId(permission.getId());
            response.setPermissionName(permission.getPermissionName());
        }
        return response;
    }

    public List<RolePermissionResponse> toResponseList(Collection<RolePermissions> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public RolePermissions toEntity(RolePermissionRequest request, Roles role) {
        RolePermissions entity = new RolePermissions();
        applyRequest(entity, request, role);
        return entity;
    }

    public void applyRequest(RolePermissions entity, RolePermissionRequest request, Roles role) {
        if (entity == null || request == null) {
            return;
        }
        RolePermissionId id = new RolePermissionId();
        id.setRoleId(request.getRoleId());
        id.setPermissionId(request.getPermissionId());
        entity.setId(id);
        entity.setRole(role);
    }
}
