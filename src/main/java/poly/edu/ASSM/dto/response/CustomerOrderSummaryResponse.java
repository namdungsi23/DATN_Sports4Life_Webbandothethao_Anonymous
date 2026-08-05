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
public class CustomerOrderSummaryResponse {
    private Integer id;
    private Instant createDate;
    private Instant updateDate;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private Boolean canCancel;
    private String address;
}
