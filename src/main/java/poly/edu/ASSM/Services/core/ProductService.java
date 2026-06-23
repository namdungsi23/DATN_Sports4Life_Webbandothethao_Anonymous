package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import poly.edu.ASSM.Entity.Products;

public interface ProductService {
    List<Products> findAll();

    Products findById(Long id);

    Products create(Products product);

    Products update(Products product);

    void delete(Long id);

    Page<Products> findAll(int page, int size, String sortBy, String sortDir, String keyword);

    Page<Products> filterProducts(String cat, String keyword, Double min, Double max, Pageable pageable);

    Products create(Products formProduct, int quantity);
}
