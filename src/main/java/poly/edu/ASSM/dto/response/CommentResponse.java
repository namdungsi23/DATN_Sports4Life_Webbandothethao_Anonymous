package poly.edu.ASSM.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {
	private Integer id;
	private Long productId;
	private Long userId;
	private String username;
	private String fullName;
	private String avatar;
	private String rankName;
	private Integer rating;
	private String content;
	private Boolean status;
	private String productName;
	private Instant createdAt;
	private Instant updatedAt;
}
