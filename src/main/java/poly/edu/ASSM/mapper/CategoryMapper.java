package poly.edu.ASSM.mapper;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.dto.request.CategoryRequest;
import poly.edu.ASSM.dto.response.CategoryResponse;
import poly.edu.ASSM.dto.response.PageResponse;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category entity) {
        if (entity == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public List<CategoryResponse> toResponseList(Collection<Category> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public Category toEntity(CategoryRequest request) {
        Category entity = new Category();
        applyRequest(entity, request);
        return entity;
    }

    public void applyRequest(Category entity, CategoryRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setId(request.getId());
        entity.setName(request.getName());
    }

    public PageResponse<CategoryResponse> toPageResponse(Page<Category> page) {
        return PageResponse.<CategoryResponse>builder()
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
