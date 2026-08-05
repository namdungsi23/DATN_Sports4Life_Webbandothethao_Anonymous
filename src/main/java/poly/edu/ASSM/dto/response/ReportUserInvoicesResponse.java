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
public class ReportUserInvoicesResponse {
    private Long accountId;
    private String username;
    private String email;
    private Integer invoiceCount;
    private BigDecimal totalSpent;
    private List<ReportInvoiceRowResponse> invoices;
}
