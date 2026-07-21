package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.RankService;
import poly.edu.ASSM.dto.request.RankRequest;

@RestController
@RequestMapping("/api/admin/ranks")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminRankApiController {

	@Autowired
	private RankService rankService;

	@GetMapping
	public Map<String, Object> list() {
		return rankService.adminList();
	}

	@PostMapping
	@PreAuthorize("@adminAuth.isAdmin()")
	public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody RankRequest request) {
		return ResponseEntity.ok(rankService.create(request));
	}

	@PutMapping("/{id}")
	@PreAuthorize("@adminAuth.isAdmin()")
	public ResponseEntity<Map<String, Object>> update(
			@PathVariable Integer id,
			@Valid @RequestBody RankRequest request) {
		return ResponseEntity.ok(rankService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("@adminAuth.isAdmin()")
	public ResponseEntity<Map<String, Object>> delete(@PathVariable Integer id) {
		return ResponseEntity.ok(rankService.delete(id));
	}
}
