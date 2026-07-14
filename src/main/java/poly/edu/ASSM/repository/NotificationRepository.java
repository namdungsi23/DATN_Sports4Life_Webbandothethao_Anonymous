package poly.edu.ASSM.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Notification;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

	List<Notification> findByUsers_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);

	long countByUsers_IdAndIsReadFalse(Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.users.id = :userId")
	int markRead(@Param("id") Integer id, @Param("userId") Long userId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Notification n SET n.isRead = true WHERE n.users.id = :userId AND (n.isRead = false OR n.isRead IS NULL)")
	int markAllRead(@Param("userId") Long userId);
}
