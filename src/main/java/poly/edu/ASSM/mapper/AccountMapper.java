package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.AccountRequest;
import poly.edu.ASSM.dto.response.AccountResponse;
import poly.edu.ASSM.dto.response.AdminUserResponse;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.security.SpringRoleNames;

/**
 * Entity → DTO. Không inject Repository — profile/roleNames do Service truyền vào.
 */
@Component
public class AccountMapper {

    public AccountResponse toResponse(Accounts entity, Users profile) {
        if (entity == null) {
            return null;
        }

        Roles role = entity.getRole();
        String roleName = role != null ? SpringRoleNames.normalize(role.getName()) : "";
        boolean admin = roleName.contains("ADMIN");

        return AccountResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(profile != null ? profile.getFullName() : null)
                .email(entity.getEmail())
                .avatar(profile != null ? profile.getAvatar() : null)
                .isActive(entity.getIsActive())
                .admin(admin)
                .superAdmin(false)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AccountResponse toResponse(Accounts entity) {
        return toResponse(entity, null);
    }

    /** Map cho màn admin user (fullname/photo/activated/roleNames/rank…). */
    public AdminUserResponse toAdminUserResponse(Accounts account, Users profile, List<String> roleNames) {
        if (account == null) {
            return null;
        }

        String roleName = account.getRole() != null
                ? SpringRoleNames.normalize(account.getRole().getName())
                : "";
        String avatar = profile != null ? profile.getAvatar() : null;

        return AdminUserResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .fullname(profile != null ? profile.getFullName() : null)
                .email(account.getEmail())
                .photo(avatar)
                .avatar(avatar)
                .activated(account.getIsActive())
                .admin(roleName.contains("ADMIN"))
                .roleNames(roleNames != null ? List.copyOf(roleNames) : List.of())
                .totalPoint(profile != null && profile.getTotalPoint() != null ? profile.getTotalPoint() : 0)
                .rankId(profile != null && profile.getRank() != null ? profile.getRank().getId() : null)
                .rankName(profile != null && profile.getRank() != null ? profile.getRank().getRankName() : null)
                .build();
    }

    public List<AccountResponse> toResponseList(Collection<Accounts> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Accounts toEntity(AccountRequest request) {
        Accounts entity = new Accounts();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(Accounts entity, AccountRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setIsActive(request.getIsActive());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            entity.setPasswordHash(request.getPassword());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
    }

    public PageResponse<AccountResponse> toPageResponse(Page<Accounts> page) {
        return PageResponse.<AccountResponse>builder()
                .content(toResponseList(page.getContent()))
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .number(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public PageResponse<AdminUserResponse> toAdminUserPageResponse(
            Page<Accounts> page,
            java.util.function.Function<Accounts, Users> profileLoader,
            java.util.function.Function<Accounts, List<String>> roleNamesLoader) {
        List<AdminUserResponse> content = page.getContent().stream()
                .map(acc -> toAdminUserResponse(
                        acc,
                        profileLoader.apply(acc),
                        roleNamesLoader.apply(acc)))
                .collect(Collectors.toList());
        return PageResponse.<AdminUserResponse>builder()
                .content(content)
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .number(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
