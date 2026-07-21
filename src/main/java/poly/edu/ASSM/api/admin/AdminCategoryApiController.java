package poly.edu.ASSM.api.admin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Services.core.CategoryService;
import poly.edu.ASSM.dto.response.CategoryResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.CategoryMapper;

@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("@adminAuth.has('CATEGORY_VIEW')")
public class AdminCategoryApiController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping
    public Map<String, Object> index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String size,
            @RequestParam(defaultValue = "0") int page) {
        List<Category> categories;
        int totalPages = 1;

        if (size == null || size.equals("all")) {
            categories = categoryService.findAll();
        } else {
            int pageSize = Integer.parseInt(size);
            Page<Category> result = categoryService.search(
                    keyword != null ? keyword : "",
                    PageRequest.of(page, pageSize));
            categories = result.getContent();
            totalPages = result.getTotalPages();
        }

        List<CategoryResponse> items = categoryMapper.toResponseList(categories);

        return Map.of(
                "categories", items,
                "totalPages", totalPages,
                "currentPage", page,
                "keyword", keyword != null ? keyword : "",
                "size", size != null ? size : "all");
    }

    @GetMapping("/{id}")
    public CategoryResponse one(@PathVariable String id) {
        try {
            return categoryMapper.toResponse(categoryService.findById(id));
        } catch (RuntimeException e) {
            throw new InvalidInputException("Không tìm thấy danh mục.");
        }
    }

    @PostMapping
    @PreAuthorize("@adminAuth.canWriteCatalog()")
    public Map<String, Object> create(@RequestBody Category category) {
        categoryService.create(category);
        return Map.of("ok", true, "message", "Lưu thành công!");
    }

    @PutMapping
    @PreAuthorize("@adminAuth.canWriteCatalog()")
    public Map<String, Object> update(@RequestBody Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            throw InvalidInputException.of("id", "Thiếu mã danh mục");
        }
        categoryService.update(category);
        return Map.of("ok", true, "message", "Cập nhật thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@adminAuth.canWriteCatalog()")
    public Map<String, Object> delete(@PathVariable String id) {
        categoryService.delete(id);
        return Map.of("ok", true, "message", "Đã xóa thành công!");
    }
}
