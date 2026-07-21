package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

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
    @Column(name = "AccountId", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountId")
    private Accounts account;

    @Nationalized
    @Column(name = "FullName", length = 100)
    private String fullName;

    @Column(name = "Avatar", length = 500)
    private String avatar;

    @Column(name = "Phone", length = 20)
    private String phone;

    @ColumnDefault("0")
    @Column(name = "Gender", nullable = false)
    private Integer gender;

    @Column(name = "DateOfBirth")
    private LocalDate dateOfBirth;

    @ColumnDefault("0")
    @Column(name = "TotalPoint", nullable = false)
    private Integer totalPoint;

    @ColumnDefault("0")
    @Column(name = "TotalSpending", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalSpending;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "RankId", nullable = false)
    private Ranks rank;

    @OneToMany
    @JoinColumn(name = "UserId")
    private Set<Carts> carts = new LinkedHashSet<>();

    @OneToMany(mappedBy = "users")
    private Set<Notification> notifications = new LinkedHashSet<>();

    @OneToMany(mappedBy = "users")
    private Set<Payments> payments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "users")
    private Set<Comment> comments = new LinkedHashSet<>();

    @OneToMany
    @JoinColumn(name = "UserId")
    private Set<Wishlist> wishlists = new LinkedHashSet<>();

    @PrePersist
    void applyDefaults() {
        if (gender == null) {
            gender = 0;
        }
        if (totalPoint == null) {
            totalPoint = 0;
        }
        if (totalSpending == null) {
            totalSpending = BigDecimal.ZERO;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

}
