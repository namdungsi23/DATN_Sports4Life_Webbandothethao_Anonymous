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
public class ShipmentResponse {
    private String shippingStatus;
    private String trackingNumber;
    private BigDecimal shippingFee;
    private String notes;
    private String carrierName;
    private String carrierCode;
}
