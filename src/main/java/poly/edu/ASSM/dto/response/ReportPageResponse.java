package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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
public class ReportPageResponse {
    private List<ReportInvoiceRowResponse> items;
    private Integer page;
    private Integer size;
    private Integer totalElements;
    private Integer totalPages;
    private BigDecimal totalRevenue;
    private Integer totalOrdersAll;
    private Long excludedCancelled;
    private Long excludedOutOfRange;
    private String from;
    private String to;
}
