package poly.edu.ASSM.Services.core;

import java.util.List;

import poly.edu.ASSM.domain.MessageSenderType;
import poly.edu.ASSM.dto.response.MessageResponse;

public interface MessageService {

    MessageResponse saveUserMessage(String username, Long conversationId, String content);

    MessageResponse saveEmployeeMessage(String username, Long conversationId, String content);

    List<MessageResponse> loadHistoryForUser(String username, Long conversationId);

    List<MessageResponse> loadHistoryForEmployee(String username, Long conversationId);

    /**
     * Đánh dấu tin nhắn phía đối phương đã xem (đơn giản: toàn bộ unseen của conversation).
     */
    int markSeen(String username, Long conversationId, MessageSenderType viewerType);
}
