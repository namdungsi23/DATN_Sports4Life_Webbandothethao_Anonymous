package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;

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
public class TopProductChartPoint {
    private Object productId;
    private String productName;
    private Long totalQuantity;
    private BigDecimal totalRevenue;
}
