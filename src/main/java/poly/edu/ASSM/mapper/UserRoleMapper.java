package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.Roles;
import poly.edu.ASSM.entity.UserRole;
import poly.edu.ASSM.entity.UserRoleId;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.dto.request.UserRoleRequest;
import poly.edu.ASSM.dto.response.UserRoleResponse;

@Component
public class UserRoleMapper {

    public UserRoleResponse toResponse(UserRole entity) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUsers();
        Roles role = entity.getRoles();
        UserRoleId embeddedId = entity.getId();

        return UserRoleResponse.builder()
                .userId(user != null ? user.getId() : embeddedId != null ? embeddedId.getUserId() : null)
                .username(user != null && user.getAccount() != null ? user.getAccount().getUsername() : null)
                .roleId(role != null ? role.getId() : embeddedId != null ? embeddedId.getRoleId() : null)
                .roleName(role != null ? role.getName() : null)
                .build();
    }

    public List<UserRoleResponse> toResponseList(Collection<UserRole> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserRole toEntity(UserRoleRequest request, Users user, Roles role) {
        UserRole entity = new UserRole();
        applyRequest(entity, request, user, role);
        return entity;
    }

    public void applyRequest(UserRole entity, UserRoleRequest request, Users user, Roles role) {
        if (entity == null || request == null) {
            return;
        }
        UserRoleId id = new UserRoleId();
        id.setUserId(request.getUserId());
        id.setRoleId(request.getRoleId());
        entity.setId(id);
        entity.setUsers(user);
        entity.setRoles(role);
    }
}
