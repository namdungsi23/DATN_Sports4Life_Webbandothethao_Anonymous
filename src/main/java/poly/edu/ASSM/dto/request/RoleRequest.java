package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleRequest {

    private Integer id;

    @NotBlank(message = "Tên role không được để trống")
    @Size(max = 50, message = "Tên role tối đa 50 ký tự")
    private String name;

    private String description;
}
