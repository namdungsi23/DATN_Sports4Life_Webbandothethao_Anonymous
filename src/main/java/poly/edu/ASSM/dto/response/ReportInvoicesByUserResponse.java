package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.util.List;

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
public class ReportInvoicesByUserResponse {
    private List<ReportUserInvoicesResponse> users;
    private Integer totalUsers;
    private Integer totalInvoices;
    private BigDecimal totalRevenue;
    private String from;
    private String to;
}
