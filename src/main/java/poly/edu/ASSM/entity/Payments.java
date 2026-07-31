package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "Payments")
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private Users users;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OrderId", nullable = false)
    private Orders order;

    @Column(name = "Amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "Status", length = 50)
    private String status;

    @Column(name = "PaidAt")
    private Instant paidAt;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PaymentMethodId", nullable = false)
    private PaymentMethods paymentMethods;


}