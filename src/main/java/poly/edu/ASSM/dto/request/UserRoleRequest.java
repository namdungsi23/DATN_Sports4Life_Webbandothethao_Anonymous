package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRoleRequest {

    @NotNull(message = "UserId không được để trống")
    private Integer userId;

    @NotNull(message = "RoleId không được để trống")
    private Integer roleId;
}
