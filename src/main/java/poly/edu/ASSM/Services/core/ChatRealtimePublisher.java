package poly.edu.ASSM.Services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.dto.request.TypingRequest;
import poly.edu.ASSM.dto.response.MessageResponse;

/**
 * Phát tin realtime tới topic conversation.
 * Tách khỏi WebSocket controller để REST cũng broadcast được.
 */
@Service
public class ChatRealtimePublisher {

    public static final String CONVERSATION_TOPIC = "/topic/conversations/";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void publishMessage(MessageResponse message) {
        if (message == null || message.getConversationId() == null) {
            return;
        }
        messagingTemplate.convertAndSend(CONVERSATION_TOPIC + message.getConversationId(), message);
    }

    public void publishTyping(Long conversationId, String username, boolean typing) {
        if (conversationId == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                CONVERSATION_TOPIC + conversationId + "/typing",
                MapTyping.of(username, typing));
    }

    public record MapTyping(String username, boolean typing) {
        public static MapTyping of(String username, boolean typing) {
            return new MapTyping(username, typing);
        }
    }

    public void publishTyping(TypingRequest request, String username) {
        if (request == null) {
            return;
        }
        publishTyping(request.getConversationId(), username, request.isTyping());
    }
}
