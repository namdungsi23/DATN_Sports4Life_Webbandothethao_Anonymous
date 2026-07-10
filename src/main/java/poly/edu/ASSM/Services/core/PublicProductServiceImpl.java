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

        String trimmedKeyword = keyword != null ? keyword.trim() : "";
        boolean sortByPrice = isPriceSort(sort);
        String sortProp = sortByPrice ? "price" : resolveSortProperty(sort);

        List<Map<String, Object>> suggestions = new ArrayList<>();
        boolean exactMatch = false;

        // Nhập đúng tên sản phẩm → chỉ hiện SP đó + gợi ý cùng danh mục / thương hiệu
        if (!trimmedKeyword.isEmpty() && (cat == null || cat.isBlank()) && min == null && max == null) {
            List<Products> exactList = productRepository.findExactByNameIgnoreCase(
                    trimmedKeyword, PageRequest.of(0, 1));
            if (!exactList.isEmpty()) {
                exactMatch = true;
                Products exact = exactList.get(0);
                ProductResponse exactResponse = productMapper.toResponse(exact);
                List<Map<String, Object>> content = List.of(toFePayload(exactResponse));

                String categoryName = exactResponse.getCategoryName();
                String brand = resolveBrand(exactResponse.getBrand(), exactResponse.getName());
                List<Products> related = productRepository.findRelatedByCategoryOrBrand(
                        exact.getId(),
                        categoryName,
                        brand,
                        PageRequest.of(0, 8));
                for (Products relatedEntity : related) {
                    suggestions.add(toFePayload(productMapper.toResponse(relatedEntity)));
                }

                Map<String, Object> pageBody = new HashMap<>();
                pageBody.put("content", content);
                pageBody.put("totalPages", 1);
                pageBody.put("totalElements", 1);
                pageBody.put("number", 0);
                pageBody.put("size", 10);
                pageBody.put("first", true);
                pageBody.put("last", true);

                return buildProductsPageBody(
                        pageBody, categoriesPayload(), cat, trimmedKeyword, min, max, sortProp, dir, page,
                        exactMatch, suggestions);
            }
        }

        Pageable pageable = sortByPrice
                ? PageRequest.of(page, 10)
                : PageRequest.of(
                        page,
                        10,
                        Sort.by(
                                "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                                sortProp));

        String filterKeyword = trimmedKeyword.isEmpty() ? null : trimmedKeyword;
        Page<Products> productPage = sortByPrice
                ? ("desc".equalsIgnoreCase(dir)
                        ? productRepository.filterProductsOrderByDefaultPriceDesc(
                                cat, filterKeyword, min, max, pageable)
                        : productRepository.filterProductsOrderByDefaultPriceAsc(
                                cat, filterKeyword, min, max, pageable))
                : productRepository.filterProducts(cat, filterKeyword, min, max, pageable);

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

        return buildProductsPageBody(
                pageBody, categoriesPayload(), cat, trimmedKeyword, min, max, sortProp, dir, page,
                exactMatch, suggestions);
    }

    @Override
    public Map<String, Object> suggestProducts(String keyword, int limit) {
        String q = keyword != null ? keyword.trim() : "";
        if (q.isEmpty()) {
            return Map.of("suggestions", List.of());
        }

        int size = Math.min(Math.max(limit, 1), 20);
        List<Products> found = productRepository.suggestByKeyword(q, PageRequest.of(0, size));
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (Products entity : found) {
            suggestions.add(toFePayload(productMapper.toResponse(entity)));
        }
        return Map.of("suggestions", suggestions);
    }

    private List<Map<String, String>> categoriesPayload() {
        List<Map<String, String>> categories = new ArrayList<>();
        for (Category c : categoryService.findAll()) {
            categories.add(Map.of(
                    "id", c.getId() != null ? c.getId() : "",
                    "name", c.getName() != null ? c.getName() : ""));
        }
        return categories;
    }

    private static Map<String, Object> buildProductsPageBody(
            Map<String, Object> pageBody,
            List<Map<String, String>> categories,
            String cat,
            String keyword,
            Double min,
            Double max,
            String sortProp,
            String dir,
            int page,
            boolean exactMatch,
            List<Map<String, Object>> suggestions) {

        Map<String, Object> filters = new HashMap<>();
        filters.put("cat", cat != null ? cat : "");
        filters.put("keyword", keyword != null ? keyword : "");
        filters.put("min", min != null ? min : 0);
        filters.put("max", max != null ? max : 0);
        filters.put("sort", sortProp);
        filters.put("dir", dir != null ? dir : "asc");
        filters.put("page", page);

        Map<String, Object> body = new HashMap<>();
        body.put("products", pageBody);
        body.put("categories", categories);
        body.put("filters", filters);
        body.put("exactMatch", exactMatch);
        body.put("suggestions", suggestions != null ? suggestions : List.of());
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

    private static boolean isPriceSort(String sort) {
        return sort != null && "price".equalsIgnoreCase(sort.trim());
    }

    private static String resolveSortProperty(String sort) {
        if (sort == null || sort.isBlank()) {
            return "createdAt";
        }
        return switch (sort.trim().toLowerCase()) {
            case "id" -> "id";
            case "name" -> "name";
            case "available", "status" -> "status";
            case "createdate", "create_date" -> "createdAt";
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

        double listPrice = p.getDefaultPrice() != null
                ? p.getDefaultPrice().doubleValue()
                : (p.getMinPrice() != null ? p.getMinPrice().doubleValue() : 0);

        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("image", image);
        m.put("price", listPrice);
        m.put("createDate", p.getCreatedAt());
        m.put("available", p.getStatus() != null ? p.getStatus() : true);
        m.put("description", p.getDescription());
        m.put("quantity", totalQty);
        m.put("inStock", Boolean.TRUE.equals(p.getInStock()));
        m.put("categoryName", p.getCategoryName());
        m.put("categoryId", p.getCategoryId());
        m.put("brand", resolveBrand(p.getBrand(), p.getName()));
        m.put("defaultPrice", p.getDefaultPrice());
        m.put("minPrice", p.getMinPrice());
        m.put("maxPrice", p.getMaxPrice());
        return m;
    }

    private Map<String, Object> toFePayloadDetail(ProductResponse p) {
        Map<String, Object> m = toFePayload(p);

        List<String> gallery = new ArrayList<>();
        if (p.getImages() != null) {
            for (ProductImageResponse img : p.getImages()) {
                if (img.getImageUrl() != null && !img.getImageUrl().isBlank()) {
                    gallery.add(img.getImageUrl());
                }
            }
        }
        if (gallery.isEmpty() && m.get("image") != null) {
            gallery.add(String.valueOf(m.get("image")));
        }
        m.put("gallery", gallery);

        List<Map<String, Object>> variants = new ArrayList<>();
        if (p.getVariants() != null) {
            for (ProductVariantResponse v : p.getVariants()) {
                Map<String, Object> variant = new HashMap<>();
                variant.put("id", v.getId());
                variant.put("size", v.getSize());
                variant.put("color", v.getColor());
                variant.put("price", v.getPrice() != null ? v.getPrice().doubleValue() : m.get("price"));
                variant.put("inStock", Boolean.TRUE.equals(v.getInStock()));
                variants.add(variant);
            }
        }
        m.put("variants", variants);

        double price = m.get("price") instanceof Number n ? n.doubleValue() : 0;
        m.put("originalPrice", Math.round(price * 1.25));
        m.put("discountPercent", 20);
        return m;
    }

    /** URL Cloudinary theo pattern dữ liệu mẫu: c001_1.jpg, c002_3.jpg, ... */
    private static String fallbackImageUrl(String categoryId, Long productId) {
        if (categoryId == null || categoryId.isBlank() || productId == null) {
            return null;
        }
        int seq = (int) ((productId - 1) % 10 + 1);
        return "https://res.cloudinary.com/pnam233/image/upload/product/"
                + categoryId.toLowerCase() + "_" + seq + ".jpg";
    }
}
