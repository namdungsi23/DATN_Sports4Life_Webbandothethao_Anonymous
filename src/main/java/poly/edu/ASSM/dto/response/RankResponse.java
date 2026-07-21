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
public class RankResponse {
    private Integer id;
    private String rankName;
    private Integer minPoint;
    private BigDecimal discountPercent;
    private String description;
    private Boolean isActive;
    private Long memberCount;
}
