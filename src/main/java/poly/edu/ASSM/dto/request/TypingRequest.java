package poly.edu.ASSM.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Client báo đang gõ (STOMP /app/chat.typing).
 */
@Getter
@Setter
public class TypingRequest {

    private Long conversationId;

    private boolean typing;
}
