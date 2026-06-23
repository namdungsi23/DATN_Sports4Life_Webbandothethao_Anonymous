package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "Username", length = 30)
    private String username;

    @Column(name = "CreateDate")
    private LocalDate createDate;

    @Nationalized
    @Column(name = "Address", length = 100)
    private String address;

    @ColumnDefault("'NEW'")
    @Column(name = "Status", length = 50)
    private String status;

    @ColumnDefault("0")
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VoucherId")
    private Voucher voucher;

    @OneToMany(mappedBy = "orders")
    private Set<OrderDetails> orderDetails = new LinkedHashSet<>();
    @OneToMany(mappedBy = "order")
    private Set<Payments> payments = new LinkedHashSet<>();


}