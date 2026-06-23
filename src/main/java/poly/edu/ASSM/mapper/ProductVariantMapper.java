package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Inventory;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.dto.request.ProductVariantRequest;
import poly.edu.ASSM.dto.response.ProductVariantResponse;

@Component
public class ProductVariantMapper {

    public ProductVariantResponse toResponse(ProductVariants entity) {
        return toResponse(entity, null);
    }

    public ProductVariantResponse toResponse(ProductVariants entity, Inventory inventory) {
        if (entity == null) {
            return null;
        }

        Inventory inv = inventory != null ? inventory : entity.getInventory();
        int quantity = inv != null && inv.getQuantity() != null ? inv.getQuantity() : 0;
        boolean active = Boolean.TRUE.equals(entity.getStatus());

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
                .build();
    }

    public List<ProductVariantResponse> toResponseList(Collection<ProductVariants> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
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
