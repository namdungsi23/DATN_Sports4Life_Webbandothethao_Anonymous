package poly.edu.ASSM.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

	@Query("""
			SELECT c FROM Comment c
			JOIN FETCH c.users u
			LEFT JOIN FETCH u.rank
			LEFT JOIN FETCH u.account
			WHERE c.product.id = :productId
			  AND (c.status IS NULL OR c.status = true)
			ORDER BY c.createdAt DESC
			""")
	List<Comment> findVisibleByProductId(@Param("productId") Long productId);

	Optional<Comment> findByProduct_IdAndUsers_Id(Long productId, Long userId);

	@Query("""
			SELECT c FROM Comment c
			JOIN FETCH c.users u
			LEFT JOIN FETCH u.rank
			LEFT JOIN FETCH u.account
			JOIN FETCH c.product p
			WHERE (:keyword IS NULL OR :keyword = ''
			    OR LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
			    OR LOWER(COALESCE(u.fullName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			    OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
			  AND (:visible IS NULL
			    OR (:visible = true AND (c.status IS NULL OR c.status = true))
			    OR (:visible = false AND c.status = false))
			ORDER BY c.createdAt DESC
			""")
	List<Comment> adminSearch(@Param("keyword") String keyword, @Param("visible") Boolean visible);

	@Query("""
			SELECT COALESCE(AVG(CAST(c.rating AS double)), 0)
			FROM Comment c
			WHERE c.product.id = :productId
			  AND (c.status IS NULL OR c.status = true)
			""")
	Double avgRating(@Param("productId") Long productId);

	@Query("""
			SELECT COUNT(c)
			FROM Comment c
			WHERE c.product.id = :productId
			  AND (c.status IS NULL OR c.status = true)
			""")
	long countVisible(@Param("productId") Long productId);
}
