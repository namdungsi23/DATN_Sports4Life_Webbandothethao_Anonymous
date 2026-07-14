package poly.edu.ASSM.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import poly.edu.ASSM.domain.ConversationStatus;

/**
 * Kết quả mở chat widget: conversation hiện có hoặc mới tạo.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateConversationResponse {

    private Long id;
    private Long userId;
    private Long employeeId;
    private String employeeFullName;
    private ConversationStatus status;
    private boolean newlyCreated;
    private Instant createdAt;
    private Instant updatedAt;
}
