package poly.edu.ASSM.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Wishlist;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @EntityGraph(attributePaths = {
            "product",
            "product.category",
            "product.productVariants",
            "product.productVariants.productImages" })
    @Query("SELECT w FROM Wishlist w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Wishlist> findAllByUserId(@Param("userId") Long userId);

    Optional<Wishlist> findByUser_IdAndProduct_Id(Long userId, Long productId);

    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);

    void deleteByUser_IdAndProduct_Id(Long userId, Long productId);
}
