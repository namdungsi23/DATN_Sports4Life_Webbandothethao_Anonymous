package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.dto.request.AccountRequest;
import poly.edu.ASSM.dto.response.AccountResponse;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.security.SpringRoleNames;

@Component
public class AccountMapper {

    @Autowired
    private UsersRepository usersRepository;

    public AccountResponse toResponse(Accounts entity) {
        if (entity == null) {
            return null;
        }

        Users profile = usersRepository.findByAccount_Id(entity.getId()).orElse(null);
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
}
