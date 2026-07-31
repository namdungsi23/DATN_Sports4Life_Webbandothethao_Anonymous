package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Permissions")
public class Permissions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @Column(name = "PermissionName", nullable = false, length = 100)
    private String permissionName;

    @Nationalized
    @Column(name = "Description")
    private String description;
    @ManyToMany
    @JoinTable(name = "RolePermissions", joinColumns = {@JoinColumn(name = "PermissionId")}, inverseJoinColumns = {@JoinColumn(name = "RoleId")})
    private Set<Roles> roles = new LinkedHashSet<>();


}