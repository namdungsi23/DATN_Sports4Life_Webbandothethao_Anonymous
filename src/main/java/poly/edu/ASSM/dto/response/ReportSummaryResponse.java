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
public class ReportSummaryResponse {
    private String from;
    private String to;
    private Integer orderCount;
    private Integer invoiceCount;
    private BigDecimal totalRevenue;
    private BigDecimal subTotal;
    private Long paidCount;
    private Long unpaidCount;
    private BigDecimal averageOrderValue;
    private Integer totalOrdersAll;
    private Long excludedCancelled;
    private Long excludedOutOfRange;
}
