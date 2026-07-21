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
import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.RoleRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.core.AdminAccessService;
import poly.edu.ASSM.Services.core.RankService;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.exception.InvalidInputException;
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

    @Autowired
    private RankService rankService;

    @Autowired
    private CloudinaryService cloudinaryService;

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
        row.put("totalPoint", profile != null && profile.getTotalPoint() != null ? profile.getTotalPoint() : 0);
        if (profile != null && profile.getRank() != null) {
            row.put("rankId", profile.getRank().getId());
            row.put("rankName", profile.getRank().getRankName());
        } else {
            row.put("rankId", null);
            row.put("rankName", null);
        }
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
	@PreAuthorize("@adminAuth.isAdmin()")
	public ResponseEntity<?> save(@RequestBody UserSaveBody body) {
        if (body == null || body.username() == null || body.username().isBlank()) {
            throw InvalidInputException.of("username", "Thiếu username");
        }
        Accounts target = accSer.findByUsername(body.username());
        if (target == null) {
            throw InvalidInputException.of("username", "Không tìm thấy người dùng");
        }

        String photo = body.photo();
        if (photo != null && !photo.isBlank() && !cloudinaryService.isOurCloudinaryUrl(photo)) {
            try {
                photo = cloudinaryService.uploadAvatarFromUrl(photo.trim(), body.username());
            } catch (Exception e) {
                throw InvalidInputException.of("photo", "Không đồng bộ được ảnh lên Cloudinary.");
            }
        }

        accSer.updateCustomerProfile(
                body.username(),
                body.fullname(),
                body.email(),
                photo,
                body.activated() != null ? body.activated() : true);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Cập nhật thành công",
                "user", toUserRow(accSer.findByUsername(body.username()))));
    }

    @PostMapping(value = "/{username}/avatar", consumes = "multipart/form-data")
    @PreAuthorize("@adminAuth.isAdmin()")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable String username,
            @RequestParam("file") MultipartFile file) {
        Accounts target = accSer.findByUsername(username);
        if (target == null) {
            throw InvalidInputException.of("username", "Không tìm thấy người dùng");
        }
        if (file == null || file.isEmpty()) {
            throw InvalidInputException.of("file", "Vui lòng chọn ảnh đại diện.");
        }
        Users profile = usersRepository.findByAccount_Id(target.getId()).orElse(null);
        String oldUrl = profile != null ? profile.getAvatar() : null;
        String url;
        try {
            url = cloudinaryService.uploadAvatar(file, username);
        } catch (IllegalArgumentException e) {
            throw InvalidInputException.of("file", e.getMessage());
        } catch (Exception e) {
            throw InvalidInputException.of("file", "Upload Cloudinary thất bại.");
        }
        accSer.updateCustomerProfile(username, null, null, url, null);
        if (oldUrl != null && !oldUrl.isBlank() && !oldUrl.equals(url)) {
            try {
                cloudinaryService.deleteByImageUrl(oldUrl);
            } catch (Exception ignored) {
                // best-effort
            }
        }
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Đã cập nhật ảnh đại diện (Cloudinary + SQL).",
                "photo", url,
                "user", toUserRow(accSer.findByUsername(username))));
    }

    public record MemberPointsBody(Integer totalPoint) {
    }

    @PostMapping("/{accountId}/points")
    @PreAuthorize("@adminAuth.isAdmin()")
    public ResponseEntity<?> setPoints(@PathVariable Long accountId, @RequestBody MemberPointsBody body) {
        if (body == null || body.totalPoint() == null) {
            throw InvalidInputException.of("totalPoint", "Thiếu totalPoint");
        }
        rankService.setMemberPoints(accountId, body.totalPoint());
        Users user = usersRepository.findByAccount_Id(accountId).orElse(null);
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("message", "Đã cập nhật điểm thành viên.");
        if (user != null && user.getAccount() != null) {
            res.put("user", toUserRow(user.getAccount()));
        }
        return ResponseEntity.ok(res);
    }
}
