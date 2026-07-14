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
@Table(name = "PaymentMethods")
public class PaymentMethods {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @Column(name = "Code", nullable = false, length = 50)
    private String code;

    @Nationalized
    @Column(name = "Name", length = 100)
    private String name;

    @Nationalized
    @Column(name = "Description")
    private String description;

    @ColumnDefault("1")
    @Column(name = "IsActive")
    private Boolean isActive;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;


}