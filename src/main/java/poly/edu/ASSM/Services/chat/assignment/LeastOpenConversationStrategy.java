package poly.edu.ASSM.Services.chat.assignment;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Repository.ConversationRepository;

/**
 * Ưu tiên Employee có ít Conversation OPEN nhất.
 * Nếu bằng nhau → giữ thứ tự danh sách online (ổn định, deterministic).
 */
@Component
public class LeastOpenConversationStrategy implements EmployeeAssignmentStrategy {

    @Autowired
    private ConversationRepository conversationRepository;

    @Override
    public Optional<Long> chooseEmployee(List<Long> onlineEmployeeIds) {
        if (onlineEmployeeIds == null || onlineEmployeeIds.isEmpty()) {
            return Optional.empty();
        }

        return onlineEmployeeIds.stream()
                .min(Comparator
                        .comparingLong(conversationRepository::countOpenByEmployeeId)
                        .thenComparingInt(onlineEmployeeIds::indexOf));
    }
}
