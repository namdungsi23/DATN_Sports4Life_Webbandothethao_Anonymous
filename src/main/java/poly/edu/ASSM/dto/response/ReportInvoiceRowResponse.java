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
public class ReportInvoiceRowResponse {
    private Integer id;
    private String invoiceCode;
    private Long accountId;
    private String username;
    private String email;
    private Instant createDate;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private String carrierName;
}
