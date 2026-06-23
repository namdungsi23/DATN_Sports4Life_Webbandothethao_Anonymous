package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.UserAddress;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.UserAddressRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.UserAddressResponse;

@Component
public class UserAddressMapper {

    public UserAddressResponse toResponse(UserAddress entity) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUsers();
        return UserAddressResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .receiverName(entity.getReceiverName())
                .receiverPhone(entity.getReceiverPhone())
                .province(entity.getProvince())
                .ward(entity.getWard())
                .addressDetail(entity.getAddressDetail())
                .isDefault(entity.getIsDefault())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<UserAddressResponse> toResponseList(Collection<UserAddress> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public UserAddress toEntity(UserAddressRequest request, Users user) {
        UserAddress entity = new UserAddress();
        applyRequest(entity, request, user);
        return entity;
    }

    public void applyRequest(UserAddress entity, UserAddressRequest request, Users user) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsers(user);
        entity.setReceiverName(request.getReceiverName());
        entity.setReceiverPhone(request.getReceiverPhone());
        entity.setProvince(request.getProvince());
        entity.setWard(request.getWard());
        entity.setAddressDetail(request.getAddressDetail());
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : Boolean.FALSE);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<UserAddressResponse> toPageResponse(Page<UserAddress> page) {
        return PageResponse.<UserAddressResponse>builder()
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
