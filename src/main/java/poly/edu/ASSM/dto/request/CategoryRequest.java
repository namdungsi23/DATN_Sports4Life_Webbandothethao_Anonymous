package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    /** Có khi cập nhật; tạo mới thì service tự sinh (CAT001…). */
    @Size(max = 10, message = "Mã danh mục tối đa 10 ký tự")
    private String id;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 50, message = "Tên danh mục tối đa 50 ký tự")
    private String name;
}
