package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Reviews;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.ReviewRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.ReviewResponse;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Reviews entity) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUsers();
        Products product = entity.getProduct();

        return ReviewResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .username(user != null && user.getAccount() != null ? user.getAccount().getUsername() : null)
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .rating(entity.getRating())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ReviewResponse toResponse(Reviews entity, Products product) {
        ReviewResponse response = toResponse(entity);
        if (response != null && product != null) {
            response.setProductId(product.getId());
            response.setProductName(product.getName());
        }
        return response;
    }

    public List<ReviewResponse> toResponseList(Collection<Reviews> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Reviews toEntity(ReviewRequest request, Users user, Products product) {
        Reviews entity = new Reviews();
        applyRequest(entity, request, user, product);
        return entity;
    }

    public void applyRequest(Reviews entity, ReviewRequest request, Users user, Products product) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsers(user);
        entity.setProduct(product);
        entity.setRating(request.getRating());
        entity.setComment(request.getComment());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<ReviewResponse> toPageResponse(Page<Reviews> page) {
        return PageResponse.<ReviewResponse>builder()
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
