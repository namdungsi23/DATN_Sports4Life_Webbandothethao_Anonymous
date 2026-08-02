package poly.edu.ASSM.dto.response;

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
public class AdminSearchUserHit {
    private Long id;
    private String username;
    private String fullname;
    private String email;
    private String link;
}
