package poly.edu.ASSM.api.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.RoleRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.core.AdminAccessService;
import poly.edu.ASSM.security.SpringRoleNames;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("@adminAuth.has('USER_VIEW')")
public class AdminUserApiController {

    @Autowired
    private AccountService accSer;
    @Autowired
    private RoleRepository roleRepo;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private AdminAccessService adminAccessService;

    private List<String> loadAuthorities() {
        return roleRepo.findAll().stream()
                .map(Roles::getName)
                .map(SpringRoleNames::normalize)
                .toList();
    }

    private Map<String, Object> toUserRow(Accounts account) {
        Users profile = usersRepository.findByAccount_Id(account.getId()).orElse(null);
        String roleName = account.getRole() != null ? SpringRoleNames.normalize(account.getRole().getName()) : "";

        Map<String, Object> row = new HashMap<>();
        row.put("id", account.getId());
        row.put("username", account.getUsername());
        row.put("fullname", profile != null ? profile.getFullName() : null);
        row.put("email", account.getEmail());
        row.put("photo", profile != null ? profile.getAvatar() : null);
        row.put("avatar", profile != null ? profile.getAvatar() : null);
        row.put("activated", account.getIsActive());
        row.put("admin", roleName.contains("ADMIN"));
        row.put("roleNames", adminAccessService.roleNamesForAccount(account));
        return row;
    }

    @GetMapping
    public Map<String, Object> index(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page) {
        int size = 5;
        Page<Accounts> pages = keyword.isEmpty()
                ? accSer.findAll(page, size)
                : accSer.search(keyword, page, size);
        return Map.of(
                "users", pages.getContent().stream().map(this::toUserRow).toList(),
                "pages", Map.of(
                        "number", pages.getNumber(),
                        "totalPages", pages.getTotalPages(),
                        "totalElements", pages.getTotalElements(),
                        "size", pages.getSize(),
                        "first", pages.isFirst(),
                        "last", pages.isLast()),
                "keyword", keyword,
                "authorities", loadAuthorities());
    }

    @GetMapping("/{username}")
    public ResponseEntity<Map<String, Object>> one(
            @PathVariable String username,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page) {
        Accounts user = accSer.findByUsername(username);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> listPayload = index(keyword, page);
        return ResponseEntity.ok(Map.of(
                "user", toUserRow(user),
                "users", listPayload.get("users"),
                "pages", listPayload.get("pages"),
                "keyword", listPayload.get("keyword"),
                "authorities", listPayload.get("authorities")));
    }

    public record UserSaveBody(String username, String fullname, String email, String photo, Boolean activated) {
    }

    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody UserSaveBody body) {
        if (body == null || body.username() == null || body.username().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Thiếu username"));
        }
        Accounts target = accSer.findByUsername(body.username());
        if (target == null) {
            return ResponseEntity.badRequest().body(Map.of("ok", false, "message", "Không tìm thấy người dùng"));
        }
        accSer.updateCustomerProfile(
                body.username(),
                body.fullname(),
                body.email(),
                body.photo(),
                body.activated() != null ? body.activated() : true);
        return ResponseEntity.ok(Map.of("ok", true, "message", "Cập nhật thành công"));
    }
}
