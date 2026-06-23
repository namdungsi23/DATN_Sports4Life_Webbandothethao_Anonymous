package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Repository.ProductRepository;
import poly.edu.ASSM.exception.InvalidInputException;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    ProductRepository repo;

    @Override
    public List<Products> findAll() {
        return repo.findAll();
    }

    @Override
    public Products findById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public Products create(Products formProduct, int quantity) {
        if (quantity <= 0) {
            throw new InvalidInputException("Số lượng phải lớn hơn 0!");
        }
        if (formProduct.getCreatedAt() == null) {
            formProduct.setCreatedAt(Instant.now());
        }
        formProduct.setUpdatedAt(Instant.now());
        return repo.save(formProduct);
    }

    @Override
    public Products update(Products product) {
        product.setUpdatedAt(Instant.now());
        return repo.save(product);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    public Page<Products> findAll(int page, int size, String sortBy, String sortDir, String keyword) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        if (keyword != null && !keyword.trim().isEmpty()) {
            return repo.findByNameContainingIgnoreCase(keyword, pageable);
        }
        return repo.findAll(pageable);
    }

    @Override
    public Page<Products> filterProducts(String cat, String keyword, Double min, Double max, Pageable pageable) {
        return repo.filterProducts(cat, keyword, min, max, pageable);
    }

    @Override
    public Products create(Products product) {
        return repo.save(product);
    }
}
