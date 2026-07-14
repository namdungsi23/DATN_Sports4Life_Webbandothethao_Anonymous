package poly.edu.ASSM.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "Shipments")
public class Shipments {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OrderId", nullable = false, unique = true)
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CarrierId")
    private Carriers carrier;

    @Column(name = "TrackingNumber", length = 100)
    private String trackingNumber;

    @ColumnDefault("'PENDING'")
    @Column(name = "ShippingStatus", nullable = false, length = 50)
    private String shippingStatus;

    @ColumnDefault("0")
    @Column(name = "ShippingFee", precision = 12, scale = 2)
    private BigDecimal shippingFee;

    @Nationalized
    @Column(name = "Notes", length = 500)
    private String notes;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;
}
