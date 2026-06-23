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
public class GoodsReceiptResponse {
    private Long id;
    private String receiptCode;
    private Integer userId;
    private String note;
    private BigDecimal totalAmount;
    private Instant createdAt;
}
