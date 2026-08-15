package poly.edu.ASSM.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.Entity.ProductImages;

public interface ProductImageRepository extends JpaRepository<ProductImages, Long> {

    List<ProductImages> findByVariant_IdOrderBySortOrderAscIdAsc(Long variantId);

    int countByVariant_Id(Long variantId);
}
