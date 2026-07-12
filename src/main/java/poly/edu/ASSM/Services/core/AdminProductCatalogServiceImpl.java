package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Entity.ProductImages;
import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Repository.ProductImageRepository;
import poly.edu.ASSM.Repository.ProductRepository;
import poly.edu.ASSM.Repository.ProductVariantRepository;
import poly.edu.ASSM.dto.request.AdminVariantSaveRequest;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.ProductMapper;
import poly.edu.ASSM.mapper.ProductVariantMapper;
import poly.edu.ASSM.Services.util.CloudinaryService;

@Service
public class AdminProductCatalogServiceImpl implements AdminProductCatalogService {

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
            throw new InvalidInputException("SKU không được để trống.");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Giá biến thể phải lớn hơn 0.");
        }
        if (request.getQuantity() != null && request.getQuantity() < 0) {
            throw new InvalidInputException("Số lượng không được âm.");
        }

        String size = request.getSize() != null ? request.getSize().trim() : "";
        String color = request.getColor() != null ? request.getColor().trim() : "";
        long variantCount = variantRepository.countByProduct_Id(product.getId());
        // Thêm biến thể mới khi đã có ≥1, hoặc sửa khi SP đã có ≥2 → bắt buộc màu + size thật
        boolean requireAttrs =
                (request.getId() == null && variantCount >= 1) || variantCount >= 2;
        if (requireAttrs) {
            if (size.isBlank() || isPlaceholderAttr(size)) {
                throw new InvalidInputException("Kích cỡ không được để trống khi có nhiều biến thể.");
            }
            if (color.isBlank() || isPlaceholderAttr(color)) {
                throw new InvalidInputException("Màu sắc không được để trống khi có nhiều biến thể.");
            }
        }

        if (!size.isBlank() && !color.isBlank()) {
            for (ProductVariants existing : variantRepository
                    .findByProduct_IdOrderByDisplayOrderAscIdAsc(product.getId())) {
                if (request.getId() != null && request.getId().equals(existing.getId())) {
                    continue;
                }
                String es = existing.getSize() != null ? existing.getSize().trim() : "";
                String ec = existing.getColor() != null ? existing.getColor().trim() : "";
                if (size.equalsIgnoreCase(es) && color.equalsIgnoreCase(ec)) {
                    throw new InvalidInputException("Đã tồn tại biến thể với màu và size này.");
                }
            }
        }

        variant.setSku(request.getSku().trim());
        variant.setSize(size.isBlank() ? null : size);
        variant.setColor(color.isBlank() ? null : color);
        variant.setPrice(request.getPrice());
        variant.setQuantity(request.getQuantity() != null ? request.getQuantity().shortValue() : (short) 0);
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
        return variantMapper.toDetailResponse(
                variantRepository.findDetailedById(saved.getId()).orElse(saved));
    }

    @Transactional
    @Override
    public void deleteVariant(Long variantId) {
        ProductVariants variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new InvalidInputException("Không tìm thấy biến thể."));
        Long productId = variant.getProduct().getId();

        imageRepository.deleteAll(imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variantId));
        variantRepository.delete(variant);

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
            imageRepository.deleteAll(
                    imageRepository.findByVariant_IdOrderBySortOrderAscIdAsc(variant.getId()));
        }
        variantRepository.deleteAll(variants);
        productRepository.deleteById(id);
    }

    @Transactional
    @Override
    public List<ProductImageResponse> uploadImages(Long variantId, MultipartFile[] files) {
        ProductVariants variant = loadVariant(variantId);
        if (files == null || files.length == 0) {
            throw new InvalidInputException("Không có ảnh để tải lên.");
        }

        int nextOrder = nextSortOrder(variantId);
        List<ProductImageResponse> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String url = cloudinaryService.uploadImage(file);
            ProductImages image = persistImage(variant, url, nextOrder++);
            uploaded.add(productMapper.toImageResponse(image));
        }

        if (uploaded.isEmpty()) {
            throw new InvalidInputException("Không có ảnh hợp lệ để tải lên.");
        }

        return uploaded;
    }

    @Transactional
    @Override
    public ProductImageResponse addImageFromUrl(Long variantId, String url) {
        ProductVariants variant = loadVariant(variantId);
        String normalized = normalizeExternalImageUrl(url);
        ProductImages image = persistImage(variant, normalized, nextSortOrder(variantId));
        return productMapper.toImageResponse(image);
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
        imageRepository.delete(image);
        renumberSortOrder(variantId);
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
        return v.isEmpty() || "default".equals(v) || "mặc định".equals(v);
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
        return imageRepository.countByVariant_Id(variantId) + 1;
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
