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
            "productVariants.inventory",
            "productVariants.productImages" })
    Page<Products> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.inventory",
            "productVariants.productImages" })
    @Query("""
            SELECT p FROM Products p
            WHERE (COALESCE(p.status, true) = true)
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
            """)
    Page<Products> filterProducts(
            @Param("cat") String cat,
            @Param("keyword") String keyword,
            @Param("min") Double min,
            @Param("max") Double max,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "category",
            "productVariants",
            "productVariants.inventory",
            "productVariants.productImages" })
    Optional<Products> findDetailedById(Long id);

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
}
