package poly.edu.ASSM.Services.core;

import java.util.Map;

public interface WishlistService {

    Map<String, Object> listWishlist(String username);

    Map<String, Object> addToWishlist(String username, Long productId);

    Map<String, Object> removeFromWishlist(String username, Long productId);
}
