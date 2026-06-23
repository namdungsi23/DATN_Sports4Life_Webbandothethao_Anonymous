package poly.edu.ASSM.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Entity.ProductImages;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.dto.request.ProductRequest;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;

@Component
public class ProductMapper {

    private final ProductVariantMapper variantMapper;

    public ProductMapper(ProductVariantMapper variantMapper) {
        this.variantMapper = variantMapper;
    }

    public ProductResponse toResponse(Products entity) {
        if (entity == null) {
            return null;
        }

        List<ProductVariantResponse> variants = variantMapper.toResponseList(entity.getProductVariants());
        List<ProductImageResponse> images = toImageResponses(entity);

        BigDecimal minPrice = variants.stream()
                .map(ProductVariantResponse::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        BigDecimal maxPrice = variants.stream()
                .map(ProductVariantResponse::getPrice)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        boolean inStock = variants.stream().anyMatch(v -> Boolean.TRUE.equals(v.getInStock()));

        String imageUrl = images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsDefault()))
                .map(ProductImageResponse::getImageUrl)
                .findFirst()
                .orElse(images.isEmpty() ? null : images.get(0).getImageUrl());

        Category category = entity.getCategory();

        return ProductResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .brand(entity.getBrand())
                .categoryId(category != null ? category.getId() : null)
                .categoryName(category != null ? category.getName() : null)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .imageUrl(imageUrl)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStock(inStock)
                .variants(variants)
                .images(images)
                .build();
    }

    public List<ProductResponse> toResponseList(List<Products> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PageResponse<ProductResponse> toPageResponse(Page<Products> page) {
        return PageResponse.<ProductResponse>builder()
                .content(toResponseList(page.getContent()))
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .number(page.getNumber())
                .size(page.getSize())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    public Products toEntity(ProductRequest request, Category category) {
        Products entity = new Products();
        applyRequest(entity, request, category);
        return entity;
    }

    public void applyRequest(Products entity, ProductRequest request, Category category) {
        if (entity == null || request == null) {
            return;
        }

        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setBrand(request.getBrand());
        entity.setCategory(category);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : Boolean.TRUE);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
    }

    private List<ProductImageResponse> toImageResponses(Products entity) {
        if (entity.getProductVariants() == null || entity.getProductVariants().isEmpty()) {
            return List.of();
        }

        List<ProductImages> allImages = new ArrayList<>();
        for (ProductVariants variant : entity.getProductVariants()) {
            if (variant.getProductImages() != null) {
                allImages.addAll(variant.getProductImages());
            }
        }

        return allImages.stream()
                .map(this::toImageResponse)
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((ProductImageResponse img) -> Boolean.TRUE.equals(img.getIsDefault()) ? 0 : 1)
                        .thenComparing(img -> img.getSortOrder() != null ? img.getSortOrder() : 0))
                .collect(Collectors.toList());
    }

    public ProductImageResponse toImageResponse(ProductImages entity) {
        if (entity == null) {
            return null;
        }
        return ProductImageResponse.builder()
                .id(entity.getId())
                .imageUrl(entity.getImageUrl())
                .isDefault(entity.getIsDefault())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
