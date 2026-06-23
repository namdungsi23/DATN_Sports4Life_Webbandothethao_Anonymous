package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Users")
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AccountId")
    private Accounts account;

    @Column(name = "Phone", length = 20)
    private String phone;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @ColumnDefault("0")
    @Column(name = "TotalPoint")
    private Integer totalPoint;

    @ColumnDefault("0")
    @Column(name = "TotalSpending", precision = 18, scale = 2)
    private BigDecimal totalSpending;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RankId")
    private Ranks ranks;
    @OneToMany
    @JoinColumn(name = "UserId")
    private Set<AuditLogs> auditLogs = new LinkedHashSet<>();
    @OneToMany
    @JoinColumn(name = "UserId")
    private Set<Carts> carts = new LinkedHashSet<>();
    @OneToMany(mappedBy = "users")
    private Set<Notification> notifications = new LinkedHashSet<>();
    @OneToMany(mappedBy = "users")
    private Set<Payments> payments = new LinkedHashSet<>();
    @OneToMany(mappedBy = "users")
    private Set<Reviews> reviews = new LinkedHashSet<>();
    @OneToMany
    @JoinColumn(name = "UserId")
    private Set<UserAddress> userAddresses = new LinkedHashSet<>();
    @ManyToMany
    @JoinTable(name = "UserRoles", joinColumns = {@JoinColumn(name = "user_id")}, inverseJoinColumns = {@JoinColumn(name = "role_id")})
    private Set<Roles> roles = new LinkedHashSet<>();
    @OneToMany
    @JoinColumn(name = "UserId")
    private Set<Wishlist> wishlists = new LinkedHashSet<>();


}