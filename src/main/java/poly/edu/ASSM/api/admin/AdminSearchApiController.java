package poly.edu.ASSM.api.admin;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Services.core.AdminSearchService;

@RestController
@RequestMapping("/api/admin/search")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
public class AdminSearchApiController {

	@Autowired
	private AdminSearchService adminSearchService;

	@GetMapping
	public Map<String, Object> search(Principal principal, @RequestParam(required = false) String q) {
		String username = principal != null ? principal.getName() : null;
		return adminSearchService.search(username, q);
	}
}
