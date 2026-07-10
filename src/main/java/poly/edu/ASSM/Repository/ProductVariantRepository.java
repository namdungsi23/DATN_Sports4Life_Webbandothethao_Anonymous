package poly.edu.ASSM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.Entity.ProductVariants;

public interface ProductVariantRepository extends JpaRepository<ProductVariants, Long> {

    @EntityGraph(attributePaths = { "productImages" })
    List<ProductVariants> findByProduct_IdOrderByDisplayOrderAscIdAsc(Long productId);

    @EntityGraph(attributePaths = { "product", "productImages" })
    Optional<ProductVariants> findDetailedById(Long id);

    Optional<ProductVariants> findByProduct_IdAndIsDefaultTrue(Long productId);

    long countByProduct_Id(Long productId);
}
