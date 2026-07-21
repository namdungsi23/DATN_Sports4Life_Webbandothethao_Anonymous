package poly.edu.ASSM.Services.core;

import java.util.List;
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

    List<Map<String, String>> getCategories();

    Map<String, Object> getHomeData();
}
