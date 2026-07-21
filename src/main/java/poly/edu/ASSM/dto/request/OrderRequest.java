package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderRequest {

    private Integer id;

    @Size(max = 30, message = "Username tối đa 30 ký tự")
    private String username;

    private LocalDate createDate;

    @Size(max = 100, message = "Địa chỉ tối đa 100 ký tự")
    private String address;

    @Size(max = 50, message = "Trạng thái tối đa 50 ký tự")
    private String status;

    private BigDecimal totalAmount;
    private Integer voucherId;
}
