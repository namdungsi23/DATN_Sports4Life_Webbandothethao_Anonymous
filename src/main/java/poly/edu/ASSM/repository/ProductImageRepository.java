package poly.edu.ASSM.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.entity.ProductImages;

public interface ProductImageRepository extends JpaRepository<ProductImages, Long> {

    List<ProductImages> findByVariant_IdOrderBySortOrderAscIdAsc(Long variantId);

    long countByVariant_Id(Long variantId);

    long countByImageUrl(String imageUrl);
}
