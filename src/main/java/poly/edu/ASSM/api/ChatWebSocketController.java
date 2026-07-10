package poly.edu.ASSM.api;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import poly.edu.ASSM.Services.chat.websocket.ConversationAccessGuard;
import poly.edu.ASSM.Services.core.ChatIdentityService;
import poly.edu.ASSM.Services.core.ChatRealtimePublisher;
import poly.edu.ASSM.Services.core.MessageService;
import poly.edu.ASSM.dto.request.SendMessageRequest;
import poly.edu.ASSM.dto.request.TypingRequest;
import poly.edu.ASSM.dto.response.MessageResponse;

/**
 * STOMP handlers — không truy cập Repository trực tiếp.
 *
 * Client publish:
 *   /app/chat.send
 *   /app/chat.typing
 *
 * Server broadcast:
 *   /topic/conversations/{id}
 *   /topic/conversations/{id}/typing
 */
@Controller
public class ChatWebSocketController {

    @Autowired
    private MessageService messageService;

    @Autowired
    private ChatRealtimePublisher chatRealtimePublisher;

    @Autowired
    private ChatIdentityService chatIdentityService;

    @Autowired
    private ConversationAccessGuard conversationAccessGuard;

    @MessageMapping("/chat.send")
    public void send(@Payload SendMessageRequest request, Principal principal) {
        if (principal == null || request == null || request.getConversationId() == null) {
            return;
        }
        String username = principal.getName();
        if (!conversationAccessGuard.canAccess(username, request.getConversationId())) {
            throw new IllegalArgumentException("Không có quyền gửi tin trong cuộc hội thoại này");
        }

        MessageResponse saved;
        if (chatIdentityService.isEmployee(username)) {
            saved = messageService.saveEmployeeMessage(
                    username, request.getConversationId(), request.getContent());
        } else {
            saved = messageService.saveUserMessage(
                    username, request.getConversationId(), request.getContent());
        }
        chatRealtimePublisher.publishMessage(saved);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, Principal principal) {
        if (principal == null || request == null || request.getConversationId() == null) {
            return;
        }
        if (!conversationAccessGuard.canAccess(principal.getName(), request.getConversationId())) {
            return;
        }
        chatRealtimePublisher.publishTyping(request, principal.getName());
    }
}
