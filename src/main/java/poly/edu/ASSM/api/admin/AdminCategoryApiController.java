package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.AdminCategoryService;
import poly.edu.ASSM.dto.request.CategoryRequest;
import poly.edu.ASSM.dto.response.CategoryResponse;

/** Thin API: Service + DTO. Không Entity / Repository. */
@RestController
@RequestMapping("/api/admin/categories")
@PreAuthorize("@adminAuth.has('CATEGORY_VIEW')")
public class AdminCategoryApiController {

    @Autowired
    private AdminCategoryService adminCategoryService;

    @GetMapping
    public Map<String, Object> index(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String size,
            @RequestParam(defaultValue = "0") int page) {
        return adminCategoryService.list(keyword, size, page);
    }

    @GetMapping("/{id}")
    public CategoryResponse one(@PathVariable String id) {
        return adminCategoryService.getById(id);
    }

    @PostMapping
    @PreAuthorize("@adminAuth.canWriteCatalog()")
    public Map<String, Object> create(@Valid @RequestBody CategoryRequest request) {
        adminCategoryService.create(request);
        return Map.of("ok", true, "message", "Lưu thành công!");
    }

    @PutMapping
    @PreAuthorize("@adminAuth.canWriteCatalog()")
    public Map<String, Object> update(@Valid @RequestBody CategoryRequest request) {
        adminCategoryService.update(request);
        return Map.of("ok", true, "message", "Cập nhật thành công");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@adminAuth.canWriteCatalog()")
    public Map<String, Object> delete(@PathVariable String id) {
        adminCategoryService.delete(id);
        return Map.of("ok", true, "message", "Đã xóa thành công!");
    }
}
