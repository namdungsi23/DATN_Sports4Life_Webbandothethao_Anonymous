package poly.edu.ASSM.Services.chat.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Entity.Employees;
import poly.edu.ASSM.Repository.ConversationRepository;
import poly.edu.ASSM.Services.core.ChatIdentityService;

/**
 * Kiểm tra User hoặc Employee được gán mới được subscribe / gửi tin conversation.
 */
@Component
public class ConversationAccessGuard {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ChatIdentityService chatIdentityService;

    public boolean canAccess(String username, Long conversationId) {
        if (username == null || conversationId == null) {
            return false;
        }

        if (chatIdentityService.isEmployee(username)) {
            Employees employee = chatIdentityService.requireEmployee(username);
            return conversationRepository.findByIdAndEmployee_Id(conversationId, employee.getId()).isPresent();
        }

        return chatIdentityService.findUser(username)
                .map(user -> conversationRepository.findByIdAndUser_Id(conversationId, user.getId()).isPresent())
                .orElse(false);
    }
}
