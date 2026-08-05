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
public class TopCustomerChartPoint {
    private Object accountId;
    private String username;
    private String displayName;
    private BigDecimal totalSpending;
    private Long orderCount;
}
