package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionRequest {

    private Integer id;

    @NotBlank(message = "Tên quyền không được để trống")
    @Size(max = 100, message = "Tên quyền tối đa 100 ký tự")
    private String permissionName;

    private String description;
}
