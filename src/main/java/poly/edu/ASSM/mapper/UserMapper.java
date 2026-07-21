package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Ranks;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.UserRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.UserResponse;

@Component
public class UserMapper {

    public UserResponse toResponse(Users entity) {
        if (entity == null) {
            return null;
        }

        Accounts account = entity.getAccount();
        Ranks rank = entity.getRank();

        return UserResponse.builder()
                .id(entity.getId())
                .accountId(account != null ? account.getId() : null)
                .username(account != null ? account.getUsername() : null)
                .fullName(entity.getFullName())
                .email(account != null ? account.getEmail() : null)
                .phone(entity.getPhone())
                .dateOfBirth(entity.getDateOfBirth())
                .totalPoint(entity.getTotalPoint())
                .totalSpending(entity.getTotalSpending())
                .rankId(rank != null ? rank.getId() : null)
                .rankName(rank != null ? rank.getRankName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public List<UserResponse> toResponseList(Collection<Users> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Users toEntity(UserRequest request, Accounts account, Ranks rank) {
        Users entity = new Users();
        applyRequest(entity, request, account, rank);
        return entity;
    }

    public void applyRequest(Users entity, UserRequest request, Accounts account, Ranks rank) {
        if (entity == null || request == null) {
            return;
        }
        entity.setAccount(account);
        entity.setPhone(request.getPhone());
        entity.setDateOfBirth(request.getDateOfBirth());
        if (request.getTotalPoint() != null) {
            entity.setTotalPoint(request.getTotalPoint());
        }
        entity.setRank(rank);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
    }

    public PageResponse<UserResponse> toPageResponse(Page<Users> page) {
        return PageResponse.<UserResponse>builder()
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
