package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.transaction.Transactional;
import poly.edu.ASSM.entity.Category;
import poly.edu.ASSM.entity.ProductImages;
import poly.edu.ASSM.entity.ProductVariants;
import poly.edu.ASSM.entity.Products;
import poly.edu.ASSM.repository.ProductImageRepository;
import poly.edu.ASSM.repository.ProductRepository;
import poly.edu.ASSM.repository.ProductVariantRepository;
import poly.edu.ASSM.dto.request.AdminVariantSaveRequest;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.ProductMapper;
import poly.edu.ASSM.mapper.ProductVariantMapper;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.Services.util.ImageColorUtils;

@Service
public class AdminProductCatalogServiceImpl implements AdminProductCatalogService {

    private static final int MAX_IMAGES_PER_VARIANT = 4;

    private static final Pattern DRIVE_FILE_PATTERN =
            Pattern.compile("drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)");
    private static final Pattern DRIVE_OPEN_PATTERN =
            Pattern.compile("drive\\.google\\.com/open\\?id=([a-zA-Z0-9_-]+)");
    private static final Pattern DRIVE_UC_PATTERN =
            Pattern.compile("drive\\.google\\.com/uc\\?(?:export=download&|export=view&|)id=([a-zA-Z0-9_-]+)");

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductVariantRepository variantRepository;
    @Autowired
    private ProductImageRepository imageRepository;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductVariantMapper variantMapper;

    @Transactional
    @Override
    public Products saveProduct(Long id, String name, String description, String categoryId, Boolean available) {
        if (name == null || name.isBlank()) {
            throw new InvalidInputException("Tên sản phẩm không được để trống.");
        }

        Products product;
        if (id != null) {
            product = productRepository.findById(id)
                    .orElseThrow(() -> new InvalidInputException("Không tìm thấy sản phẩm."));
        } else {
            product = new Products();
            product.setCreatedAt(Instant.now());
        }

        product.setName(name.trim());
        product.setDescription(description);
        product.setStatus(available != null ? available : Boolean.TRUE);
        product.setUpdatedAt(Instant.now());
        product.setCategory(resolveCategory(categoryId));

        Products saved = productRepository.save(product);

        if (variantRepository.countByProduct_Id(saved.getId()) == 0) {
            createDefaultVariant(saved);
        }

        return saved;
    }

    @Transactional
    @Override
    public Products createProductWithDefaultVariant(String name, String description, String categoryId,
            Boolean available) {
        return saveProduct(null, name, description, categoryId, available);
    }

    @Override
    public List<ProductVariantResponse> listVariants(Long productId) {
        return variantRepository.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).stream()
                .map(variantMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductVariantResponse getVariantDetail(Long variantId) {
        ProductVariants variant = variantRepository.findDetailedById(variantId)
                .orElseThrow(() -> new InvalidInputException("Không tìm thấy biến thể."));
        return variantMapper.toDetailResponse(variant);
    }

    @Transactional
    @Override
    public ProductVariantResponse saveVariant(AdminVariantSaveRequest request) {
        if (request == null || request.getProductId() == null) {
            throw new InvalidInputException("Thiếu thông tin sản phẩm.");
        }

        Products product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new InvalidInputException("Không tìm thấy sản phẩm."));

        ProductVariants variant;
        if (request.getId() != null) {
            variant = variantRepository.findById(request.getId())
                    .orElseThrow(() -> new InvalidInputException("Không tìm thấy biến thể."));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new InvalidInputException("Biến thể không thuộc sản phẩm này.");
            }
        } else {
            variant = new ProductVariants();
            variant.setProduct(product);
            variant.setCreatedAt(Instant.now());
            variant.setDisplayOrder((int) variantRepository.countByProduct_Id(product.getId()) + 1);
            if (variantRepository.countByProduct_Id(product.getId()) == 0) {
                variant.setIsDefault(true);
            }
        }

