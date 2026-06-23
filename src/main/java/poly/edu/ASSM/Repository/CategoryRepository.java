package poly.edu.ASSM.Repository;



import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {

    boolean existsByName(String name);

    @Query("SELECT COUNT(p) FROM Products p WHERE p.category.id = :id")
    long countProductsByCategoryId(@Param("id") String id);

    Page<Category> findByNameContainingIgnoreCase(String keyword, Pageable pageable);
    
    @Query("""
    		SELECT c 
    		FROM Category c
    		WHERE :keyword IS NULL OR :keyword = '' OR c.id = :keyword OR c.name LIKE %:keyword%
    		""")
    Page<Category> filterCategories(
    								@Param("keyword") String keyword,
    								Pageable pageable);

  
}

