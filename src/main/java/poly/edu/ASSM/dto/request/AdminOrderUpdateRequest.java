package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminOrderUpdateRequest {
    private Integer orderId;
    private String orderStatus;
    private String paymentStatus;
    private Integer carrierId;
    private String trackingNumber;
    private String shippingStatus;
    private BigDecimal shippingFee;
    private String notes;
}
