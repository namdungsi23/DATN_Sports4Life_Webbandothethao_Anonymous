package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.dto.request.CategoryRequest;
import poly.edu.ASSM.dto.response.CategoryResponse;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.CategoryMapper;

/**
 * Use-case admin category: Entity chỉ trong Service; API nhận/trả DTO qua Mapper.
 */
@Service
public class AdminCategoryService {

    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> list(String keyword, String size, int page) {
        List<CategoryResponse> items;
        int totalPages = 1;
        String key = keyword != null ? keyword : "";

        if (size == null || size.equals("all")) {
            items = categoryMapper.toResponseList(categoryService.findAll());
        } else {
            int pageSize = Integer.parseInt(size);
            Page<Category> result = categoryService.search(key, PageRequest.of(page, pageSize));
            items = categoryMapper.toResponseList(result.getContent());
            totalPages = result.getTotalPages();
        }

        return Map.of(
                "categories", items,
                "totalPages", totalPages,
                "currentPage", page,
                "keyword", key,
                "size", size != null ? size : "all");
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(String id) {
        try {
            return categoryMapper.toResponse(categoryService.findById(id));
        } catch (RuntimeException e) {
            throw new InvalidInputException("Không tìm thấy danh mục.");
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> filterPage(
            String keyword, int page, int pageSize, String dir, String sortBy) {
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        return categoryMapper.toPageResponse(categoryService.filterCategories(keyword, pageable));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw InvalidInputException.of("name", "Tên danh mục không được để trống");
        }
        Category entity = categoryMapper.toEntity(request);
        try {
            return categoryMapper.toResponse(categoryService.create(entity));
        } catch (RuntimeException e) {
            throw new InvalidInputException(e.getMessage());
        }
    }

    @Transactional
    public CategoryResponse update(CategoryRequest request) {
        if (request == null || request.getId() == null || request.getId().isBlank()) {
            throw InvalidInputException.of("id", "Thiếu mã danh mục");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw InvalidInputException.of("name", "Tên danh mục không được để trống");
        }
        Category entity = categoryMapper.toEntity(request);
        try {
            return categoryMapper.toResponse(categoryService.update(entity));
        } catch (RuntimeException e) {
            throw new InvalidInputException(e.getMessage());
        }
    }

    @Transactional
    public void delete(String id) {
        try {
            categoryService.delete(id);
        } catch (RuntimeException e) {
            throw new InvalidInputException(e.getMessage());
        }
    }
}
