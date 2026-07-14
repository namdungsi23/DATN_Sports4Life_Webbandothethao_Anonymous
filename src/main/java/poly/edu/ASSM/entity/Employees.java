package poly.edu.ASSM.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "Employees")
public class Employees {
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PositionId", nullable = false)
    private Positions position;

    @ColumnDefault("0")
    @Column(name = "Salary", nullable = false, precision = 18, scale = 2)
    private BigDecimal salary;

    @Column(name = "HireDate")
    private LocalDate hireDate;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

}
