package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
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
public class VoucherResponse {
    private Integer id;
    private String code;
    private String name;
    private Integer discountPercent;
    private BigDecimal discountAmount;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscount;
    private Integer quantity;
    private Integer usedCount;
    private Instant startDate;
    private Instant expiredAt;
    private Short isActive;
}
