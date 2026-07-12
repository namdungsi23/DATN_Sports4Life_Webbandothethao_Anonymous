package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Notification;
import poly.edu.ASSM.Entity.Ranks;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.NotificationRepository;
import poly.edu.ASSM.Repository.RankRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.mapper.NotificationMapper;

@Service
public class AdminNotificationService {

	private static final Logger log = LoggerFactory.getLogger(AdminNotificationService.class);

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Autowired
	private UsersRepository usersRepository;

	@Autowired
	private RankRepository rankRepository;

	@Autowired
	private NotificationMapper notificationMapper;

	/**
	 * Gửi thông báo tới mọi tài khoản ADMIN/STAFF.
	 * Tự tạo hồ sơ Users nếu thiếu (admin thường chỉ có Accounts).
	 * REQUIRES_NEW: lỗi notify không làm rollback đơn hàng.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void notifyPanelUsers(String title, String message, String link) {
		List<Accounts> panel = accountRepository.findPanelAccounts();
		if (panel.isEmpty()) {
			log.warn("Không có tài khoản ADMIN/STAFF để nhận thông báo: {}", title);
			return;
		}
		Instant now = Instant.now();
		int saved = 0;
		for (Accounts account : panel) {
			Users user = resolveOrCreatePanelUser(account);
			if (user == null) {
				log.warn("Bỏ qua notify cho account {} — không tạo được Users", account.getUsername());
				continue;
			}
			Notification n = new Notification();
			n.setUsers(user);
			n.setTitle(title);
			n.setMessage(message);
			n.setLink(link);
			n.setIsRead(false);
			n.setCreatedAt(now);
			notificationRepository.save(n);
			saved++;
		}
		log.info("Đã gửi {} thông báo panel: {}", saved, title);
	}

	@Transactional
	public Map<String, Object> listForUsername(String username, int limit) {
		Users user = requireUser(username);
		int size = Math.min(Math.max(limit, 1), 50);
		List<Notification> rows = notificationRepository.findByUsers_IdOrderByCreatedAtDesc(
				user.getId(), PageRequest.of(0, size));
		long unread = notificationRepository.countByUsers_IdAndIsReadFalse(user.getId());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("notifications", notificationMapper.toResponseList(rows));
		body.put("unreadCount", unread);
		return body;
	}

	@Transactional
	public Map<String, Object> unreadCount(String username) {
		try {
			Users user = requireUser(username);
			return Map.of("unreadCount", notificationRepository.countByUsers_IdAndIsReadFalse(user.getId()));
		} catch (ResponseStatusException ex) {
			throw ex;
		} catch (Exception ex) {
			log.warn("unread-count lỗi (kiểm tra bảng Notifications/cột Link): {}", ex.getMessage());
			return Map.of("unreadCount", 0L);
		}
	}

	@Transactional
	public Map<String, Object> markRead(String username, int id) {
		Users user = requireUser(username);
		int updated = notificationRepository.markRead(id, user.getId());
		if (updated == 0) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo.");
		}
		return Map.of("ok", true);
	}

	@Transactional
	public Map<String, Object> markAllRead(String username) {
		Users user = requireUser(username);
		notificationRepository.markAllRead(user.getId());
		return Map.of("ok", true);
	}

	private Users requireUser(String username) {
		Accounts account = accountRepository.findByUsername(username);
		if (account == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập.");
		}
		Users user = resolveOrCreatePanelUser(account);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Tài khoản chưa có hồ sơ Users để nhận thông báo.");
		}
		return user;
	}

	/** Lấy hoặc tạo Users cho tài khoản panel (ADMIN/STAFF). */
	private Users resolveOrCreatePanelUser(Accounts account) {
		Users user = account.getUsers();
		if (user == null) {
			user = usersRepository.findByAccount_Id(account.getId()).orElse(null);
		}
		if (user != null) {
			return user;
		}
		Ranks rank = rankRepository.findById(1).orElse(null);
		if (rank == null) {
			log.error("Không tìm thấy RankId=1 — không tạo Users cho {}", account.getUsername());
			return null;
		}
		Users created = new Users();
		created.setAccount(account);
		created.setFullName(account.getUsername());
		created.setGender(0);
		created.setTotalPoint(0);
		created.setTotalSpending(BigDecimal.ZERO);
		created.setRank(rank);
		created.setCreatedAt(Instant.now());
		return usersRepository.save(created);
	}
}
