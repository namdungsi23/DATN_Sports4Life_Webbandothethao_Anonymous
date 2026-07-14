package poly.edu.ASSM.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "OrderAddresses")
public class OrderAddresses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OrderId", nullable = false)
    private Orders order;

    @Nationalized
    @Column(name = "ReceiverName", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "ReceiverPhone", nullable = false, length = 20)
    private String receiverPhone;

    @Nationalized
    @Column(name = "Province", nullable = false, length = 100)
    private String province;

    @Nationalized
    @Column(name = "Ward", nullable = false, length = 100)
    private String ward;

    @Nationalized
    @Column(name = "AddressDetail", nullable = false, length = 255)
    private String addressDetail;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

}
