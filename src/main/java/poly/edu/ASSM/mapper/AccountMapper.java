package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.dto.request.AccountRequest;
import poly.edu.ASSM.dto.response.AccountResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class AccountMapper {

    public AccountResponse toResponse(Accounts entity) {
        if (entity == null) {
            return null;
        }
        return AccountResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .avatar(entity.getAvatar())
                .isActive(entity.getIsActive())
                .admin(entity.getAdmin())
                .superAdmin(entity.getSuperAdmin())
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
        entity.setFullName(request.getFullName());
        entity.setEmail(request.getEmail());
        entity.setAvatar(request.getAvatar());
        entity.setIsActive(request.getIsActive());
        entity.setAdmin(request.getAdmin());
        entity.setSuperAdmin(request.getSuperAdmin());

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
