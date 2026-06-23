package poly.edu.ASSM.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
public class UserResponse {
    private Integer id;
    private Long accountId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private Integer totalPoint;
    private BigDecimal totalSpending;
    private Integer rankId;
    private String rankName;
    private Instant createdAt;
    private Instant updatedAt;
}
