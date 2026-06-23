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
@Table(name = "UserAddresses")
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private Users users;

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
    @Column(name = "AddressDetail", nullable = false)
    private String addressDetail;

    @ColumnDefault("0")
    @Column(name = "IsDefault")
    private Boolean isDefault;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;


}