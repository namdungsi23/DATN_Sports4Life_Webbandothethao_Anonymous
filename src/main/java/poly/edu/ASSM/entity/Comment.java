package poly.edu.ASSM.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

/**
 * Bình luận + xếp hạng sao (1–5) cho sản phẩm.
 * Map bảng dbo.Comments (đã có trong DB).
 */
@Getter
@Setter
@Entity
@Table(name = "Comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "Id", nullable = false)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "ProductId", nullable = false)
	private Products product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "UserId", nullable = false)
	private Users users;

	@Nationalized
	@Column(name = "Content", nullable = false, length = 1000)
	private String content;

	/** Xếp hạng sao 1–5 */
	@Column(name = "Rating", nullable = false)
	private Integer rating;

	@ColumnDefault("getdate()")
	@Column(name = "CreatedAt")
	private Instant createdAt;

	@Column(name = "UpdatedAt")
	private Instant updatedAt;

	/** true = hiển thị */
	@ColumnDefault("1")
	@Column(name = "Status")
	private Boolean status;
}
