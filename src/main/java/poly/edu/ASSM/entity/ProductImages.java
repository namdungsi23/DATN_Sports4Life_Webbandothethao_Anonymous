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
@Table(name = "ProductImages")
public class ProductImages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @Nationalized
    @Column(name = "ImageUrl", nullable = false, length = 500)
    private String imageUrl;

    @ColumnDefault("0")
    @Column(name = "IsDefault")
    private Boolean isDefault;

    @ColumnDefault("1")
    @Column(name = "SortOrder")
    private Integer sortOrder;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VariantId")
    private ProductVariants variant;

}