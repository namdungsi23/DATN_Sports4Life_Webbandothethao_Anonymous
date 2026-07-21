package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Products;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Entity.Wishlist;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.ProductRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.Repository.WishlistRepository;
import poly.edu.ASSM.dto.response.ProductImageResponse;
import poly.edu.ASSM.dto.response.ProductResponse;
import poly.edu.ASSM.dto.response.ProductVariantResponse;
import poly.edu.ASSM.mapper.ProductMapper;

@Service
public class WishlistServiceImpl implements WishlistService {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listWishlist(String username) {
        Users user = requireUser(username);
        List<Wishlist> rows = wishlistRepository.findAllByUserId(user.getId());

        List<Map<String, Object>> items = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();

        for (Wishlist row : rows) {
            if (row.getProduct() == null) {
                continue;
            }
            productIds.add(row.getProduct().getId());
            items.add(toWishlistItem(row));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("items", items);
        body.put("productIds", productIds);
        body.put("count", items.size());
        return body;
    }

    @Override
    @Transactional
    public Map<String, Object> addToWishlist(String username, Long productId) {
        Users user = requireUser(username);
        Products product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));

        if (wishlistRepository.existsByUser_IdAndProduct_Id(user.getId(), productId)) {
            return Map.of(
                    "message", "Sản phẩm đã có trong danh sách yêu thích",
                    "added", false,
                    "productId", productId);
        }

        Wishlist wishlist = new Wishlist();
        wishlist.setUser(user);
        wishlist.setProduct(product);
        wishlist.setCreatedAt(Instant.now());
        wishlistRepository.save(wishlist);

        return Map.of(
                "message", "Đã thêm vào yêu thích",
                "added", true,
                "productId", productId);
    }

    @Override
    @Transactional
    public Map<String, Object> removeFromWishlist(String username, Long productId) {
        Users user = requireUser(username);

        if (!wishlistRepository.existsByUser_IdAndProduct_Id(user.getId(), productId)) {
            return Map.of(
                    "message", "Sản phẩm không có trong danh sách yêu thích",
                    "removed", false,
                    "productId", productId);
        }

        wishlistRepository.deleteByUser_IdAndProduct_Id(user.getId(), productId);

        return Map.of(
                "message", "Đã xóa khỏi yêu thích",
                "removed", true,
                "productId", productId);
    }

    private Users requireUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }

        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        }

        return usersRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ người dùng"));
    }

    private Map<String, Object> toWishlistItem(Wishlist row) {
        ProductResponse product = productMapper.toResponse(row.getProduct());
        Map<String, Object> item = new HashMap<>();
        item.put("wishlistId", row.getId());
        item.put("createdAt", row.getCreatedAt());
        item.put("product", toFePayload(product));
        return item;
    }

    private static Map<String, Object> toFePayload(ProductResponse p) {
        Map<String, Object> m = new HashMap<>();
        if (p == null) {
            return m;
        }

        int totalQty = 0;
        if (p.getVariants() != null) {
            for (ProductVariantResponse v : p.getVariants()) {
                if (v.getQuantity() != null) {
                    totalQty += v.getQuantity();
                }
            }
        }

        String image = p.getImageUrl();
        if (image == null || image.isBlank()) {
            image = fallbackImageUrl(p.getCategoryId(), p.getId());
        }

        m.put("id", p.getId());
        m.put("name", p.getName());
        m.put("image", image);
        m.put("price", p.getMinPrice() != null ? p.getMinPrice().doubleValue() : 0);
        m.put("createDate", p.getCreatedAt());
        m.put("available", p.getStatus() != null ? p.getStatus() : true);
        m.put("description", p.getDescription());
        m.put("quantity", totalQty);
        m.put("inStock", Boolean.TRUE.equals(p.getInStock()));
        m.put("categoryName", p.getCategoryName());
        m.put("categoryId", p.getCategoryId());
        m.put("brand", p.getBrand());
        m.put("minPrice", p.getMinPrice());
        m.put("maxPrice", p.getMaxPrice());
        return m;
    }

    private static String fallbackImageUrl(String categoryId, Long productId) {
        if (categoryId == null || categoryId.isBlank() || productId == null) {
            return null;
        }
        int seq = (int) ((productId - 1) % 10 + 1);
        return "https://res.cloudinary.com/pnam233/image/upload/product/"
                + categoryId.toLowerCase() + "_" + seq + ".jpg";
    }
}
