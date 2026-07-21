package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.dto.request.ProductVariantRequest;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;

@Component
public class ProductVariantMapper {

    private final ProductImageMapper imageMapper;

    public ProductVariantMapper(ProductImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    public ProductVariantResponse toResponse(ProductVariants entity) {
        if (entity == null) {
            return null;
        }

        int quantity = entity.getQuantity() != null ? entity.getQuantity() : 0;
        boolean active = Boolean.TRUE.equals(entity.getStatus());

        List<ProductImageResponse> images = List.of();
        if (entity.getProductImages() != null && !entity.getProductImages().isEmpty()) {
            images = entity.getProductImages().stream()
                    .sorted(Comparator.comparing(
                            img -> img.getSortOrder() != null ? img.getSortOrder() : Integer.MAX_VALUE))
                    .map(imageMapper::toResponse)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }

        return ProductVariantResponse.builder()
                .id(entity.getId())
                .productId(entity.getProduct() != null ? entity.getProduct().getId() : null)
                .sku(entity.getSku())
                .size(entity.getSize())
                .color(entity.getColor())
                .price(entity.getPrice())
                .isDefault(entity.getIsDefault())
                .displayOrder(entity.getDisplayOrder())
                .status(entity.getStatus())
                .quantity(quantity)
                .inStock(active && quantity > 0)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .images(images)
                .build();
    }

    public List<ProductVariantResponse> toResponseList(Collection<ProductVariants> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                .sorted(Comparator
                        .comparing((ProductVariants v) -> v.getDisplayOrder() != null ? v.getDisplayOrder() : Integer.MAX_VALUE)
                        .thenComparing(v -> v.getId() != null ? v.getId() : Long.MAX_VALUE))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductVariantResponse toDetailResponse(ProductVariants entity) {
        return toResponse(entity);
    }

    public ProductVariants toEntity(ProductVariantRequest request, Products product) {
        ProductVariants entity = new ProductVariants();
        applyRequest(entity, request, product);
        return entity;
    }

    public void applyRequest(ProductVariants entity, ProductVariantRequest request, Products product) {
        if (entity == null || request == null) {
            return;
        }

        entity.setSku(request.getSku());
        entity.setSize(request.getSize());
        entity.setColor(request.getColor());
        entity.setPrice(request.getPrice());
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : Boolean.FALSE);
        entity.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : Boolean.TRUE);
        entity.setProduct(product);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
    }
}
