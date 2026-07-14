package poly.edu.ASSM.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Orders")
public class Orders {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountId", nullable = false)
    private Accounts account;

    @ColumnDefault("'PENDING'")
    @Column(name = "OrderStatus", nullable = false, length = 50)
    private String orderStatus;

    @ColumnDefault("'UNPAID'")
    @Column(name = "PaymentStatus", nullable = false, length = 50)
    private String paymentStatus;

    @Column(name = "SubTotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subTotal;

    @ColumnDefault("0")
    @Column(name = "DiscountAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "TotalAmount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VoucherId")
    private Voucher voucher;

    @ColumnDefault("getdate()")
    @Column(name = "CreateDate", nullable = false)
    private Instant createDate;

    @Column(name = "UpdateDate")
    private Instant updateDate;

    @OneToMany(mappedBy = "orders")
    private Set<OrderDetails> orderDetails = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order")
    private Set<Payments> payments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "order")
    private Set<OrderAddresses> orderAddresses = new LinkedHashSet<>();

}
