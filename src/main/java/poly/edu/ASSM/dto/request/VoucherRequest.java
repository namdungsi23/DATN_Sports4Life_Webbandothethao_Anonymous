package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VoucherRequest {

    private Integer id;

    @NotBlank(message = "Mã voucher không được để trống")
    @Size(max = 50, message = "Mã voucher tối đa 50 ký tự")
    private String code;

    private String name;
    private Integer discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private Integer quantity;
    private Instant startDate;
    private Instant expiredAt;
    private Short isActive;

    /** PERCENT | FIXED | FREESHIP — chỉ dùng khi tạo/cập nhật từ admin */
    private String discountType;
}
