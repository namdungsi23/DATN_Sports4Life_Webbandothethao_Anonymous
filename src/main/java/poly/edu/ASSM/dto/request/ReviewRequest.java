package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    private Long id;

    @NotNull(message = "UserId không được để trống")
    private Integer userId;

    @NotNull(message = "ProductId không được để trống")
    private Long productId;

    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Integer rating;

    @Size(max = 1000, message = "Comment tối đa 1000 ký tự")
    private String comment;
}
