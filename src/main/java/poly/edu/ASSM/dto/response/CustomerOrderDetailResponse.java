package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
public class CustomerOrderDetailResponse {
    private Integer id;
    private Instant createDate;
    private String orderStatus;
    private String paymentStatus;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Boolean canCancel;

    @Builder.Default
    private List<CustomerOrderItemResponse> items = new ArrayList<>();

    private OrderAddressResponse shippingAddress;
    private ShipmentResponse shipment;
}
