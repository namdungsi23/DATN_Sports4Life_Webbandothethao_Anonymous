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
public class RolePermissionResponse {
    private Integer roleId;
    private String roleName;
    private Integer permissionId;
    private String permissionName;
}
