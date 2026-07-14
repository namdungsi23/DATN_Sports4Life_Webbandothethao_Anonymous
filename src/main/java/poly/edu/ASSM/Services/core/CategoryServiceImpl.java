package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.entity.Category;
import poly.edu.ASSM.repository.CategoryRepository;

@Service
public class CategoryServiceImpl implements CategoryService {

	@Autowired
	private CategoryRepository repo;

	@Override
	public List<Category> findAll() {
		return repo.findAll();
	}

	@Override
	public Category findById(String id) {
		return repo.findById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));
	}

	@Override
	public Category create(Category category) {

		// ❌ Admin không được set ID
		category.setId(generateCategoryId());

		if (repo.existsByName(category.getName())) {
			throw new RuntimeException("Tên danh mục đã tồn tại");
		}

		return repo.save(category);
	}

	@Override
	public Category update(Category category) {

		Category old = findById(category.getId());

		// ❌ Không cho sửa ID
		old.setName(category.getName());

		return repo.save(old);
	}

	@Override
	public void delete(String id) {

		if (!canDelete(id)) {
			throw new RuntimeException("Danh mục đang chứa sản phẩm");
		}

		repo.deleteById(id);
	}

	@Override
	public boolean existsByName(String name) {
		return repo.existsByName(name);
	}

	@Override
	public boolean canDelete(String id) {
		return repo.countProductsByCategoryId(id) == 0;
	}

	@Override
	public Page<Category> search(String keyword, Pageable pageable) {
		return repo.findByNameContainingIgnoreCase(keyword, pageable);
	}

	@Override
	public long countProductsByCategory(String categoryId) {
		return repo.countProductsByCategoryId(categoryId);
	}

	// 🔥 TỰ SINH ID
	private String generateCategoryId() {
		long count = repo.count() + 1;
		return "CAT" + String.format("%03d", count);
	}

	@Override
	public Page<Category> filterCategories(String keyword, Pageable pageable) {
		return repo.filterCategories(keyword, pageable);
	}
}
