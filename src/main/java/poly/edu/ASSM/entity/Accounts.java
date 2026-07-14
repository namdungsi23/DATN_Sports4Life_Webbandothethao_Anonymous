package poly.edu.ASSM.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Accounts")
public class Accounts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Column(name = "Username", nullable = false, length = 30)
    private String username;

    @Column(name = "PasswordHash", length = 255)
    private String passwordHash;

    @Column(name = "Email", length = 255)
    private String email;

    @ColumnDefault("1")
    @Column(name = "IsActive", nullable = false)
    private Boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RoleId", nullable = false)
    private Roles role;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

    @OneToOne(mappedBy = "account")
    private Users users;

    @OneToOne(mappedBy = "account")
    private Employees employee;

    @OneToMany(mappedBy = "account")
    private Set<Addresses> addresses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "account")
    private Set<Orders> orders = new LinkedHashSet<>();

}
