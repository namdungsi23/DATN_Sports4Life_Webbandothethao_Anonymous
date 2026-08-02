package poly.edu.ASSM.dto.response;

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
public class CustomerOrderItemResponse {
    private Integer id;
    private Integer quantity;
    private Double price;
    private Double lineTotal;
    private Long variantId;
    private String size;
    private String color;
    private String sku;
    private Long productId;
    private String productName;
}
