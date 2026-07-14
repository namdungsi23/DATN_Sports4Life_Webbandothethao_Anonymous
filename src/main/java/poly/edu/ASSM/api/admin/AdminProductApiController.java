package poly.edu.ASSM.api.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.Services.core.AdminProductCatalogService;
import poly.edu.ASSM.Services.core.ProductService;
import poly.edu.ASSM.dto.request.AdminVariantSaveRequest;
import poly.edu.ASSM.dto.request.ImageReorderRequest;
import poly.edu.ASSM.dto.request.ImageUrlRequest;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.ProductMapper;

/**
 * Không catch InvalidInputException ở đây — để {@link poly.edu.ASSM.exception.ApiExceptionHandler}
 * trả JSON chuẩn cho FE.
 */
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("@adminAuth.has('PRODUCT_VIEW')")
public class AdminProductApiController {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private AdminProductCatalogService catalogService;

    @GetMapping
    public Map<String, Object> index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String cat,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return catalogService.listProductsForAdmin(cat, keyword, page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable Long id) {
        Products product = productService.findById(id);
        if (product == null) {
            throw new InvalidInputException("Không tìm thấy sản phẩm.");
        }
        ProductResponse resp = productMapper.toResponse(product);
        Map<String, Object> form = new HashMap<>();
        form.put("id", resp.getId());
        form.put("name", resp.getName());
        form.put("description", resp.getDescription());
        form.put("available", resp.getStatus() != null ? resp.getStatus() : true);
        form.put("image", resp.getImageUrl());
        form.put("categoryId", resp.getCategoryId() != null ? resp.getCategoryId() : "");
        return Map.of("product", form);
    }

    @GetMapping("/{productId}/variants")
    public Map<String, Object> listVariants(@PathVariable Long productId) {
        return Map.of("variants", catalogService.listVariants(productId));
    }

    @GetMapping("/variants/{variantId}")
    public ProductVariantResponse getVariant(@PathVariable Long variantId) {
        return catalogService.getVariantDetail(variantId);
    }

    @PostMapping("/save")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE') or @adminAuth.has('PRODUCT_CREATE')")
    public ResponseEntity<Map<String, Object>> saveProduct(
            @RequestParam(required = false) Long id,
            @RequestParam String name,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "true") Boolean available) {
        Products saved = catalogService.saveProduct(id, name, description, categoryId, available);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Lưu sản phẩm thành công!",
                "productId", saved.getId()));
    }

    @PostMapping("/variants/save")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE') or @adminAuth.has('PRODUCT_CREATE')")
    public ResponseEntity<Map<String, Object>> saveVariant(@RequestBody AdminVariantSaveRequest request) {
        ProductVariantResponse variant = catalogService.saveVariant(request);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Lưu biến thể thành công!",
                "variant", variant));
    }

    @PostMapping(value = "/variants/{variantId}/images", consumes = "multipart/form-data")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE')")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable Long variantId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "replace", defaultValue = "false") boolean replace) {
        Map<String, Object> result = replace
                ? catalogService.replaceImagesAndDetectColor(variantId, files)
                : catalogService.uploadImagesAndDetectColor(variantId, files);
        String detected = String.valueOf(result.getOrDefault("detectedColor", ""));
        boolean colorUpdated = Boolean.TRUE.equals(result.get("colorUpdated"));
        boolean replaced = Boolean.TRUE.equals(result.get("replaced"));
        int synced = 0;
        Object syncedObj = result.get("syncedSameColorCount");
        if (syncedObj instanceof Number n) {
            synced = n.intValue();
        }
        StringBuilder message = new StringBuilder();
        if (replaced) {
            message.append(colorUpdated
                    ? "Đã thay thế 4 góc ảnh! Đã tự gán màu: " + detected
                    : "Đã thay thế ảnh biến thể với tên chuẩn Cloudinary!");
        } else if (colorUpdated) {
            message.append("Tải ảnh thành công! Đã tự gán màu biến thể: ").append(detected);
        } else if (detected.isBlank()) {
            message.append("Tải ảnh thành công!");
        } else {
            message.append("Tải ảnh thành công! Gợi ý màu: ").append(detected);
        }
        if (synced > 0) {
            message.append(" Đã đồng bộ sang ").append(synced)
                    .append(" biến thể cùng màu (user/admin).");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("message", message.toString());
        body.put("images", result.get("images"));
        body.put("detectedColor", detected);
        body.put("colorUpdated", colorUpdated);
        body.put("syncedSameColorCount", synced);
        body.put("replaced", replaced);
        body.put("variant", result.get("variant"));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/variants/{variantId}/images/resync")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE')")
    public ResponseEntity<Map<String, Object>> resyncImageNames(@PathVariable Long variantId) {
        List<ProductImageResponse> images = catalogService.resyncImageNames(variantId);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Đã đồng bộ tên ảnh Cloudinary (sp{id}_{biến_thể}_{1-4}).",
                "images", images));
    }

    @GetMapping("/color-suggestions")
    public Map<String, Object> colorSuggestions() {
        return Map.of("colors", poly.edu.ASSM.Services.util.ImageColorUtils.suggestedColors());
    }

    @PostMapping("/variants/{variantId}/images/url")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE')")
    public ResponseEntity<Map<String, Object>> addImageUrl(
            @PathVariable Long variantId,
            @RequestBody ImageUrlRequest request) {
        ProductImageResponse image = catalogService.addImageFromUrl(variantId, request.getUrl());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Thêm ảnh từ URL thành công! Đã nhận diện màu và đồng bộ biến thể cùng màu (nếu có).",
                "image", image));
    }

    @PutMapping("/variants/{variantId}/images/reorder")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE')")
    public ResponseEntity<Map<String, Object>> reorderImages(
            @PathVariable Long variantId,
            @RequestBody ImageReorderRequest request) {
        List<ProductImageResponse> images = catalogService.reorderImages(variantId, request.getImageIds());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Cập nhật thứ tự ảnh thành công!",
                "images", images));
    }

    @DeleteMapping("/variants/images/{imageId}")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE')")
    public ResponseEntity<Map<String, Object>> deleteImage(@PathVariable Long imageId) {
        catalogService.deleteImage(imageId);
        return ResponseEntity.ok(Map.of("ok", true, "message", "Xóa ảnh thành công!"));
    }

    @DeleteMapping("/variants/{variantId}")
    @PreAuthorize("@adminAuth.has('PRODUCT_UPDATE')")
    public ResponseEntity<Map<String, Object>> deleteVariant(@PathVariable Long variantId) {
        catalogService.deleteVariant(variantId);
        return ResponseEntity.ok(Map.of("ok", true, "message", "Xóa biến thể thành công!"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@adminAuth.has('PRODUCT_DELETE')")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        catalogService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("ok", true, "message", "Xóa sản phẩm thành công!"));
    }
}