        if (request.getSku() == null || request.getSku().isBlank()) {
            throw InvalidInputException.of("sku", "SKU không được để trống.");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw InvalidInputException.of("price", "Giá biến thể phải lớn hơn 0.");
        }
        if (request.getQuantity() == null) {
            throw InvalidInputException.of("quantity", "Số lượng là bắt buộc (nhập tay, không tự tăng).");
        }
        if (request.getQuantity() < 0 || request.getQuantity() > 9999) {
            throw InvalidInputException.of("quantity", "Số lượng phải từ 0 đến 9999.");
        }

        String size = request.getSize() != null ? request.getSize().trim() : "";
        String color = request.getColor() != null ? request.getColor().trim() : "";
        if (size.isBlank() || isPlaceholderAttr(size)) {
            throw InvalidInputException.of("size", "Kích cỡ không được để trống.");
        }
        if (color.isBlank() || isPlaceholderAttr(color)) {
            throw InvalidInputException.of("color", "Màu sắc không được để trống.");
        }

        for (ProductVariants existing : variantRepository
                .findByProduct_IdOrderByDisplayOrderAscIdAsc(product.getId())) {
            if (request.getId() != null && request.getId().equals(existing.getId())) {
                continue;
            }
            String es = existing.getSize() != null ? existing.getSize().trim() : "";
            String ec = existing.getColor() != null ? existing.getColor().trim() : "";
            if (size.equalsIgnoreCase(es) && color.equalsIgnoreCase(ec)) {
                throw InvalidInputException.of("color", "Đã tồn tại biến thể với màu và size này.");
            }
        }

