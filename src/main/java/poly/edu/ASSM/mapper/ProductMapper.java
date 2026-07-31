package poly.edu.ASSM.mapper;

import java.math.BigDecimal;
import java.time.Instant;
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

        String imageUrl = resolveProductThumbnail(entity);

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

    public int totalQuantity(Products entity) {
        if (entity == null || entity.getProductVariants() == null) {
            return 0;
        }
        return entity.getProductVariants().stream()
                .map(v -> v.getQuantity() != null ? v.getQuantity() : 0)
                .mapToInt(Number::intValue)
                .sum();
    }

    public String resolveProductThumbnail(Products entity) {
        ProductVariants preferred = findVariantWithImages(entity, true);
        if (preferred == null) {
            return null;
        }
        return preferred.getProductImages().stream()
                .filter(img -> img != null && img.getImageUrl() != null && !img.getImageUrl().isBlank())
                .sorted(Comparator.comparing(
                        img -> img.getSortOrder() != null ? img.getSortOrder() : Integer.MAX_VALUE))
                .map(ProductImages::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    private ProductVariants findDefaultVariant(Products entity) {
        if (entity.getProductVariants() == null || entity.getProductVariants().isEmpty()) {
            return null;
        }
        return entity.getProductVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .orElse(entity.getProductVariants().iterator().next());
    }

    /**
     * Ưu tiên biến thể mặc định nếu có ảnh; nếu không thì lấy biến thể đầu tiên có ảnh.
     * Tránh mất thumbnail khi gắn "mặc định" cho biến thể chưa upload ảnh.
     */
    private ProductVariants findVariantWithImages(Products entity, boolean preferDefault) {
        if (entity == null || entity.getProductVariants() == null || entity.getProductVariants().isEmpty()) {
            return null;
        }

        if (preferDefault) {
            ProductVariants def = findDefaultVariant(entity);
            if (hasImages(def)) {
                return def;
            }
        }

        return entity.getProductVariants().stream()
                .sorted(Comparator.comparing(
                        v -> v.getDisplayOrder() != null ? v.getDisplayOrder() : Integer.MAX_VALUE))
                .filter(this::hasImages)
                .findFirst()
                .orElse(null);
    }

    private boolean hasImages(ProductVariants variant) {
        if (variant == null || variant.getProductImages() == null || variant.getProductImages().isEmpty()) {
            return false;
        }
        return variant.getProductImages().stream()
                .anyMatch(img -> img != null && img.getImageUrl() != null && !img.getImageUrl().isBlank());
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
        ProductVariants preferred = findVariantWithImages(entity, true);
        if (preferred == null || preferred.getProductImages() == null) {
            return List.of();
        }

        return preferred.getProductImages().stream()
                .filter(img -> img != null && img.getImageUrl() != null && !img.getImageUrl().isBlank())
                .sorted(Comparator.comparing(
                        img -> img.getSortOrder() != null ? img.getSortOrder() : Integer.MAX_VALUE))
                .limit(4)
                .map(this::toImageResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public ProductImageResponse toImageResponse(ProductImages entity) {
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
}
