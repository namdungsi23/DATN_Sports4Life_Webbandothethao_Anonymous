package poly.edu.ASSM.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Gửi tin nhắn TEXT qua REST hoặc STOMP payload.
 */
@Getter
@Setter
public class SendMessageRequest {

    @NotNull(message = "ConversationId không được để trống")
    private Long conversationId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    @Size(max = 4000, message = "Nội dung tin nhắn tối đa 4000 ký tự")
    private String content;
}
