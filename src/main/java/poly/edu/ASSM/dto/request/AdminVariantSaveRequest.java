package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminVariantSaveRequest {
    private Long id;
    private Long productId;
    private String sku;
    private String size;
    private String color;
    private BigDecimal price;
    private Integer quantity;
    private Boolean isDefault;
    private Boolean status;
}
