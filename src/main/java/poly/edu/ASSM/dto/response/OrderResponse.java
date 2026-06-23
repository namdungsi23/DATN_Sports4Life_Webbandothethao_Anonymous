package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class OrderResponse {
    private Integer id;
    private String username;
    private LocalDate createDate;
    private String address;
    private String status;
    private BigDecimal totalAmount;
    private Integer voucherId;
    private String voucherCode;

    @Builder.Default
    private List<OrderDetailResponse> details = new ArrayList<>();
}
