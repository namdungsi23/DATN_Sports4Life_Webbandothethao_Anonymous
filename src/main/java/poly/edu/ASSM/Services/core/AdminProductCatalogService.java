package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.dto.request.AdminVariantSaveRequest;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;

public interface AdminProductCatalogService {

    Products saveProduct(Long id, String name, String description, String categoryId, Boolean available);

    Products createProductWithDefaultVariant(String name, String description, String categoryId, Boolean available);

    List<ProductVariantResponse> listVariants(Long productId);

    ProductVariantResponse getVariantDetail(Long variantId);

    ProductVariantResponse saveVariant(AdminVariantSaveRequest request);

    void deleteVariant(Long variantId);

    void deleteProduct(Long id);

    List<ProductImageResponse> uploadImages(Long variantId, MultipartFile[] files);

    /** Upload ảnh + tự gán màu biến thể nếu đang trống/placeholder. */
    Map<String, Object> uploadImagesAndDetectColor(Long variantId, MultipartFile[] files);

    /**
     * Xóa ảnh cũ của biến thể rồi upload lại tối đa 4 ảnh với tên chuẩn
     * {@code sp{productId}_{variantNo}_{1..4}}.
     */
    Map<String, Object> replaceImagesAndDetectColor(Long variantId, MultipartFile[] files);

    /**
     * Đồng bộ lại public_id Cloudinary theo đúng sortOrder (1–4) của biến thể hiện tại.
     */
    List<ProductImageResponse> resyncImageNames(Long variantId);

    ProductImageResponse addImageFromUrl(Long variantId, String url);

    List<ProductImageResponse> reorderImages(Long variantId, List<Long> imageIds);

    void deleteImage(Long imageId);

    int totalQuantity(Products product);

    Map<String, Object> listProductsForAdmin(
            String categoryId, String keyword, int page, int size, String sortBy, String sortDir);
}
