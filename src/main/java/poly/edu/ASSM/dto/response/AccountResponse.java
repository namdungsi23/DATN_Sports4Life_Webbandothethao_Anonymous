package poly.edu.ASSM.dto.response;

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
public class AccountResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String avatar;
    private Boolean isActive;
    private Boolean admin;
    private Boolean superAdmin;
    private Instant createdAt;
    private Instant updatedAt;
}
