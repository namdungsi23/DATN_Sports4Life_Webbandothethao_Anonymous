package poly.edu.ASSM.Services.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.dto.response.AdminUserResponse;
import poly.edu.ASSM.dto.response.PageResponse;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.mapper.AccountMapper;

/**
 * Use-case admin user: Repository/Entity chỉ ở đây + Mapper → DTO ra API.
 */
@Service
public class AdminUserService {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AdminAccessService adminAccessService;
    @Autowired
    private RankService rankService;
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private AccountMapper accountMapper;

    private AdminUserResponse toRow(Accounts account) {
        if (account == null) {
            return null;
        }
        return accountMapper.toAdminUserResponse(
                account,
                accountService.findCustomerProfileByAccountId(account.getId()),
                adminAccessService.roleNamesForAccount(account));
    }

    private Map<String, Object> pageMeta(PageResponse<?> page) {
        return Map.of(
                "number", page.getNumber(),
                "totalPages", page.getTotalPages(),
                "totalElements", page.getTotalElements(),
                "size", page.getSize(),
                "first", page.isFirst(),
                "last", page.isLast());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listUsers(String keyword, int page, int size) {
        String key = keyword != null ? keyword : "";
        Page<Accounts> pages = key.isEmpty()
                ? accountService.findAll(page, size)
                : accountService.search(key, page, size);
        PageResponse<AdminUserResponse> mapped = accountMapper.toAdminUserPageResponse(
                pages,
                acc -> accountService.findCustomerProfileByAccountId(acc.getId()),
                adminAccessService::roleNamesForAccount);
        return Map.of(
                "users", mapped.getContent(),
                "pages", pageMeta(mapped),
                "keyword", key,
                "authorities", accountService.listRoleNames());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUser(String username, String keyword, int page) {
        Accounts user = accountService.findByUsername(username);
        if (user == null) {
            return null;
        }
        Map<String, Object> listPayload = listUsers(keyword, page, 5);
        return Map.of(
                "user", toRow(user),
                "users", listPayload.get("users"),
                "pages", listPayload.get("pages"),
                "keyword", listPayload.get("keyword"),
                "authorities", listPayload.get("authorities"));
    }

    @Transactional
    public AdminUserResponse save(String username, String fullname, String email, String photo, Boolean activated) {
        if (username == null || username.isBlank()) {
            throw InvalidInputException.of("username", "Thiếu username");
        }
        Accounts target = accountService.findByUsername(username);
        if (target == null) {
            throw InvalidInputException.of("username", "Không tìm thấy người dùng");
        }

        String syncedPhoto = photo;
        if (syncedPhoto != null && !syncedPhoto.isBlank() && !cloudinaryService.isOurCloudinaryUrl(syncedPhoto)) {
            try {
                syncedPhoto = cloudinaryService.uploadAvatarFromUrl(syncedPhoto.trim(), username);
            } catch (Exception e) {
                throw InvalidInputException.of("photo", "Không đồng bộ được ảnh lên Cloudinary.");
            }
        }

        accountService.updateCustomerProfile(
                username,
                fullname,
                email,
                syncedPhoto,
                activated != null ? activated : true);
        return toRow(accountService.findByUsername(username));
    }

    @Transactional
    public Map<String, Object> uploadAvatar(String username, MultipartFile file) {
        Accounts target = accountService.findByUsername(username);
        if (target == null) {
            throw InvalidInputException.of("username", "Không tìm thấy người dùng");
        }
        if (file == null || file.isEmpty()) {
            throw InvalidInputException.of("file", "Vui lòng chọn ảnh đại diện.");
        }

        Users profile = accountService.findCustomerProfileByAccountId(target.getId());
        String oldUrl = profile != null ? profile.getAvatar() : null;
        String url;
        try {
            url = cloudinaryService.uploadAvatar(file, username);
        } catch (IllegalArgumentException e) {
            throw InvalidInputException.of("file", e.getMessage());
        } catch (Exception e) {
            throw InvalidInputException.of("file", "Upload Cloudinary thất bại.");
        }

        accountService.updateCustomerProfile(username, null, null, url, null);
        if (oldUrl != null && !oldUrl.isBlank() && !oldUrl.equals(url)) {
            try {
                cloudinaryService.deleteByImageUrl(oldUrl);
            } catch (Exception ignored) {
                // best-effort
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("ok", true);
        body.put("message", "Đã cập nhật ảnh đại diện (Cloudinary + SQL).");
        body.put("photo", url);
        body.put("user", toRow(accountService.findByUsername(username)));
        return body;
    }

    @Transactional
    public Map<String, Object> setPoints(Long accountId, Integer totalPoint) {
        if (totalPoint == null) {
            throw InvalidInputException.of("totalPoint", "Thiếu totalPoint");
        }
        rankService.setMemberPoints(accountId, totalPoint);
        Users user = accountService.findCustomerProfileByAccountId(accountId);
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("message", "Đã cập nhật điểm thành viên.");
        if (user != null && user.getAccount() != null) {
            res.put("user", toRow(user.getAccount()));
        }
        return res;
    }
}
