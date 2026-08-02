package poly.edu.ASSM.api.admin;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.Services.core.AdminUserService;
import poly.edu.ASSM.dto.response.AdminUserResponse;

/**
 * Thin API: chỉ Service + DTO. Không Entity / Repository.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("@adminAuth.has('USER_VIEW')")
public class AdminUserApiController {

    @Autowired
    private AdminUserService adminUserService;

    @GetMapping
    public Map<String, Object> index(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page) {
        return adminUserService.listUsers(keyword, page, 5);
    }

    @GetMapping("/{username}")
    public ResponseEntity<Map<String, Object>> one(
            @PathVariable String username,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page) {
        Map<String, Object> body = adminUserService.getUser(username, keyword, page);
        if (body == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(body);
    }

    public record UserSaveBody(String username, String fullname, String email, String photo, Boolean activated) {
    }

    @PostMapping("/save")
    @PreAuthorize("@adminAuth.isAdmin()")
    public ResponseEntity<?> save(@RequestBody UserSaveBody body) {
        if (body == null) {
            return ResponseEntity.badRequest().build();
        }
        AdminUserResponse user = adminUserService.save(
                body.username(), body.fullname(), body.email(), body.photo(), body.activated());
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Cập nhật thành công",
                "user", user));
    }

    @PostMapping(value = "/{username}/avatar", consumes = "multipart/form-data")
    @PreAuthorize("@adminAuth.isAdmin()")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String username,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(adminUserService.uploadAvatar(username, file));
    }

    public record MemberPointsBody(Integer totalPoint) {
    }

    @PostMapping("/{accountId}/points")
    @PreAuthorize("@adminAuth.isAdmin()")
    public ResponseEntity<?> setPoints(@PathVariable Long accountId, @RequestBody MemberPointsBody body) {
        Integer points = body != null ? body.totalPoint() : null;
        return ResponseEntity.ok(adminUserService.setPoints(accountId, points));
    }
}
