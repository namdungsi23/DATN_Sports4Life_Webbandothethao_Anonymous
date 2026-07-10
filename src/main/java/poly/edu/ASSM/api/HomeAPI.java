package poly.edu.ASSM.api;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.PublicProductService;

@RestController
@RequestMapping("/api/public")
public class HomeAPI {

    @Autowired
    private PublicProductService publicProductService;

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("message", "API is running", "status", "ok");
    }

    @GetMapping("/products")
    public Map<String, Object> products(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {

        return publicProductService.getProductsPage(cat, keyword, min, max, page, sort, dir);
    }

    @GetMapping("/products/suggest")
    public Map<String, Object> productSuggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "8") int limit) {
        return publicProductService.suggestProducts(q, limit);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> productDetail(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return publicProductService.getProductDetail(id);
    }

    @GetMapping("/brands")
    public Map<String, Object> brands() {
        return publicProductService.getBrands();
    }
}
