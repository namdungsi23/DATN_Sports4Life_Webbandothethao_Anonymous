package poly.edu.ASSM.api.admin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<CategoryResponse> one(@PathVariable String id) {
        try {
            return ResponseEntity.ok(categoryMapper.toResponse(categoryService.findById(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody Category category) {
        try {
            categoryService.create(category);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Lưu thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Lưu thất bại!"));
        }
    }

    @PutMapping
    public ResponseEntity<Map<String, Object>> update(@RequestBody Category category) {
        if (category.getId() == null || category.getId().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Thiếu mã danh mục"));
        }
        categoryService.update(category);
        return ResponseEntity.ok(Map.of("ok", true, "message", "Cập nhật thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String id) {
        try {
            categoryService.delete(id);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Đã xóa thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Xóa thất bại!"));
        }
    }
}
