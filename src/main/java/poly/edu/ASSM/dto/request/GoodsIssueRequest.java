package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoodsIssueRequest {

    private Long id;

    @Size(max = 20, message = "Mã phiếu xuất tối đa 20 ký tự")
    private String issueCode;

    private Integer userId;
    private String note;
}
