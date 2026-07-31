package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "ProductVariants")
public class ProductVariants {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ProductId", nullable = false)
    private Products product;

    @Column(name = "SKU", nullable = false, length = 100)
    private String sku;

    @Nationalized
    @Column(name = "Size", length = 50)
    private String size;

    @Nationalized
    @Column(name = "Color", length = 50)
    private String color;

    @Column(name = "Price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "IsDefault")
    private Boolean isDefault;

    @Column(name = "DisplayOrder")
    private Integer displayOrder;

    @ColumnDefault("1")
    @Column(name = "Status")
    private Boolean status;

    @ColumnDefault("getdate()")
    @Column(name = "CreatedAt")
    private Instant createdAt;

    @Column(name = "UpdatedAt")
    private Instant updatedAt;

    @Column(name = "Quantity")
    private Short quantity;

    @ColumnDefault("0")
    @Column(name = "SoldCount", nullable = false)
    private Integer soldCount;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductImages> productImages = new LinkedHashSet<>();

    @OneToMany(mappedBy = "variant")
    private Set<OrderDetails> orderDetails = new LinkedHashSet<>();

}
