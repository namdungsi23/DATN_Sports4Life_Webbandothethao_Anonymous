package poly.edu.ASSM.Services.chat.assignment;

import java.util.List;

/**
 * Chiến lược gán Employee cho Conversation mới.
 * Thay bean implementation để đổi thuật toán (Least / RoundRobin / SkillBased).
 */
public interface EmployeeAssignmentStrategy {

    /**
     * Chọn một employeeId từ danh sách đang online.
     *
     * @param onlineEmployeeIds danh sách employee đang online (không rỗng)
     * @return employeeId được chọn, hoặc empty nếu không chọn được
     */
    java.util.Optional<Long> chooseEmployee(List<Long> onlineEmployeeIds);
}
