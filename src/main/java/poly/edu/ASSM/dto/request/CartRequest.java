package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CartRequest {

    private Integer id;

    @NotNull(message = "UserId không được để trống")
    private Integer userId;
}
