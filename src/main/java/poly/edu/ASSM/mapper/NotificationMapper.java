package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Notification;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.NotificationRequest;
import poly.edu.ASSM.dto.response.NotificationResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification entity) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUsers();
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .title(entity.getTitle())
                .message(entity.getMessage())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<NotificationResponse> toResponseList(Collection<Notification> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Notification toEntity(NotificationRequest request, Users user) {
        Notification entity = new Notification();
        applyRequest(entity, request, user);
        return entity;
    }

    public void applyRequest(Notification entity, NotificationRequest request, Users user) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsers(user);
        entity.setTitle(request.getTitle());
        entity.setMessage(request.getMessage());
        entity.setIsRead(request.getIsRead() != null ? request.getIsRead() : Boolean.FALSE);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<NotificationResponse> toPageResponse(Page<Notification> page) {
        return PageResponse.<NotificationResponse>builder()
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
