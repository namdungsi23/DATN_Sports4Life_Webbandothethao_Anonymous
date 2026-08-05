package poly.edu.ASSM.dto.response;

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
public class ReportSeriesResponse {
    private String period;
    private List<String> labels;
    private List<Number> revenue;
    private List<Integer> orderCounts;
    private String from;
    private String to;
}
