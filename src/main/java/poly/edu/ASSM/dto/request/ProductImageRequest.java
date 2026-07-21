package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductImageRequest {

    private Long id;

    @NotNull(message = "ProductId không được để trống")
    private Long productId;

    @NotBlank(message = "ImageUrl không được để trống")
    @Size(max = 500, message = "ImageUrl tối đa 500 ký tự")
    private String imageUrl;

    private Boolean isDefault;
    private Integer sortOrder;
}
