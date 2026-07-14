package poly.edu.ASSM.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.entity.Category;
import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.Services.core.CategoryService;
import poly.edu.ASSM.Services.core.ProductService;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.mapper.CategoryMapper;
import poly.edu.ASSM.mapper.ProductMapper;

@RestController
@RequestMapping("/api/admin/v2")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminAPI {
    @Autowired
    CategoryService catService;

    @Autowired
    ProductService productService;

    @Autowired
    CategoryMapper categoryMapper;

    @Autowired
    ProductMapper productMapper;

    @GetMapping("/categories")
    public ResponseEntity<PageResponse<poly.edu.ASSM.dto.response.CategoryResponse>> getCategoryResponse(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "name") String sortBy) {

        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        Page<Category> categories = catService.filterCategories(keyword, pageable);
        return ResponseEntity.ok(categoryMapper.toPageResponse(categories));
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<poly.edu.ASSM.dto.response.ProductResponse>> getProductResponse(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "name") String sortBy) {

        Sort.Direction direction = dir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
        Page<Products> products = productService.filterProducts(cat, keyword, min, max, pageable);
        return ResponseEntity.ok(productMapper.toPageResponse(products));
    }
}
