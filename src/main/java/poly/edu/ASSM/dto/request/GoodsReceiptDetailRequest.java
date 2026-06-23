package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoodsReceiptDetailRequest {

    private Long id;
    private Integer quantity;
    private BigDecimal importPrice;
}
