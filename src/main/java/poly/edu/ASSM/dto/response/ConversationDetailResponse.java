package poly.edu.ASSM.dto.response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import poly.edu.ASSM.domain.ConversationStatus;

/**
 * Chi tiết Conversation kèm lịch sử tin nhắn (mở chat / load history).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationDetailResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private Long employeeId;
    private String employeeFullName;
    private ConversationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<MessageResponse> messages = new ArrayList<>();
}
