package poly.edu.ASSM.Services.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Category;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Repository.ProductRepository;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;
import poly.edu.ASSM.mapper.ProductMapper;

@Service
public class PublicProductServiceImpl implements PublicProductService {

    private static final long CATEGORY_CACHE_TTL_MS = 5 * 60 * 1000L;

    private volatile List<Map<String, String>> categoriesCache;
    private volatile long categoriesCacheAt;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductMapper productMapper;

    @Override
    public Map<String, Object> getProductsPage(
            String cat,
            String keyword,
            Double min,
            Double max,
            int page,
            String sort,
            String dir) {

        String safeKeyword = normalizeKeyword(keyword);
        Double safeMin = normalizePrice(min, "Giá từ");
        Double safeMax = normalizePrice(max, "Giá đến");
        if (safeMin != null && safeMax != null && safeMin > safeMax) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá từ không được lớn hơn giá đến");
        }

        int safePage = Math.max(page, 0);
        String sortKey = resolveSortKey(sort);
        String dirKey = "desc".equalsIgnoreCase(dir) ? "desc" : "asc";

        Page<Products> productPage;
        if ("price".equals(sortKey)) {
            Pageable unsorted = PageRequest.of(safePage, 10);
            productPage = "desc".equals(dirKey)
                    ? productRepository.filterProductsOrderByMinPriceDesc(cat, safeKeyword, safeMin, safeMax, unsorted)
                    : productRepository.filterProductsOrderByMinPriceAsc(cat, safeKeyword, safeMin, safeMax, unsorted);
        } else {
            Sort.Direction direction = "desc".equals(dirKey) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(safePage, 10, Sort.by(direction, resolveSortProperty(sortKey)));
            productPage = productRepository.filterProducts(cat, safeKeyword, safeMin, safeMax, pageable);
        }

        List<Map<String, Object>> content = new ArrayList<>();
        for (Products entity : productPage.getContent()) {
            content.add(toFePayload(productMapper.toResponse(entity)));
        }

        Map<String, Object> pageBody = new HashMap<>();
        pageBody.put("content", content);
        pageBody.put("totalPages", productPage.getTotalPages());
        pageBody.put("totalElements", productPage.getTotalElements());
        pageBody.put("number", productPage.getNumber());
        pageBody.put("size", productPage.getSize());
        pageBody.put("first", productPage.isFirst());
        pageBody.put("last", productPage.isLast());

        Map<String, Object> filters = new HashMap<>();
        filters.put("cat", cat != null ? cat : "");
        filters.put("keyword", safeKeyword != null ? safeKeyword : "");
        filters.put("min", safeMin != null ? safeMin : 0);
        filters.put("max", safeMax != null ? safeMax : 0);
        filters.put("sort", sortKey);
        filters.put("dir", dirKey);
        filters.put("page", safePage);

