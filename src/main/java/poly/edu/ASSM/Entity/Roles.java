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
@Table(name = "Roles")
public class Roles {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Nationalized
    @Column(name = "Description")
    private String description;
    @ManyToMany(mappedBy = "roles")
    private Set<Permissions> permissions = new LinkedHashSet<>();
    @ManyToMany(mappedBy = "roles")
    private Set<Users> users = new LinkedHashSet<>();


}