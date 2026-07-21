package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.WishlistService;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistApiController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public Map<String, Object> list(Principal principal) {
        return wishlistService.listWishlist(principal.getName());
    }

    @PostMapping("/{productId}")
    public Map<String, Object> add(Principal principal, @PathVariable Long productId) {
        return wishlistService.addToWishlist(principal.getName(), productId);
    }

    @DeleteMapping("/{productId}")
    public Map<String, Object> remove(Principal principal, @PathVariable Long productId) {
        return wishlistService.removeFromWishlist(principal.getName(), productId);
    }
}
