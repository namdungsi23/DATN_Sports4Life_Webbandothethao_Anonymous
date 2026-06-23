package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductVariantRequest {

    private Long id;

    @NotNull(message = "ProductId không được để trống")
    private Long productId;

    @NotBlank(message = "SKU không được để trống")
    @Size(max = 100, message = "SKU tối đa 100 ký tự")
    private String sku;

    @Size(max = 50, message = "Size tối đa 50 ký tự")
    private String size;

    @Size(max = 50, message = "Color tối đa 50 ký tự")
    private String color;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    private Boolean isDefault;
    private Integer displayOrder;
    private Boolean status;
}
