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
public class AdminSearchOrderHit {
    private Integer id;
    private String username;
    private String orderStatus;
    private BigDecimal totalAmount;
    private String link;
}
