package poly.edu.ASSM.Services.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Services.core.ProductServiceImpl;
import poly.edu.ASSM.domain.CartItem;

@Service
@SessionScope
public class ShoppingCartServiceImpl implements ShoppingCartService {
    private final Map<Integer, CartItem> map = new HashMap<>();

    @Autowired
    ProductServiceImpl productService;

    private static double resolvePrice(Products product) {
        if (product == null || product.getProductVariants() == null) {
            return 0;
        }
        return product.getProductVariants().stream()
                .map(ProductVariants::getPrice)
                .filter(p -> p != null)
                .map(BigDecimal::doubleValue)
                .min(Double::compare)
                .orElse(0d);
    }

    @Override
    public CartItem add(Integer id) {
        CartItem item = map.get(id);
        if (item == null) {
            Products dbProduct = productService.findById(id.longValue());
            if (dbProduct == null) {
                throw new IllegalArgumentException("Không tìm thấy sản phẩm");
            }
            item = new CartItem();
            item.setProductId(id);
            item.setName(dbProduct.getName());
            item.setPrice(resolvePrice(dbProduct));
            item.setQuantity(1);
            map.put(id, item);
            return item;
        }

        item.setQuantity(item.getQuantity() + 1);
        return item;
    }

    @Override
    public void remove(Integer id) {
        map.remove(id);
    }

    @Override
    public CartItem update(Integer id, int qty) {
        if (qty <= 0) {
            map.remove(id);
            return null;
        }

        CartItem item = map.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm");
        }

        item.setQuantity(qty);
        return item;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Collection<CartItem> getItems() {
        return Collections.unmodifiableCollection(map.values());
    }

    @Override
    public int getCount() {
        int count = 0;
        for (CartItem item : map.values()) {
            count += item.getQuantity();
        }
        return count;
    }

    @Override
    public double getAmount() {
        double amount = 0;
        for (CartItem item : map.values()) {
            amount += item.getQuantity() * item.getPrice();
        }
        return amount;
    }
}
