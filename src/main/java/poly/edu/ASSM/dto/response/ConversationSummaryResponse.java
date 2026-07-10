package poly.edu.ASSM.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import poly.edu.ASSM.domain.ConversationStatus;

/**
 * Bản tóm tắt cho danh sách inbox Employee / trạng thái nhanh.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationSummaryResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private Long employeeId;
    private String employeeFullName;
    private ConversationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastMessagePreview;
    private Instant lastMessageAt;
}
