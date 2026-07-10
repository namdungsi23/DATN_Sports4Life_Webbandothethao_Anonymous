package poly.edu.ASSM.dto.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import poly.edu.ASSM.domain.MessageSenderType;
import poly.edu.ASSM.domain.MessageType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {

    private Long id;
    private Long conversationId;
    private MessageSenderType senderType;
    private Long senderId;
    private MessageType messageType;
    private String content;
    private Instant createdAt;
    private Boolean seen;
}
