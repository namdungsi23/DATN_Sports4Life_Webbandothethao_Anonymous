package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.CommentService;

@RestController
@RequestMapping("/api/admin/comments")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminCommentApiController {

	@Autowired
	private CommentService commentService;

	@GetMapping
	public Map<String, Object> list(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Boolean visible) {
		return commentService.adminList(keyword, visible);
	}

	@PatchMapping("/{id}/visible")
	public ResponseEntity<Map<String, Object>> setVisible(
			@PathVariable int id,
			@RequestParam boolean value) {
		return ResponseEntity.ok(commentService.adminSetVisible(id, value));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable int id) {
		return ResponseEntity.ok(commentService.adminDelete(id));
	}
}
