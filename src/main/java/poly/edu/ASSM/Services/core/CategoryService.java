package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import poly.edu.ASSM.entity.Category;

public interface CategoryService {

    List<Category> findAll();

    Category findById(String id);

    Category create(Category category);

    Category update(Category category);

    void delete(String id);

    Page<Category> search(String keyword, Pageable pageable);

    Page<Category> filterCategories(String keyword, Pageable pageable);

    boolean canDelete(String id);

    long countProductsByCategory(String categoryId);

    boolean existsByName(String name);
}
