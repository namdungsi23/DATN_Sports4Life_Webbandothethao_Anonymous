package poly.edu.ASSM.Services.core;

import java.util.Map;

public interface PublicProductService {

    Map<String, Object> getProductsPage(
            String cat,
            String keyword,
            Double min,
            Double max,
            int page,
            String sort,
            String dir);

    Map<String, Object> getProductDetail(Long id);

    Map<String, Object> getBrands();

    /** Gợi ý autocomplete khi gõ từ khóa (prefix / chứa). */
    Map<String, Object> suggestProducts(String keyword, int limit);
}
