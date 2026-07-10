package poly.edu.ASSM.Repository;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Products;

@Repository
public interface ProductRepository extends JpaRepository<Products, Long> {

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    Page<Products> findByNameContainingIgnoreCase(String name, Pageable pageable);

    String PUBLIC_PRODUCT_FILTER = """
            (COALESCE(p.status, true) = true)
            AND ((:cat IS NULL OR :cat = '') OR (p.category IS NOT NULL AND p.category.name = :cat))
            AND ((:keyword IS NULL OR :keyword = '')
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:min IS NULL OR EXISTS (
                    SELECT 1 FROM ProductVariants v
                    WHERE v.product = p AND v.price >= :min AND COALESCE(v.status, true) = true))
            AND (:max IS NULL OR EXISTS (
                    SELECT 1 FROM ProductVariants v
                    WHERE v.product = p AND v.price <= :max AND COALESCE(v.status, true) = true))
            """;

    /** Giá dùng để sort public: biến thể mặc định, fallback MIN(price) nếu chưa có default. */
    String PUBLIC_DEFAULT_PRICE_ORDER = """
            (
                SELECT COALESCE(
                    (SELECT MIN(v.price) FROM ProductVariants v
                     WHERE v.product = p AND v.isDefault = true AND COALESCE(v.status, true) = true),
                    (SELECT MIN(v2.price) FROM ProductVariants v2
                     WHERE v2.product = p AND COALESCE(v2.status, true) = true)
                )
            )
            """;

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query("""
            SELECT p FROM Products p
            WHERE """ + PUBLIC_PRODUCT_FILTER)
    Page<Products> filterProducts(
            @Param("cat") String cat,
            @Param("keyword") String keyword,
            @Param("min") Double min,
            @Param("max") Double max,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + PUBLIC_PRODUCT_FILTER + """
                    ORDER BY """ + PUBLIC_DEFAULT_PRICE_ORDER + """ 
                    ASC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + PUBLIC_PRODUCT_FILTER)
    Page<Products> filterProductsOrderByDefaultPriceAsc(
            @Param("cat") String cat,
            @Param("keyword") String keyword,
            @Param("min") Double min,
            @Param("max") Double max,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + PUBLIC_PRODUCT_FILTER + """
                    ORDER BY """ + PUBLIC_DEFAULT_PRICE_ORDER + """ 
                    DESC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + PUBLIC_PRODUCT_FILTER)
    Page<Products> filterProductsOrderByDefaultPriceDesc(
            @Param("cat") String cat,
            @Param("keyword") String keyword,
            @Param("min") Double min,
            @Param("max") Double max,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    Optional<Products> findDetailedById(Long id);

    /** Gợi ý tìm kiếm: ưu tiên tên/thương hiệu bắt đầu bằng từ khóa, sau đó chứa từ khóa. */
    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query("""
            SELECT p FROM Products p
            WHERE COALESCE(p.status, true) = true
            AND (
                LOWER(p.name) LIKE LOWER(CONCAT(:keyword, '%'))
                OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT(:keyword, '%'))
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY
                CASE
                    WHEN LOWER(p.name) LIKE LOWER(CONCAT(:keyword, '%')) THEN 0
                    WHEN LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT(:keyword, '%')) THEN 1
                    WHEN LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) THEN 2
                    ELSE 3
                END,
                p.name ASC
            """)
    List<Products> suggestByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query("""
            SELECT p FROM Products p
            WHERE COALESCE(p.status, true) = true
            AND LOWER(TRIM(p.name)) = LOWER(TRIM(:name))
            """)
    List<Products> findExactByNameIgnoreCase(@Param("name") String name, Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query("""
            SELECT p FROM Products p
            WHERE COALESCE(p.status, true) = true
            AND p.id <> :excludeId
            AND (
                (:categoryName IS NOT NULL AND :categoryName <> ''
                    AND p.category IS NOT NULL AND p.category.name = :categoryName)
                OR (:brand IS NOT NULL AND :brand <> ''
                    AND LOWER(TRIM(COALESCE(p.brand, ''))) = LOWER(TRIM(:brand)))
            )
            ORDER BY p.createdAt DESC
            """)
    List<Products> findRelatedByCategoryOrBrand(
            @Param("excludeId") Long excludeId,
            @Param("categoryName") String categoryName,
            @Param("brand") String brand,
            Pageable pageable);

    @Query("SELECT COUNT(p) FROM Products p WHERE p.createdAt >= :date")
    long countNewProducts(@Param("date") Instant date);

    @Query("""
            SELECT p.brand, COUNT(p)
            FROM Products p
            WHERE p.brand IS NOT NULL
            AND TRIM(p.brand) <> ''
            AND COALESCE(p.status, true) = true
            GROUP BY p.brand
            ORDER BY p.brand
            """)
    List<Object[]> findBrandSummaries();

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.productImages" })
    @Query("""
            SELECT p FROM Products p
            WHERE LOWER(TRIM(p.brand)) = LOWER(TRIM(:brand))
            AND COALESCE(p.status, true) = true
            ORDER BY p.createdAt DESC
            """)
    Page<Products> findSampleByBrand(@Param("brand") String brand, Pageable pageable);

    @EntityGraph(attributePaths = { "category" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE ((:categoryId IS NULL OR :categoryId = '') OR (p.category IS NOT NULL AND p.category.id = :categoryId))
                    AND ((:keyword IS NULL OR :keyword = '')
                            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE ((:categoryId IS NULL OR :categoryId = '') OR (p.category IS NOT NULL AND p.category.id = :categoryId))
                    AND ((:keyword IS NULL OR :keyword = '')
                            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
                    """)
    Page<Products> findAdminProducts(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    String ADMIN_PRODUCT_FILTER = """
            ((:categoryId IS NULL OR :categoryId = '') OR (p.category IS NOT NULL AND p.category.id = :categoryId))
            AND ((:keyword IS NULL OR :keyword = '')
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(p.brand, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """;

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """
                    + ADMIN_PRODUCT_FILTER +
                    """
                    ORDER BY (
                        SELECT MIN(v.price) FROM ProductVariants v
                        WHERE v.product = p AND COALESCE(v.status, true) = true
                    ) ASC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByMinPriceAsc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT MIN(v.price) FROM ProductVariants v
                        WHERE v.product = p AND COALESCE(v.status, true) = true
                    ) DESC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByMinPriceDesc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT COALESCE(SUM(v.quantity), 0) FROM ProductVariants v
                        WHERE v.product = p
                    ) ASC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByTotalQuantityAsc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT COALESCE(SUM(v.quantity), 0) FROM ProductVariants v
                        WHERE v.product = p
                    ) DESC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByTotalQuantityDesc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT MIN(img.imageUrl) FROM ProductVariants v
                        JOIN v.productImages img
                        WHERE v.product = p
                    ) ASC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByImageAsc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT MIN(img.imageUrl) FROM ProductVariants v
                        JOIN v.productImages img
                        WHERE v.product = p
                    ) DESC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByImageDesc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT CASE WHEN MAX(COALESCE(v.quantity, 0)) > 0 THEN 1 ELSE 0 END
                        FROM ProductVariants v
                        WHERE v.product = p
                    ) ASC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByStockStatusAsc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);

    @EntityGraph(attributePaths = { "category", "productVariants", "productVariants.productImages" })
    @Query(
            value = """
                    SELECT p FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER + """
                    ORDER BY (
                        SELECT CASE WHEN MAX(COALESCE(v.quantity, 0)) > 0 THEN 1 ELSE 0 END
                        FROM ProductVariants v
                        WHERE v.product = p
                    ) DESC
                    """,
            countQuery = """
                    SELECT count(p) FROM Products p
                    WHERE """ + ADMIN_PRODUCT_FILTER)
    Page<Products> findAdminProductsOrderByStockStatusDesc(
            @Param("categoryId") String categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
