package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminNotificationService;

/** Chuông thông báo trang user (cần đăng nhập). */
@RestController
@RequestMapping("/api/notifications")
public class CustomerNotificationApiController {

	@Autowired
	private AdminNotificationService notificationService;

	@GetMapping
	public Map<String, Object> list(
			Principal principal,
			@RequestParam(defaultValue = "20") int limit) {
		return notificationService.listForUsername(principal.getName(), limit);
	}

	@GetMapping("/unread-count")
	public Map<String, Object> unreadCount(Principal principal) {
		return notificationService.unreadCount(principal.getName());
	}

	@PatchMapping("/{id}/read")
	public Map<String, Object> markRead(Principal principal, @PathVariable int id) {
		return notificationService.markRead(principal.getName(), id);
	}

	@PatchMapping("/read-all")
	public Map<String, Object> markAllRead(Principal principal) {
		return notificationService.markAllRead(principal.getName());
	}
}
