package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RankRequest {

    private Integer id;

    @NotBlank(message = "Tên hạng không được để trống")
    @Size(max = 50, message = "Tên hạng tối đa 50 ký tự")
    private String rankName;

    @NotNull(message = "MinPoint không được để trống")
    private Integer minPoint;

    private BigDecimal discountPercent;
    private String description;
    private Boolean isActive;
}
