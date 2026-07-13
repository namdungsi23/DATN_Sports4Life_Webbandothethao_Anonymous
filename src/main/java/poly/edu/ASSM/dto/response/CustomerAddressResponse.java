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
public class CustomerAddressResponse {
    private Integer id;
    private Long accountId;
    /** Suy ra từ hồ sơ Users liên kết với Account — chỉ để hiển thị */
    private String accountFullName;
    /** Suy ra từ hồ sơ Users liên kết với Account — chỉ để hiển thị */
    private String accountPhone;
    private String label;
    private String province;
    private String ward;
    private String addressDetail;
    private Boolean isDefault;
    private Instant createdAt;
    private Instant updatedAt;
}
