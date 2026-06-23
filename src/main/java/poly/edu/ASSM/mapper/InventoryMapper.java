package poly.edu.ASSM.mapper;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Inventory;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.dto.request.InventoryRequest;
import poly.edu.ASSM.dto.response.InventoryResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class InventoryMapper {

    public InventoryResponse toResponse(Inventory entity) {
        if (entity == null) {
            return null;
        }
        ProductVariants variant = entity.getVariant();
        return InventoryResponse.builder()
                .id(entity.getId())
                .variantId(variant != null ? variant.getId() : null)
                .sku(variant != null ? variant.getSku() : null)
                .quantity(entity.getQuantity())
                .lastUpdated(entity.getLastUpdated())
                .build();
    }

    public List<InventoryResponse> toResponseList(Collection<Inventory> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Inventory toEntity(InventoryRequest request, ProductVariants variant) {
        Inventory entity = new Inventory();
        applyRequest(entity, request, variant);
        return entity;
    }

    public void applyRequest(Inventory entity, InventoryRequest request, ProductVariants variant) {
        if (entity == null || request == null) {
            return;
        }
        entity.setVariant(variant);
        entity.setQuantity(request.getQuantity());
        entity.setLastUpdated(Instant.now());
    }

    public PageResponse<InventoryResponse> toPageResponse(Page<Inventory> page) {
        return PageResponse.<InventoryResponse>builder()
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
