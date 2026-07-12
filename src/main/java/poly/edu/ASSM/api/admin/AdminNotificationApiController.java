package poly.edu.ASSM.api.admin;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminNotificationService;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminNotificationApiController {

	@Autowired
	private AdminNotificationService adminNotificationService;

	@GetMapping
	public Map<String, Object> list(
			Principal principal,
			@RequestParam(defaultValue = "20") int limit) {
		return adminNotificationService.listForUsername(principal.getName(), limit);
	}

	@GetMapping("/unread-count")
	public Map<String, Object> unreadCount(Principal principal) {
		return adminNotificationService.unreadCount(principal.getName());
	}

	@PatchMapping("/{id}/read")
	public ResponseEntity<Map<String, Object>> markRead(Principal principal, @PathVariable int id) {
		return ResponseEntity.ok(adminNotificationService.markRead(principal.getName(), id));
	}

	@PatchMapping("/read-all")
	public ResponseEntity<Map<String, Object>> markAllRead(Principal principal) {
		return ResponseEntity.ok(adminNotificationService.markAllRead(principal.getName()));
	}
}
