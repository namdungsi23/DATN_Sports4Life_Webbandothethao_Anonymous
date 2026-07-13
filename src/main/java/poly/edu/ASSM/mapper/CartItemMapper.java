package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.CartItems;
import poly.edu.ASSM.Entity.Carts;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.dto.request.CartItemRequest;
import poly.edu.ASSM.dto.response.CartItemResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class CartItemMapper {

    public CartItemResponse toResponse(CartItems entity) {
        if (entity == null) {
            return null;
        }
        Products product = entity.getProduct();
        return CartItemResponse.builder()
                .id(entity.getId())
                .productId(product != null ? product.getId() : null)
                .productName(product != null ? product.getName() : null)
                .quantity(entity.getQuantity())
                .build();
    }

    public List<CartItemResponse> toResponseList(Collection<CartItems> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public CartItems toEntity(CartItemRequest request, Products product) {
        CartItems entity = new CartItems();
        applyRequest(entity, request, product);
        return entity;
    }

    public void applyRequest(CartItems entity, CartItemRequest request, Products product) {
        if (entity == null || request == null) {
            return;
        }
        entity.setProduct(product);
        entity.setQuantity(request.getQuantity());
    }

    public PageResponse<CartItemResponse> toPageResponse(Page<CartItems> page) {
        return PageResponse.<CartItemResponse>builder()
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
