package poly.edu.ASSM.Services.core;

import java.util.List;

import poly.edu.ASSM.domain.ConversationStatus;
import poly.edu.ASSM.dto.response.ConversationDetailResponse;
import poly.edu.ASSM.dto.response.ConversationSummaryResponse;
import poly.edu.ASSM.dto.response.CreateConversationResponse;

public interface ConversationService {

    /**
     * User mở widget: tái sử dụng OPEN hoặc tạo mới + gán employee.
     */
    CreateConversationResponse openOrCreate(String username);

    /**
     * Conversation OPEN hiện tại của User (không tạo mới).
     */
    CreateConversationResponse getCurrentOpen(String username);

    ConversationDetailResponse getDetailForUser(String username, Long conversationId);

    ConversationDetailResponse getDetailForEmployee(String username, Long conversationId);

    List<ConversationSummaryResponse> listForEmployee(String username, ConversationStatus status);

    ConversationSummaryResponse updateStatus(String username, Long conversationId, ConversationStatus status);
}
