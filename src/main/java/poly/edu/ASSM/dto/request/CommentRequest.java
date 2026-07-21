package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequest {

	@NotNull(message = "ProductId là bắt buộc.")
	private Long productId;

	@NotNull(message = "Rating là bắt buộc.")
	@Min(value = 1, message = "Rating tối thiểu là 1 sao.")
	@Max(value = 5, message = "Rating tối đa là 5 sao.")
	private Integer rating;

	@NotBlank(message = "Nội dung bình luận là bắt buộc.")
	@Size(max = 1000, message = "Bình luận tối đa 1000 ký tự.")
	private String content;
}
