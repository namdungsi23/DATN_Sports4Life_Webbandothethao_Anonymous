package poly.edu.ASSM.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Messages;

@Repository
public interface MessageRepository extends JpaRepository<Messages, Long> {

    /**
     * Toàn bộ lịch sử theo thứ tự thời gian tăng dần.
     */
    List<Messages> findByConversation_IdOrderByCreatedAtAsc(Long conversationId);

    /**
     * Lịch sử có phân trang (load thêm tin cũ / mới).
     */
    Page<Messages> findByConversation_IdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    long countByConversation_Id(Long conversationId);
}
