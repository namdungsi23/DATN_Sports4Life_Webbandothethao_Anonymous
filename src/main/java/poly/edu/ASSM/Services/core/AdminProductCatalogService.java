package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.Entity.Products;
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

    ProductImageResponse addImageFromUrl(Long variantId, String url);

    List<ProductImageResponse> reorderImages(Long variantId, List<Long> imageIds);

    void deleteImage(Long imageId);

    int totalQuantity(Products product);

    Map<String, Object> listProductsForAdmin(
            String categoryId, String keyword, int page, int size, String sortBy, String sortDir);
}
