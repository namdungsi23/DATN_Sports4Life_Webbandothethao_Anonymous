package poly.edu.ASSM.dto.response;

import java.util.Map;

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
public class ReportStatusBreakdownResponse {
    private Map<String, Integer> orderStatus;
    private Map<String, Integer> paymentStatus;
    private String from;
    private String to;
}
