package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RolePermissionRequest {

    @NotNull(message = "RoleId không được để trống")
    private Integer roleId;

    @NotNull(message = "PermissionId không được để trống")
    private Integer permissionId;
}
