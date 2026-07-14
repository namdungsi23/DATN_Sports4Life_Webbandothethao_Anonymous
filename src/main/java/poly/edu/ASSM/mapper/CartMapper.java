package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.Carts;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.dto.request.CartRequest;
import poly.edu.ASSM.dto.response.CartResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class CartMapper {

    private final CartItemMapper cartItemMapper;

    public CartMapper(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    public CartResponse toResponse(Carts entity) {
        if (entity == null) {
            return null;
        }
        Users user = entity.getUsers();
        return CartResponse.builder()
                .id(entity.getId())
                .userId(user != null ? user.getId() : null)
                .createdAt(entity.getCreatedAt())
                .items(cartItemMapper.toResponseList(entity.getCartItems()))
                .build();
    }

    public List<CartResponse> toResponseList(Collection<Carts> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Carts toEntity(CartRequest request, Users user) {
        Carts entity = new Carts();
        applyRequest(entity, request, user);
        return entity;
    }

    public void applyRequest(Carts entity, CartRequest request, Users user) {
        if (entity == null || request == null) {
            return;
        }
        entity.setUsers(user);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<CartResponse> toPageResponse(Page<Carts> page) {
        return PageResponse.<CartResponse>builder()
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
