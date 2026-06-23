package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

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

    @Column(name = "PasswordHash", length = 100)
    private String passwordHash;

    @Nationalized
    @Column(name = "FullName", length = 50)
    private String fullName;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "Avatar", length = 100)
    private String avatar;

    @Column(name = "IsActive")
    private Boolean isActive;

    @Column(name = "Admin")
    private Boolean admin;

    @ColumnDefault("0")
    @Column(name = "SuperAdmin")
    private Boolean superAdmin;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

    @OneToOne(mappedBy = "account")
    private Users users;


}