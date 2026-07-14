package poly.edu.ASSM.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import poly.edu.ASSM.entity.ProductVariants;

public interface ProductVariantRepository extends JpaRepository<ProductVariants, Long> {

    @EntityGraph(attributePaths = { "productImages" })
    List<ProductVariants> findByProduct_IdOrderByDisplayOrderAscIdAsc(Long productId);

    @EntityGraph(attributePaths = { "product", "productImages" })
    Optional<ProductVariants> findDetailedById(Long id);

    Optional<ProductVariants> findByProduct_IdAndIsDefaultTrue(Long productId);

    long countByProduct_Id(Long productId);

    /**
     * Trừ tồn atomic — chỉ thành công khi còn đủ hàng (chặn race nhiều người mua cùng lúc).
     * @return số dòng cập nhật (0 = không đủ tồn)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProductVariants v
            SET v.quantity = v.quantity - :qty,
                v.soldCount = COALESCE(v.soldCount, 0) + :qty
            WHERE v.id = :variantId
              AND v.quantity IS NOT NULL
              AND v.quantity >= :qty
            """)
    int tryDeductStock(@Param("variantId") Long variantId, @Param("qty") int qty);

    /**
     * Biến thể không theo dõi tồn (quantity NULL) — chỉ tăng soldCount.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProductVariants v
            SET v.soldCount = COALESCE(v.soldCount, 0) + :qty
            WHERE v.id = :variantId
              AND v.quantity IS NULL
            """)
    int incrementSoldWhenUnlimited(@Param("variantId") Long variantId, @Param("qty") int qty);

    /** Hoàn tồn khi hủy đơn (atomic). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ProductVariants v
            SET v.quantity = CASE
                    WHEN v.quantity IS NULL THEN NULL
                    ELSE v.quantity + :qty
                END,
                v.soldCount = CASE
                    WHEN COALESCE(v.soldCount, 0) < :qty THEN 0
                    ELSE COALESCE(v.soldCount, 0) - :qty
                END
            WHERE v.id = :variantId
            """)
    int restock(@Param("variantId") Long variantId, @Param("qty") int qty);
}
