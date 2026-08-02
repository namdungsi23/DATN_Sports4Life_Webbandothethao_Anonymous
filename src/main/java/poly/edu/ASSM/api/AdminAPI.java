package poly.edu.ASSM.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminCategoryService;
import poly.edu.ASSM.Services.core.AdminProductCatalogService;
import poly.edu.ASSM.dto.response.CategoryResponse;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;

/** Admin v2 API — chỉ Service + DTO. */
@RestController
@RequestMapping("/api/admin/v2")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminAPI {

    @Autowired
    private AdminCategoryService adminCategoryService;

    @Autowired
    private AdminProductCatalogService adminProductCatalogService;

    @GetMapping("/categories")
    public ResponseEntity<PageResponse<CategoryResponse>> getCategoryResponse(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(adminCategoryService.filterPage(keyword, page, pageSize, dir, sortBy));
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<ProductResponse>> getProductResponse(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "name") String sortBy) {
        return ResponseEntity.ok(adminProductCatalogService.filterProductsPage(
                cat, keyword, min, max, page, pageSize, dir, sortBy));
    }
}
