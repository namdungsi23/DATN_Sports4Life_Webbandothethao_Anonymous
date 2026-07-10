package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String brand;
    private String categoryId;
    private String categoryName;
    private Boolean status;
    private Instant createdAt;
    private Instant updatedAt;
    private String imageUrl;
    /** Giá biến thể mặc định (fallback biến thể đầu tiên). */
    private BigDecimal defaultPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Boolean inStock;

    @Builder.Default
    private List<ProductVariantResponse> variants = new ArrayList<>();

    @Builder.Default
    private List<ProductImageResponse> images = new ArrayList<>();
}
