package poly.edu.ASSM.api.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Services.core.CategoryService;
import poly.edu.ASSM.Services.core.ProductService;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.ProductMapper;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("@adminAuth.has('PRODUCT_VIEW')")
public class AdminProductApiController {

    @Autowired
    private ProductService prt;
    @Autowired
    private CategoryService ctr;
    @Autowired
    private CloudinaryService cloudService;
    @Autowired
    private ProductMapper productMapper;

    private static Map<String, Object> toRow(ProductResponse p) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", p.getId());
        row.put("name", p.getName());
        row.put("price", p.getMinPrice());
        row.put("quantity", 0);
        row.put("categoryName", p.getCategoryName());
        row.put("createDate", p.getCreatedAt());
        row.put("available", p.getStatus());
        row.put("stockStatus", Boolean.TRUE.equals(p.getInStock()) ? "IN" : "OUT");
        return row;
    }

    @GetMapping
    public Map<String, Object> index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String cat,
            @RequestParam(required = false) Long editId) {

        Page<Products> pages = prt.filterProducts(
                cat,
                keyword,
                null,
                null,
                PageRequest.of(page, size, Sort.by("id").descending()));

        Products product = editId != null ? prt.findById(editId) : new Products();
        if (product == null) {
            product = new Products();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Products p : pages.getContent()) {
            rows.add(toRow(productMapper.toResponse(p)));
        }

        ProductResponse formResp = productMapper.toResponse(product);
        Map<String, Object> form = new HashMap<>();
        form.put("id", formResp.getId());
        form.put("name", formResp.getName());
        form.put("price", formResp.getMinPrice());
        form.put("description", formResp.getDescription());
        form.put("available", formResp.getStatus() != null ? formResp.getStatus() : true);
        form.put("image", formResp.getImageUrl());
        form.put("inventoryQuantity", 0);
        form.put("categoryId", formResp.getCategoryId() != null ? formResp.getCategoryId() : "");

        return Map.of(
                "products", rows,
                "pages", Map.of(
                        "number", pages.getNumber(),
                        "totalPages", pages.getTotalPages(),
                        "totalElements", pages.getTotalElements(),
                        "size", pages.getSize()),
                "keyword", keyword,
                "cat", cat,
                "categories", ctr.findAll(),
                "productForm", form);
    }

    @PostMapping(value = "/save", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> save(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) Double price,
            @RequestParam(name = "inventory.quantity", required = false) Integer inventoryQuantity,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) MultipartFile uploadImage,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") Boolean available) {

        Products product = id != null ? prt.findById(id) : new Products();
        if (product == null) {
            product = new Products();
        }
        product.setName(name);
        product.setDescription(description);
        product.setStatus(available);
        if (categoryId != null && !categoryId.isBlank()) {
            product.setCategory(ctr.findById(categoryId));
        }

        int qty = inventoryQuantity != null ? inventoryQuantity : 1;
        try {
            prt.create(product, qty);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Lưu thành công!"));
        } catch (InvalidInputException e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            prt.delete(id);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Xóa sản phẩm thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Xóa thất bại!"));
        }
    }
}
