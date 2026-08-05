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
public class AdminSearchResponse {
    private String q;
    private List<AdminSearchProductHit> products;
    private List<AdminSearchCategoryHit> categories;
    private List<AdminSearchUserHit> users;
    private List<AdminSearchOrderHit> orders;
}