        variant.setSku(request.getSku().trim());
        variant.setSize(size);
        variant.setColor(color);
        variant.setPrice(request.getPrice());
        variant.setQuantity(request.getQuantity().shortValue());
        variant.setStatus(request.getStatus() != null ? request.getStatus() : Boolean.TRUE);
        variant.setUpdatedAt(Instant.now());

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultVariant(product.getId());
            variant.setIsDefault(true);
        } else if (variant.getIsDefault() == null) {
            variant.setIsDefault(false);
        }

        ensureDefaultVariantExists(product.getId(), variant);

        ProductVariants saved = variantRepository.save(variant);
        // Biến thể mới cùng màu → tự lấy 4 góc từ size đã có ảnh
        copyImagesFromSameColorIfEmpty(saved);
        return variantMapper.toDetailResponse(
                variantRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    /** Khi thêm size mới cùng màu đã có ảnh — copy URL (không upload lại Cloudinary). */
    private void copyImagesFromSameColorIfEmpty(ProductVariants target) {
        if (target == null || target.getId() == null || target.getProduct() == null) {
            return;
        }
        if (imageRepository.countByVariant_Id(target.getId()) > 0) {
            return;
        }
        String color = target.getColor() != null ? target.getColor().trim() : "";
        if (color.isBlank() || ImageColorUtils.isPlaceholder(color)) {
            return;
        }
        for (ProductVariants other : variantRepository
                .findByProduct_IdOrderByDisplayOrderAscIdAsc(target.getProduct().getId())) {
            if (other.getId() == null || other.getId().equals(target.getId())) {
                continue;
            }
            String otherColor = other.getColor() != null ? other.getColor().trim() : "";
            if (!color.equalsIgnoreCase(otherColor)) {
                continue;
            }
            List<ProductImages> sourceImages = imageRepository
                    .findByVariant_IdOrderBySortOrderAscIdAsc(other.getId())
                    .stream()
                    .limit(MAX_IMAGES_PER_VARIANT)
                    .toList();
            if (!sourceImages.isEmpty()) {
                replaceVariantImageUrlsOnly(target, sourceImages);
                return;
            }
        }
    }

    @Transactional
    @Override
    public void deleteVariant(Long variantId) {
        ProductVariants variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new InvalidInputException("Không tìm thấy biến thể."));
        Long productId = variant.getProduct().getId();

        List<ProductImages> images = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        List<String> urls = images.stream()
                .map(ProductImages::getImageUrl)
                .filter(u -> u != null && !u.isBlank())
                .distinct()
                .toList();
        imageRepository.deleteAll(images);
        variantRepository.delete(variant);

        for (String url : urls) {
            if (!isImageUrlReferenced(url)) {
                deleteCloudinaryAsset(url);
            }
        }

        if (variantRepository.countByProduct_Id(productId) == 0) {
            createDefaultVariant(productRepository.findById(productId).orElseThrow());
        } else if (Boolean.TRUE.equals(variant.getIsDefault())) {
            variantRepository.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).stream()
                    .findFirst()
                    .ifPresent(v -> {
                        v.setIsDefault(true);
                        variantRepository.save(v);
                    });
        }
    }

    @Transactional
    @Override
    public void deleteProduct(Long id) {
        List<ProductVariants> variants = variantRepository.findByProduct_IdOrderByDisplayOrderAscIdAsc(id);
        for (ProductVariants variant : variants) {
            List<ProductImages> images =
                    imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variant.getId());
            for (ProductImages image : images) {
                deleteCloudinaryAsset(image.getImageUrl());
            }
            imageRepository.deleteAll(images);
        }
        variantRepository.deleteAll(variants);
        productRepository.deleteById(id);
    }

    @Transactional
    @Override
    public List<ProductImageResponse> uploadImages(Long variantId, MultipartFile[] files) {
        Map<String, Object> result = uploadImagesAndDetectColor(variantId, files);
        @SuppressWarnings("unchecked")
        List<ProductImageResponse> images = (List<ProductImageResponse>) result.get("images");
        return images != null ? images : List.of();
    }

    @Transactional
    @Override
    public Map<String, Object> uploadImagesAndDetectColor(Long variantId, MultipartFile[] files) {
        return uploadImagesInternal(variantId, files, false);
    }

    @Transactional
    @Override
    public Map<String, Object> replaceImagesAndDetectColor(Long variantId, MultipartFile[] files) {
        return uploadImagesInternal(variantId, files, true);
    }

    private Map<String, Object> uploadImagesInternal(Long variantId, MultipartFile[] files, boolean replaceAll) {
        ProductVariants variant = loadVariant(variantId);
        if (files == null || files.length == 0) {
            throw new InvalidInputException("Không có ảnh để tải lên.");
        }

        List<String> pendingCloudinaryCleanup = List.of();
        if (replaceAll) {
            pendingCloudinaryCleanup = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId)
                    .stream()
                    .map(ProductImages::getImageUrl)
                    .filter(u -> u != null && !u.isBlank())
                    .distinct()
                    .toList();
            // Chỉ xóa bản ghi SQL trước; Cloudinary dọn sau khi đã upload + đồng bộ cùng màu
            clearVariantImageRowsOnly(variantId);
        }

        int existingCount = (int) imageRepository.countByVariant_Id(variantId);
        if (existingCount >= MAX_IMAGES_PER_VARIANT) {
            throw new InvalidInputException(
                    "Mỗi biến thể tối đa " + MAX_IMAGES_PER_VARIANT
                            + " ảnh (4 góc). Hãy xóa ảnh cũ hoặc dùng «Thay thế 4 góc».");
        }

        String productName = variant.getProduct() != null ? variant.getProduct().getName() : null;
        Long productId = variant.getProduct() != null ? variant.getProduct().getId() : null;
        String colorHint = variant.getColor();
        int nextOrder = nextSortOrder(variantId);
        int variantNo = resolveVariantNumber(variant);
        List<ProductImageResponse> uploaded = new ArrayList<>();
        String detectedColor = null;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (existingCount + uploaded.size() >= MAX_IMAGES_PER_VARIANT) {
                break;
            }
            int sortOrder = Math.min(nextOrder, MAX_IMAGES_PER_VARIANT);
            CloudinaryService.VariantImageUpload up =
                    cloudinaryService.uploadVariantImage(
                            file, productName, colorHint, productId, variantNo, sortOrder);
            if (detectedColor == null && up.detectedColor() != null && !up.detectedColor().isBlank()) {
                detectedColor = up.detectedColor().trim();
                if (ImageColorUtils.isPlaceholder(colorHint)) {
                    colorHint = detectedColor;
                }
            }
            ProductImages image = persistImage(variant, up.secureUrl(), sortOrder);
            nextOrder++;
            uploaded.add(productMapper.toImageResponse(image));
        }

        if (uploaded.isEmpty()) {
            throw new InvalidInputException("Không có ảnh hợp lệ để tải lên.");
        }

        boolean colorUpdated = applyDetectedColorIfNeeded(variant, detectedColor);
        // Đồng bộ 4 góc sang mọi size cùng màu → user/admin cùng thấy đúng theo màu
        int synced = propagateImagesToSameColor(variant);

        if (replaceAll) {
            // Dọn file Cloudinary cũ không còn được trỏ từ SQL
            for (String oldUrl : pendingCloudinaryCleanup) {
                if (!isImageUrlReferenced(oldUrl)) {
                    deleteCloudinaryAsset(oldUrl);
                }
            }
        }

        ProductVariants refreshed = variantRepository.findDetailedById(variant.getId()).orElse(variant);
        Map<String, Object> body = new HashMap<>();
        body.put("images",
                imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId).stream()
                        .limit(MAX_IMAGES_PER_VARIANT)
                        .map(productMapper::toImageResponse)
                        .toList());
        body.put("detectedColor", detectedColor != null ? detectedColor : "");
        body.put("colorUpdated", colorUpdated);
        body.put("syncedSameColorCount", synced);
        body.put("replaced", replaceAll);
        body.put("variant", variantMapper.toDetailResponse(refreshed));
        return body;
    }

    @Transactional
    @Override
    public List<ProductImageResponse> resyncImageNames(Long variantId) {
        ProductVariants variant = loadVariant(variantId);
        Long productId = variant.getProduct() != null ? variant.getProduct().getId() : null;
        if (productId == null) {
            throw new InvalidInputException("Biến thể thiếu sản phẩm.");
        }
        int variantNo = resolveVariantNumber(variant);
        renumberSortOrder(variantId);

        List<ProductImages> images = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        if (images.isEmpty()) {
            return List.of();
        }
        if (images.size() > MAX_IMAGES_PER_VARIANT) {
            // Giữ tối đa 4 góc; xóa phần thừa
            for (int i = MAX_IMAGES_PER_VARIANT; i < images.size(); i++) {
                deleteCloudinaryAsset(images.get(i).getImageUrl());
                imageRepository.delete(images.get(i));
            }
            images = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        }

        List<ProductImageResponse> result = new ArrayList<>();
        int order = 1;
        for (ProductImages image : images) {
            String oldUrl = image.getImageUrl();
            CloudinaryService.VariantImageUpload up =
                    cloudinaryService.uploadRemoteImageUrl(oldUrl, productId, variantNo, order);
            image.setImageUrl(up.secureUrl());
            image.setSortOrder(order);
            image.setIsDefault(order == 1);
            imageRepository.save(image);
            // Xóa asset cũ nếu public_id khác (tránh rác Cloudinary)
            if (oldUrl != null && !oldUrl.equals(up.secureUrl())) {
                String oldId = CloudinaryService.extractPublicId(oldUrl);
                String newId = CloudinaryService.extractPublicId(up.secureUrl());
                if (oldId != null && newId != null && !oldId.equals(newId)) {
                    deleteCloudinaryAsset(oldUrl);
                }
            }
            result.add(productMapper.toImageResponse(image));
            order++;
        }
        propagateImagesToSameColor(variant);
        return result;
    }

    private boolean applyDetectedColorIfNeeded(ProductVariants variant, String detectedColor) {
        if (detectedColor == null || detectedColor.isBlank()
                || !ImageColorUtils.isPlaceholder(variant.getColor())) {
            return false;
        }
        String size = variant.getSize() != null ? variant.getSize().trim() : "";
        boolean dup = false;
        if (!size.isBlank()) {
            for (ProductVariants existing : variantRepository
                    .findByProduct_IdOrderByDisplayOrderAscIdAsc(variant.getProduct().getId())) {
                if (existing.getId().equals(variant.getId())) {
                    continue;
                }
                String es = existing.getSize() != null ? existing.getSize().trim() : "";
                String ec = existing.getColor() != null ? existing.getColor().trim() : "";
                if (size.equalsIgnoreCase(es) && detectedColor.equalsIgnoreCase(ec)) {
                    dup = true;
                    break;
                }
            }
        }
        if (dup) {
            return false;
        }
        variant.setColor(detectedColor.trim());
        variant.setUpdatedAt(Instant.now());
        variantRepository.save(variant);
        return true;
    }

    /**
     * Đồng bộ ảnh (cùng URL Cloudinary) sang mọi size cùng màu trong sản phẩm.
     * User chọn màu → 4 góc giống nhau trên mọi size; admin list cũng thấy thumb đúng màu.
     */
    private int propagateImagesToSameColor(ProductVariants source) {
        if (source == null || source.getProduct() == null || source.getId() == null) {
            return 0;
        }
        String color = source.getColor() != null ? source.getColor().trim() : "";
        if (color.isBlank() || ImageColorUtils.isPlaceholder(color)) {
            return 0;
        }
        List<ProductImages> sourceImages = imageRepository
                .findByVariant_IdOrderBySortOrderAscIdAsc(source.getId())
                .stream()
                .limit(MAX_IMAGES_PER_VARIANT)
                .toList();
        if (sourceImages.isEmpty()) {
            return 0;
        }

        int synced = 0;
        for (ProductVariants other : variantRepository
                .findByProduct_IdOrderByDisplayOrderAscIdAsc(source.getProduct().getId())) {
            if (other.getId() == null || other.getId().equals(source.getId())) {
                continue;
            }
            String otherColor = other.getColor() != null ? other.getColor().trim() : "";
            if (!color.equalsIgnoreCase(otherColor)) {
                continue;
            }
            replaceVariantImageUrlsOnly(other, sourceImages);
            synced++;
        }
        return synced;
    }

    /** Chỉ ghi đè bản ghi SQL — không xóa file Cloudinary (URL dùng chung theo màu). */
    private void replaceVariantImageUrlsOnly(ProductVariants target, List<ProductImages> sourceImages) {
        List<ProductImages> old = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(target.getId());
        if (!old.isEmpty()) {
            imageRepository.deleteAll(old);
        }
        int order = 1;
        for (ProductImages src : sourceImages) {
            ProductImages copy = new ProductImages();
            copy.setVariant(target);
            copy.setImageUrl(src.getImageUrl());
            copy.setSortOrder(order);
            copy.setIsDefault(order == 1);
            copy.setCreatedAt(Instant.now());
            imageRepository.save(copy);
            order++;
        }
    }

    private void clearVariantImageRowsOnly(Long variantId) {
        List<ProductImages> images = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        if (!images.isEmpty()) {
            imageRepository.deleteAll(images);
        }
    }

    private void clearVariantImages(Long variantId) {
        List<ProductImages> images = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        for (ProductImages image : images) {
            deleteCloudinaryAsset(image.getImageUrl());
        }
        imageRepository.deleteAll(images);
    }

    @Transactional
    @Override
    public ProductImageResponse addImageFromUrl(Long variantId, String url) {
        ProductVariants variant = loadVariant(variantId);
        if (imageRepository.countByVariant_Id(variantId) >= MAX_IMAGES_PER_VARIANT) {
            throw new InvalidInputException("Mỗi biến thể tối đa " + MAX_IMAGES_PER_VARIANT + " ảnh.");
        }
        String normalized = normalizeExternalImageUrl(url);
        Long productId = variant.getProduct() != null ? variant.getProduct().getId() : null;
        int variantNo = resolveVariantNumber(variant);
        int sortOrder = nextSortOrder(variantId);

        // Nghiệp vụ: mọi ảnh phải nằm trên Cloudinary rồi mới lưu URL vào SQL
        String cloudUrl = normalized;
        if (!isOurCloudinaryUrl(normalized)) {
            CloudinaryService.VariantImageUpload up =
                    cloudinaryService.uploadRemoteImageUrl(normalized, productId, variantNo, sortOrder);
            cloudUrl = up.secureUrl();
        }

        ProductImages image = persistImage(variant, cloudUrl, sortOrder);

        String detectedColor = ImageColorUtils.fromImageUrl(cloudUrl).orElse(null);
        if (detectedColor == null || detectedColor.isBlank()) {
            detectedColor = ImageColorUtils.fromFilename(normalized).orElse(null);
        }
        applyDetectedColorIfNeeded(variant, detectedColor);
        propagateImagesToSameColor(variant);

        return productMapper.toImageResponse(image);
    }

    private static boolean isOurCloudinaryUrl(String url) {
        return url != null && url.contains("res.cloudinary.com/pnam233/");
    }

    @Transactional
    @Override
    public List<ProductImageResponse> reorderImages(Long variantId, List<Long> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            throw new InvalidInputException("Danh sách ảnh không hợp lệ.");
        }

        List<ProductImages> existing = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        Map<Long, ProductImages> byId = existing.stream()
                .collect(Collectors.toMap(ProductImages::getId, img -> img, (a, b) -> a, LinkedHashMap::new));

        int order = 1;
        for (Long imageId : imageIds) {
            ProductImages image = byId.get(imageId);
            if (image != null) {
                image.setSortOrder(order++);
                imageRepository.save(image);
            }
        }

        ProductVariants variant = loadVariant(variantId);
        propagateImagesToSameColor(variant);

        return imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId).stream()
                .map(productMapper::toImageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public void deleteImage(Long imageId) {
        ProductImages image = imageRepository.findById(imageId)
                .orElseThrow(() -> new InvalidInputException("Không tìm thấy ảnh."));
        Long variantId = image.getVariant().getId();
        String removedUrl = image.getImageUrl();
        imageRepository.delete(image);
        renumberSortOrder(variantId);

        ProductVariants variant = loadVariant(variantId);
        propagateImagesToSameColor(variant);

        // Chỉ xóa Cloudinary khi không còn biến thể nào trỏ URL (tránh gãy ảnh size cùng màu)
        if (!isImageUrlReferenced(removedUrl)) {
            deleteCloudinaryAsset(removedUrl);
        }
    }

    private boolean isImageUrlReferenced(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return false;
        }
        return imageRepository.countByImageUrl(imageUrl) > 0;
    }

    /** Đồng bộ xóa file trên Cloudinary; URL ngoài Cloudinary thì bỏ qua. */
    private void deleteCloudinaryAsset(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || cloudinaryService == null) {
            return;
        }
        try {
            cloudinaryService.deleteByImageUrl(imageUrl);
        } catch (Exception ignored) {
            // Best-effort: vẫn xóa SQL; file Cloudinary có thể dọn lại sau
        }
    }

    @Override
    public int totalQuantity(Products product) {
        return productMapper.totalQuantity(product);
    }

    @Transactional
    @Override
    public Map<String, Object> listProductsForAdmin(
            String categoryId, String keyword, int page, int size, String sortBy, String sortDir) {
        Page<Products> pages = findAdminProductsPage(categoryId, keyword, page, size, sortBy, sortDir);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Products entity : pages.getContent()) {
            rows.add(toAdminRow(entity));
        }

        Map<String, Object> pageMeta = new HashMap<>();
        pageMeta.put("number", pages.getNumber());
        pageMeta.put("totalPages", pages.getTotalPages());
        pageMeta.put("totalElements", pages.getTotalElements());
        pageMeta.put("size", pages.getSize());
        pageMeta.put("first", pages.isFirst());
        pageMeta.put("last", pages.isLast());

        Map<String, Object> response = new HashMap<>();
        response.put("products", rows);
        response.put("pages", pageMeta);
        response.put("totalPages", pages.getTotalPages());
        response.put("totalElements", pages.getTotalElements());
        response.put("currentPage", pages.getNumber());
        response.put("pageSize", pages.getSize());
        response.put("keyword", keyword != null ? keyword : "");
        response.put("cat", categoryId != null ? categoryId : "");
        response.put("sortBy", normalizeAdminSortBy(sortBy));
        response.put("sortDir", normalizeAdminSortDir(sortDir));
        response.put("categories", toCategoryOptions(categoryService.findAll()));
        return response;
    }

    private Page<Products> findAdminProductsPage(
            String categoryId, String keyword, int page, int size, String sortBy, String sortDir) {
        String normalizedSort = normalizeAdminSortBy(sortBy);
        boolean ascending = "asc".equalsIgnoreCase(normalizeAdminSortDir(sortDir));
        Pageable pageable = PageRequest.of(page, size);

        return switch (normalizedSort) {
            case "price" -> ascending
                    ? productRepository.findAdminProductsOrderByMinPriceAsc(categoryId, keyword, pageable)
                    : productRepository.findAdminProductsOrderByMinPriceDesc(categoryId, keyword, pageable);
            case "quantity" -> ascending
                    ? productRepository.findAdminProductsOrderByTotalQuantityAsc(categoryId, keyword, pageable)
                    : productRepository.findAdminProductsOrderByTotalQuantityDesc(categoryId, keyword, pageable);
            case "image" -> ascending
                    ? productRepository.findAdminProductsOrderByImageAsc(categoryId, keyword, pageable)
                    : productRepository.findAdminProductsOrderByImageDesc(categoryId, keyword, pageable);
            case "stockStatus" -> ascending
                    ? productRepository.findAdminProductsOrderByStockStatusAsc(categoryId, keyword, pageable)
                    : productRepository.findAdminProductsOrderByStockStatusDesc(categoryId, keyword, pageable);
            default -> {
                Sort.Direction direction = ascending ? Sort.Direction.ASC : Sort.Direction.DESC;
                String property = resolveSimpleAdminSortProperty(normalizedSort);
                yield productRepository.findAdminProducts(
                        categoryId,
                        keyword,
                        PageRequest.of(page, size, Sort.by(direction, property)));
            }
        };
    }

    private static String normalizeAdminSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return switch (sortBy.trim().toLowerCase()) {
            case "id" -> "id";
            case "image" -> "image";
            case "name" -> "name";
            case "price" -> "price";
            case "quantity" -> "quantity";
            case "categoryname", "category" -> "categoryName";
            case "createdate", "create_date" -> "createDate";
            case "stockstatus", "status" -> "stockStatus";
            default -> "id";
        };
    }

    private static String normalizeAdminSortDir(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir) ? "asc" : "desc";
    }

    private static String resolveSimpleAdminSortProperty(String sortBy) {
        return switch (sortBy) {
            case "name" -> "name";
            case "categoryName" -> "category.name";
            case "createDate" -> "createdAt";
            default -> "id";
        };
    }

    private List<Map<String, String>> toCategoryOptions(List<Category> categories) {
        List<Map<String, String>> options = new ArrayList<>();
        for (Category category : categories) {
            Map<String, String> option = new HashMap<>();
            option.put("id", category.getId() != null ? category.getId() : "");
            option.put("name", category.getName() != null ? category.getName() : "");
            options.add(option);
        }
        return options;
    }

    private Map<String, Object> toAdminRow(Products entity) {
        ProductResponse p = productMapper.toResponse(entity);
        Map<String, Object> row = new HashMap<>();
        row.put("id", p.getId());
        row.put("name", p.getName());
        row.put("price", p.getMinPrice());
        row.put("quantity", totalQuantity(entity));
        row.put("image", p.getImageUrl());
        row.put("categoryName", p.getCategoryName());
        row.put("createDate", p.getCreatedAt());
        row.put("available", p.getStatus());
        row.put("stockStatus", Boolean.TRUE.equals(p.getInStock()) ? "IN" : "OUT");
        return row;
    }

    private ProductVariants loadVariant(Long variantId) {
        return variantRepository.findById(variantId)
                .orElseThrow(() -> new InvalidInputException("Không tìm thấy biến thể."));
    }

    /**
     * Số thứ tự biến thể trong SP: 1 = biến thể mặc định, sau đó theo displayOrder/id.
     * Dùng cho public_id {@code sp{id}_{variantNo}_{sort}} — mỗi biến thể folder riêng,
     * không gộp theo màu (tránh ghi đè ảnh giữa các size).
     */
    private int resolveVariantNumber(ProductVariants variant) {
        Long productId = variant.getProduct().getId();
        List<ProductVariants> all =
                variantRepository.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId);
        all.sort((a, b) -> {
            boolean aDef = Boolean.TRUE.equals(a.getIsDefault());
            boolean bDef = Boolean.TRUE.equals(b.getIsDefault());
            if (aDef != bDef) {
                return aDef ? -1 : 1;
            }
            int ao = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
            int bo = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
            if (ao != bo) {
                return Integer.compare(ao, bo);
            }
            long ai = a.getId() != null ? a.getId() : Long.MAX_VALUE;
            long bi = b.getId() != null ? b.getId() : Long.MAX_VALUE;
            return Long.compare(ai, bi);
        });
        int idx = 1;
        for (ProductVariants v : all) {
            if (v.getId() != null && v.getId().equals(variant.getId())) {
                return idx;
            }
            idx++;
        }
        return Boolean.TRUE.equals(variant.getIsDefault()) ? 1 : Math.max(1, all.size());
    }

    private Category resolveCategory(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw new InvalidInputException("Vui lòng chọn danh mục.");
        }
        Category category = categoryService.findById(categoryId);
        if (category == null) {
            throw new InvalidInputException("Danh mục không hợp lệ.");
        }
        return category;
    }

    private ProductVariants createDefaultVariant(Products product) {
        ProductVariants variant = new ProductVariants();
        variant.setProduct(product);
        variant.setSku("SKU-" + product.getId());
        variant.setColor("Mặc định");
        variant.setSize("Free");
        variant.setPrice(BigDecimal.ZERO);
        variant.setQuantity((short) 0);
        variant.setIsDefault(true);
        variant.setDisplayOrder(1);
        variant.setStatus(true);
        variant.setSoldCount(0);
        variant.setCreatedAt(Instant.now());
        variant.setUpdatedAt(null);
        return variantRepository.save(variant);
    }

    private static boolean isPlaceholderAttr(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim().toLowerCase();
        return v.isEmpty()
                || "default".equals(v)
                || "mặc định".equals(v)
                || "mac dinh".equals(v);
    }

    private void clearDefaultVariant(Long productId) {
        variantRepository.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).forEach(v -> {
            if (Boolean.TRUE.equals(v.getIsDefault())) {
                v.setIsDefault(false);
                variantRepository.save(v);
            }
        });
    }

    private void ensureDefaultVariantExists(Long productId, ProductVariants current) {
        boolean hasDefault = variantRepository.findByProduct_IdOrderByDisplayOrderAscIdAsc(productId).stream()
                .anyMatch(v -> Boolean.TRUE.equals(v.getIsDefault()));
        if (!hasDefault) {
            current.setIsDefault(true);
        }
    }

    private int nextSortOrder(Long variantId) {
        return (int) imageRepository.countByVariant_Id(variantId) + 1;
    }

    private ProductImages persistImage(ProductVariants variant, String url, int sortOrder) {
        ProductImages image = new ProductImages();
        image.setVariant(variant);
        image.setImageUrl(url);
        image.setSortOrder(sortOrder);
        image.setIsDefault(sortOrder == 1);
        image.setCreatedAt(Instant.now());
        return imageRepository.save(image);
    }

    private void renumberSortOrder(Long variantId) {
        List<ProductImages> images = imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId);
        int order = 1;
        for (ProductImages image : images) {
            image.setSortOrder(order);
            image.setIsDefault(order == 1);
            imageRepository.save(image);
            order++;
        }
    }

    static String normalizeExternalImageUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidInputException("URL không hợp lệ.");
        }
        String trimmed = url.trim();

        Matcher fileMatcher = DRIVE_FILE_PATTERN.matcher(trimmed);
        if (fileMatcher.find()) {
            return "https://drive.google.com/uc?export=view&id=" + fileMatcher.group(1);
        }

        Matcher openMatcher = DRIVE_OPEN_PATTERN.matcher(trimmed);
        if (openMatcher.find()) {
            return "https://drive.google.com/uc?export=view&id=" + openMatcher.group(1);
        }

        Matcher ucMatcher = DRIVE_UC_PATTERN.matcher(trimmed);
        if (ucMatcher.find()) {
            return "https://drive.google.com/uc?export=view&id=" + ucMatcher.group(1);
        }

        return trimmed;
    }
}
