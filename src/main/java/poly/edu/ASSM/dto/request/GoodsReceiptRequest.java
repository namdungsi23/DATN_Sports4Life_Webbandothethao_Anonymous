package poly.edu.ASSM.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoodsReceiptRequest {

    private Long id;

    @Size(max = 20, message = "Mã phiếu nhập tối đa 20 ký tự")
    private String receiptCode;

    private Integer userId;
    private String note;
    private BigDecimal totalAmount;
}
