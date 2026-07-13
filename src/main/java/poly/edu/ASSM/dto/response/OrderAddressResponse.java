package poly.edu.ASSM.dto.response;

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
public class OrderAddressResponse {
    private Integer id;
    private Integer orderId;
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String ward;
    private String addressDetail;
    private Instant createdAt;
}
