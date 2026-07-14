package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.entity.ProductImages;
import poly.edu.ASSM.entity.ProductVariants;
import poly.edu.ASSM.dto.request.ProductImageRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.ProductImageResponse;

@Component
public class ProductImageMapper {

    public ProductImageResponse toResponse(ProductImages entity) {
        if (entity == null) {
            return null;
        }
        ProductVariants variant = entity.getVariant();
        return ProductImageResponse.builder()
                .id(entity.getId())
                .variantId(variant != null ? variant.getId() : null)
                .productId(variant != null && variant.getProduct() != null ? variant.getProduct().getId() : null)
                .imageUrl(entity.getImageUrl())
                .isDefault(entity.getIsDefault())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<ProductImageResponse> toResponseList(Collection<ProductImages> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProductImages toEntity(ProductImageRequest request, ProductVariants variant) {
        ProductImages entity = new ProductImages();
        applyRequest(entity, request, variant);
        return entity;
    }

    public void applyRequest(ProductImages entity, ProductImageRequest request, ProductVariants variant) {
        if (entity == null || request == null) {
            return;
        }
        entity.setVariant(variant);
        entity.setImageUrl(request.getImageUrl());
        entity.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : Boolean.FALSE);
        entity.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 1);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
    }

    public PageResponse<ProductImageResponse> toPageResponse(Page<ProductImages> page) {
        return PageResponse.<ProductImageResponse>builder()
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
