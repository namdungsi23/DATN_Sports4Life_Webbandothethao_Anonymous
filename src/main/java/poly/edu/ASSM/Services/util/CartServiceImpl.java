package poly.edu.ASSM.Services.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.ProductVariants;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Services.core.InventoryService;
import poly.edu.ASSM.Services.core.ProductService;
import poly.edu.ASSM.Services.web.SessionService;
import poly.edu.ASSM.domain.CartItem;
import poly.edu.ASSM.exception.OutOfStockException;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    ProductService productService;

    @Autowired
    SessionService sessionService;

    @Autowired
    InventoryService inventoryService;

    static final String CART_KEY = "cart";

    Map<Integer, CartItem> getCart() {
        Map<Integer, CartItem> cart = sessionService.getAttribute(CART_KEY);
        if (cart == null) {
            cart = new HashMap<>();
            sessionService.setAttribute(CART_KEY, cart);
        }
        return cart;
    }

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
    public void add(Integer productId) {
        Map<Integer, CartItem> cart = getCart();
        CartItem item = cart.get(productId);

        if (item == null) {
            Products product = productService.findById(productId.longValue());
            if (product == null) {
                return;
            }
            item = new CartItem();
            item.setProductId(productId);
            item.setName(product.getName());
            item.setPrice(resolvePrice(product));
            item.setQuantity(1);
            cart.put(productId, item);
        } else {
            item.setQuantity(item.getQuantity() + 1);
        }
    }

    @Override
    public void remove(Integer productId) {
        getCart().remove(productId);
    }

    @Override
    public void update(Integer productId, int quantity) {
        CartItem item = getCart().get(productId);
        if (item == null) {
            return;
        }
        try {
            inventoryService.checkInventory(productId, quantity);
            item.setQuantity(quantity);
        } catch (OutOfStockException e) {
            throw e;
        }
    }

    @Override
    public void clear() {
        getCart().clear();
    }

    @Override
    public Collection<CartItem> getItems() {
        return getCart().values();
    }

    @Override
    public int getCount() {
        return getCart().values().stream().mapToInt(CartItem::getQuantity).sum();
    }

    @Override
    public double getAmount() {
        return getCart().values().stream().mapToDouble(p -> p.getPrice() * p.getQuantity()).sum();
    }
}
