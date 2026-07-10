package poly.edu.ASSM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Conversations;
import poly.edu.ASSM.domain.ConversationStatus;

@Repository
public interface ConversationRepository extends JpaRepository<Conversations, Long> {

    /**
     * Conversation OPEN hiện tại của User (mở lại widget → tái sử dụng).
     * Dùng findFirst để an toàn nếu vô tình có nhiều bản ghi OPEN.
     */
    @EntityGraph(attributePaths = { "user", "employee" })
    Optional<Conversations> findFirstByUser_IdAndStatusOrderByUpdatedAtDesc(
            Long userId,
            ConversationStatus status);

    default Optional<Conversations> findOpenByUserId(Long userId) {
        return findFirstByUser_IdAndStatusOrderByUpdatedAtDesc(userId, ConversationStatus.OPEN);
    }

    /**
     * Đếm số Conversation OPEN của một Employee — dùng cho LeastConversationStrategy.
     */
    long countByEmployee_IdAndStatus(Long employeeId, ConversationStatus status);

    default long countOpenByEmployeeId(Long employeeId) {
        return countByEmployee_IdAndStatus(employeeId, ConversationStatus.OPEN);
    }

    /**
     * Inbox nhân viên — mới cập nhật trước.
     */
    @EntityGraph(attributePaths = { "user", "employee" })
    List<Conversations> findByEmployee_IdOrderByUpdatedAtDesc(Long employeeId);

    @EntityGraph(attributePaths = { "user", "employee" })
    List<Conversations> findByEmployee_IdAndStatusOrderByUpdatedAtDesc(
            Long employeeId,
            ConversationStatus status);

    /**
     * Kiểm tra User có quyền truy cập Conversation.
     */
    @EntityGraph(attributePaths = { "user", "employee" })
    Optional<Conversations> findByIdAndUser_Id(Long id, Long userId);

    /**
     * Kiểm tra Employee được gán có quyền truy cập Conversation.
     */
    @EntityGraph(attributePaths = { "user", "employee" })
    Optional<Conversations> findByIdAndEmployee_Id(Long id, Long employeeId);

    @EntityGraph(attributePaths = { "user", "employee" })
    @Query("SELECT c FROM Conversations c WHERE c.id = :id")
    Optional<Conversations> findDetailedById(@Param("id") Long id);
}
