package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.CommentService;
import poly.edu.ASSM.dto.request.CommentRequest;

@RestController
@RequestMapping("/api")
public class CommentApiController {

	@Autowired
	private CommentService commentService;

	/** Công khai: danh sách bình luận + điểm trung bình */
	@GetMapping("/public/products/{productId}/comments")
	public Map<String, Object> list(@PathVariable Long productId) {
		return commentService.listByProduct(productId);
	}

	/** Đã đăng nhập: gửi / cập nhật bình luận + rating */
	@PostMapping("/comments")
	public ResponseEntity<Map<String, Object>> createOrUpdate(
			Principal principal,
			@Valid @RequestBody CommentRequest request) {
		if (principal == null) {
			return ResponseEntity.status(401).body(Map.of("ok", false, "message", "Chưa đăng nhập."));
		}
		return ResponseEntity.ok(commentService.createOrUpdate(principal.getName(), request));
	}
}
