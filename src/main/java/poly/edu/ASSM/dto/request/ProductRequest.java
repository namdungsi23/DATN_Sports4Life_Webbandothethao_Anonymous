package poly.edu.ASSM.dto.request;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    private Long id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 510, message = "Tên sản phẩm tối đa 510 ký tự")
    private String name;

    private String description;

    @Size(max = 200, message = "Thương hiệu tối đa 200 ký tự")
    private String brand;

    @Size(max = 4, message = "Mã danh mục tối đa 4 ký tự")
    private String categoryId;

    private Boolean status;

    @Valid
    private List<ProductVariantRequest> variants = new ArrayList<>();
}