        Map<String, Object> body = new HashMap<>();
        body.put("products", pageBody);
        body.put("categories", getCategories());
        body.put("filters", filters);
        return body;
    }

    @Override
    public List<Map<String, String>> getCategories() {
        long now = System.currentTimeMillis();
        List<Map<String, String>> cached = categoriesCache;
        if (cached != null && now - categoriesCacheAt < CATEGORY_CACHE_TTL_MS) {
            return cached;
        }

        List<Map<String, String>> categories = new ArrayList<>();
        for (Category c : categoryService.findAll()) {
            categories.add(Map.of(
                    "id", c.getId() != null ? c.getId().trim() : "",
                    "name", c.getName() != null ? c.getName() : ""));
        }
        categoriesCache = List.copyOf(categories);
        categoriesCacheAt = now;
        return categoriesCache;
    }

    @Override
    public Map<String, Object> getHomeData() {
        Map<String, Object> body = new HashMap<>();

        List<Map<String, Object>> bestSellers = new ArrayList<>();
        Page<Products> newestPage = productRepository.filterProducts(
                null, null, null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));
        for (Products entity : newestPage.getContent()) {
            bestSellers.add(toFePayload(productMapper.toResponse(entity)));
        }

        List<Map<String, Object>> featured = new ArrayList<>();
        Page<Products> featuredPage = productRepository.filterProductsOrderByMinPriceDesc(
                null, null, null, null, PageRequest.of(0, 5));
        for (Products entity : featuredPage.getContent()) {
            featured.add(toFePayload(productMapper.toResponse(entity)));
        }

        body.put("brands", getBrands().get("brands"));
        body.put("bestSellers", bestSellers);
        body.put("featured", featured);
        return body;
    }

    @Override
    public Map<String, Object> getProductDetail(Long id) {
        Products entity = productRepository.findDetailedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        ProductResponse response = productMapper.toResponse(entity);
        Map<String, Object> product = toFePayloadDetail(response);

        String categoryName = response.getCategoryName();
        Page<Products> relatedPage = productRepository.filterProducts(
                categoryName, null, null, null, PageRequest.of(0, 8));

        List<Map<String, Object>> related = new ArrayList<>();
        for (Products relatedEntity : relatedPage.getContent()) {
            if (relatedEntity.getId().equals(id)) {
                continue;
            }
            related.add(toFePayload(productMapper.toResponse(relatedEntity)));
            if (related.size() >= 6) {
                break;
            }
        }

        return Map.of("product", product, "related", related);
    }

    @Override
    public Map<String, Object> getBrands() {
        List<Object[]> rows = productRepository.findBrandSummaries();
        List<Map<String, Object>> brands = new ArrayList<>();

        if (rows.isEmpty()) {
            return Map.of("brands", buildBrandsFromProducts());
        }

        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }

            String name = String.valueOf(row[0]).trim();
            if (name.isEmpty()) {
                continue;
            }

            long productCount = row[1] instanceof Number number ? number.longValue() : 0L;
            String image = null;

            Page<Products> samplePage = productRepository.findSampleByBrand(name, PageRequest.of(0, 1));
            if (!samplePage.isEmpty()) {
                ProductResponse sample = productMapper.toResponse(samplePage.getContent().get(0));
                image = sample.getImageUrl();
                if (image == null || image.isBlank()) {
                    image = fallbackImageUrl(sample.getCategoryId(), sample.getId());
                }
            }

            Map<String, Object> brand = new HashMap<>();
            brand.put("name", name);
            brand.put("productCount", productCount);
            brand.put("image", image);
            brands.add(brand);
        }

        return Map.of("brands", brands);
    }

    private List<Map<String, Object>> buildBrandsFromProducts() {
        Page<Products> page = productRepository.filterProducts(null, null, null, null, PageRequest.of(0, 100));
        Map<String, Map<String, Object>> grouped = new java.util.LinkedHashMap<>();

        for (Products entity : page.getContent()) {
            ProductResponse response = productMapper.toResponse(entity);
            String brand = resolveBrand(response.getBrand(), response.getName());
            if (brand == null || brand.isBlank()) {
                continue;
            }

            String key = brand.toLowerCase();
            Map<String, Object> existing = grouped.get(key);
            if (existing != null) {
                long count = existing.get("productCount") instanceof Number number
                        ? number.longValue() + 1
                        : 1L;
                existing.put("productCount", count);
                continue;
            }

            String image = response.getImageUrl();
            if (image == null || image.isBlank()) {
                image = fallbackImageUrl(response.getCategoryId(), response.getId());
            }

            Map<String, Object> brandMap = new HashMap<>();
            brandMap.put("name", brand);
            brandMap.put("productCount", 1L);
            brandMap.put("image", image);
            grouped.put(key, brandMap);
        }

        return new ArrayList<>(grouped.values());
    }

    private static String resolveBrand(String brand, String productName) {
        if (brand != null && !brand.isBlank()) {
            return brand.trim();
        }
        if (productName == null || productName.isBlank()) {
            return null;
        }

        String[] knownBrands = {
                "New Balance", "Under Armour", "Nike", "Adidas", "Puma",
                "Converse", "Asics", "Reebok", "Vans", "Mizuno"
        };
        String lowerName = productName.toLowerCase();
        for (String known : knownBrands) {
            if (lowerName.contains(known.toLowerCase())) {
                return known;
            }
        }

        String firstWord = productName.trim().split("\\s+")[0];
        return firstWord.isBlank() ? null : firstWord;
    }

    private static String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Từ khóa tìm kiếm tối đa 100 ký tự");
        }
        return trimmed;
    }

    private static Double normalizePrice(Double value, String label) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " không được âm");
        }
        if (value > 100_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " vượt quá giới hạn cho phép");
        }
        return value;
    }

    private static String resolveSortKey(String sort) {
        if (sort == null || sort.isBlank()) {
            return "createDate";
        }
        return switch (sort.trim().toLowerCase()) {
            case "id" -> "id";
            case "name" -> "name";
            case "price" -> "price";
            case "available", "status" -> "status";
            case "createdate", "create_date", "createdat", "created_at" -> "createDate";
            default -> "createDate";
        };
    }

    private static String resolveSortProperty(String sortKey) {
        return switch (sortKey) {
            case "id" -> "id";
            case "name" -> "name";
            case "status" -> "status";
            default -> "createdAt";
        };
    }

    /** Format tương thích Vue FE (BestSeller, Featured, ProductView). */
    private static Map<String, Object> toFePayload(ProductResponse p) {
        Map<String, Object> m = new HashMap<>();
        if (p == null) {
            return m;
        }

        int totalQty = 0;
        if (p.getVariants() != null) {
            for (ProductVariantResponse v : p.getVariants()) {
                if (v.getQuantity() != null) {
                    totalQty += v.getQuantity();
                }
            }
        }

        String image = p.getImageUrl();
        if (image == null || image.isBlank()) {
            image = fallbackImageUrl(p.getCategoryId(), p.getId());
        }

        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("image", image);
        m.put("price", p.getMinPrice() != null ? p.getMinPrice().doubleValue() : 0);
        m.put("createDate", p.getCreatedAt());
        m.put("available", p.getStatus() != null ? p.getStatus() : true);
        m.put("description", p.getDescription());
        m.put("quantity", totalQty);
        m.put("inStock", Boolean.TRUE.equals(p.getInStock()));
        m.put("categoryName", p.getCategoryName());
        m.put("categoryId", p.getCategoryId());
        m.put("brand", resolveBrand(p.getBrand(), p.getName()));
        m.put("minPrice", p.getMinPrice());
        m.put("maxPrice", p.getMaxPrice());
        m.put("stock", totalQty);
        return m;
    }

    private Map<String, Object> toFePayloadDetail(ProductResponse p) {
        Map<String, Object> m = toFePayload(p);

        List<String> gallery = new ArrayList<>();
        if (p.getImages() != null) {
            for (ProductImageResponse img : p.getImages()) {
                if (img.getImageUrl() != null && !img.getImageUrl().isBlank()) {
                    addUnique(gallery, img.getImageUrl().trim());
                }
            }
        }

        List<Map<String, Object>> variants = new ArrayList<>();
        if (p.getVariants() != null) {
            for (ProductVariantResponse v : p.getVariants()) {
                Map<String, Object> variant = new HashMap<>();
                variant.put("id", v.getId());
                variant.put("sku", v.getSku());
                variant.put("size", v.getSize());
                variant.put("color", v.getColor());
                variant.put("price", v.getPrice() != null ? v.getPrice().doubleValue() : m.get("price"));
                variant.put("isDefault", Boolean.TRUE.equals(v.getIsDefault()));
                variant.put("inStock", Boolean.TRUE.equals(v.getInStock()));
                variant.put("stock", v.getQuantity() != null ? v.getQuantity() : 0);

                // Tạm thời: lấy ảnh DB của biến thể; không có thì dùng 1 ảnh Cloudinary tạm
                List<String> imageUrls = resolveVariantImageUrlsTemp(v, p.getCategoryId(), p.getId());
                variant.put("images", imageUrls);
                variant.put("image", imageUrls.isEmpty() ? null : imageUrls.get(0));
                variants.add(variant);

                for (String url : imageUrls) {
                    addUnique(gallery, url);
                }
            }
        }

        if (gallery.isEmpty() && m.get("image") != null) {
            gallery.add(String.valueOf(m.get("image")));
        }

        m.put("gallery", gallery);
        m.put("images", gallery);
        m.put("variants", variants);
        return m;
    }

    private static void addUnique(List<String> list, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        if (!list.contains(url)) {
            list.add(url);
        }
    }

    /** Tạm thời: ảnh DB của biến thể, hoặc 1 URL Cloudinary fallback. */
    private static List<String> resolveVariantImageUrlsTemp(
            ProductVariantResponse variant,
            String categoryId,
            Long productId) {
        List<String> urls = new ArrayList<>();
        if (variant != null && variant.getImages() != null) {
            for (ProductImageResponse img : variant.getImages()) {
                if (img != null && img.getImageUrl() != null && !img.getImageUrl().isBlank()) {
                    addUnique(urls, img.getImageUrl().trim());
                }
            }
        }
        if (!urls.isEmpty()) {
            return urls;
        }
        String fallback = fallbackImageUrl(categoryId, productId);
        return fallback != null ? List.of(fallback) : List.of();
    }

    /** URL Cloudinary tạm theo pattern: c001_1.jpg, c002_3.jpg, ... */
    private static String fallbackImageUrl(String categoryId, Long productId) {
        if (categoryId == null || categoryId.isBlank() || productId == null) {
            return null;
        }
        int seq = (int) ((productId - 1) % 10 + 1);
        return "https://res.cloudinary.com/pnam233/image/upload/product/"
                + categoryId.toLowerCase().trim() + "_" + seq + ".jpg";
    }
}
