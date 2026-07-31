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
@Table(name = "Addresses")
public class Addresses {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "AccountId", nullable = false)
    private Accounts account;

    @Nationalized
    @Column(name = "Label", length = 50)
    private String label;

    @Nationalized
    @Column(name = "Province", nullable = false, length = 100)
    private String province;

    @Nationalized
    @Column(name = "Ward", nullable = false, length = 100)
    private String ward;

    @Nationalized
    @Column(name = "AddressDetail", nullable = false, length = 255)
    private String addressDetail;

    @ColumnDefault("0")
    @Column(name = "IsDefault")
    private Boolean isDefault;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

}
