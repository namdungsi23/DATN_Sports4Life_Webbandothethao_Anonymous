package poly.edu.ASSM.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Vouchers")
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "DiscountPercent")
    private Integer discountPercent;

    @Column(name = "DiscountAmount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "MinOrderValue", precision = 10, scale = 2)
    private BigDecimal minOrderValue;

    @Column(name = "MaxDiscount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "Quantity")
    private Integer quantity;

    @Column(name = "StartDate")
    private Instant startDate;

    @Column(name = "ExpiredAt")
    private Instant expiredAt;

    @Column(name = "IsActive", columnDefinition = "tinyint")
    private Short isActive;

    @Nationalized
    @Column(name = "Name", length = 255)
    private String name;

    @ColumnDefault("0")
    @Column(name = "UsedCount")
    private Integer usedCount;

    @OneToMany(mappedBy = "voucher")
    private Set<Orders> orders = new LinkedHashSet<>();

}
