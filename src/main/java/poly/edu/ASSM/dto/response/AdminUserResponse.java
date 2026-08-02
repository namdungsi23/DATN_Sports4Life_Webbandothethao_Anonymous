package poly.edu.ASSM.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO danh sách/user admin — khớp contract Vue {@code AdminUserView}.
 * Không expose Entity ra client.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Long id;
    private String username;
    private String fullname;
    private String email;
    private String photo;
    private String avatar;
    private Boolean activated;
    private Boolean admin;
    private List<String> roleNames;
    private Integer totalPoint;
    private Integer rankId;
    private String rankName;
}
