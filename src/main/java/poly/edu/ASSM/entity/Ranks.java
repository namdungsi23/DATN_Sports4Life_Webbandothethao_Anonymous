package poly.edu.ASSM.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "Ranks")
public class Ranks {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Integer id;

    @Nationalized
    @Column(name = "RankName", nullable = false, length = 50)
    private String rankName;

    @Column(name = "MinPoint", nullable = false)
    private Integer minPoint;

    @ColumnDefault("0")
    @Column(name = "DiscountPercent", precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @Nationalized
    @Column(name = "Description", length = 255)
    private String description;

    @ColumnDefault("1")
    @Column(name = "IsActive")
    private Boolean isActive;

    @OneToMany
    @JoinColumn(name = "RankId")
    private Set<Users> users = new LinkedHashSet<>();

}
