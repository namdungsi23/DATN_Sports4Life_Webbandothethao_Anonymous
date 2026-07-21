package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
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
public class ProductVariantResponse {
    private Long id;
    private Long productId;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Boolean isDefault;
    private Integer displayOrder;
    private Boolean status;
    private Integer quantity;
    private Boolean inStock;
    private Instant createdAt;
    private Instant updatedAt;
    private List<ProductImageResponse> images;
}
