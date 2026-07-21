package poly.edu.ASSM.Services.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Theo dõi Employee đang online (in-memory).
 * Sau này có thể thay Impl bằng Redis mà không đổi caller.
 */
public interface OnlineEmployeeService {

    void employeeConnected(Long employeeId, String sessionId);

    void employeeDisconnected(Long employeeId, String sessionId);

    boolean isOnline(Long employeeId);

    List<Long> getOnlineEmployeeIds();

    Set<String> getSessionIds(Long employeeId);

    /**
     * Chọn employee khả dụng theo {@link poly.edu.ASSM.Services.chat.assignment.EmployeeAssignmentStrategy}.
     */
    Optional<Long> getAvailableEmployeeId();
}
