package poly.edu.ASSM.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@Embeddable
public class RolePermissionId implements Serializable {
    private static final long serialVersionUID = -2552252810897497768L;
    @Column(name = "RoleId", nullable = false)
    private Integer roleId;

    @Column(name = "PermissionId", nullable = false)
    private Integer permissionId;


}