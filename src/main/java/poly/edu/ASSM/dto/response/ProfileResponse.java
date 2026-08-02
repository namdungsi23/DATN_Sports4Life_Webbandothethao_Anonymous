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
public class ProfileResponse {
    private String username;
    private String email;
    private Instant createdAt;
    private String fullname;
    private String photo;
    private String avatar;
    private String phone;
    private Instant createDate;
    private Integer totalPoint;
    private Integer rankId;
    private String rankName;
    private BigDecimal rankDiscountPercent;
    private Integer rankMinPoint;
}
