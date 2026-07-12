package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Ranks;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.RankRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.dto.request.ChangePasswordRequest;
import poly.edu.ASSM.dto.request.ProfileUpdateRequest;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.security.PasswordPolicy;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RankRepository rankRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> getProfile(String username) {
        Accounts account = requireAccount(username);
        return Map.of("profile", toProfileMap(account));
    }

    @Override
    public Map<String, Object> updateProfile(String username, ProfileUpdateRequest request) {
        Accounts account = requireAccount(username);
        Users users = requireOrCreateUser(account);

        if (request.getFullname() != null) {
            users.setFullName(request.getFullname().trim());
        }
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail().trim());
        }
        if (request.getPhoto() != null && !request.getPhoto().isBlank()) {
            users.setAvatar(request.getPhoto().trim());
        }
        if (request.getPhone() != null) {
            users.setPhone(request.getPhone().trim());
        }

        account.setUpdatedAt(Instant.now());
        users.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        usersRepository.save(users);

        return Map.of("profile", toProfileMap(account), "message", "Cập nhật hồ sơ thành công");
    }

    @Override
    public Map<String, Object> uploadAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh");
        }

        Accounts account = requireAccount(username);
        Users users = requireOrCreateUser(account);
        String url = cloudinaryService.uploadAvatar(file);
        users.setAvatar(url);
        users.setUpdatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);
        usersRepository.save(users);

        return Map.of(
                "profile", toProfileMap(account),
                "photo", url,
                "message", "Cập nhật ảnh đại diện thành công");
    }

    @Override
    public Map<String, Object> changePassword(String username, ChangePasswordRequest request) {
        Accounts account = requireAccount(username);
        String stored = account.getPasswordHash();
        if (stored == null || stored.isBlank()) {
            throw new InvalidInputException(
                    "Tài khoản đăng nhập bằng Google không có mật khẩu. Dùng Quên mật khẩu nếu muốn đặt mật khẩu.");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), stored)) {
            throw new InvalidInputException("Mật khẩu hiện tại không đúng.");
        }

        if (request.getNewPassword() == null || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new InvalidInputException("Mật khẩu xác nhận không khớp.");
        }

        String pwdError = PasswordPolicy.validate(request.getNewPassword());
        if (pwdError != null) {
            throw new InvalidInputException(pwdError);
        }

        if (passwordEncoder.matches(request.getNewPassword(), stored)) {
            throw new InvalidInputException("Mật khẩu mới phải khác mật khẩu hiện tại.");
        }

        account.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        return Map.of("ok", true, "message", "Đổi mật khẩu thành công.");
    }

    private Accounts requireAccount(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        }
        return account;
    }

    private Users requireOrCreateUser(Accounts account) {
        return usersRepository.findByAccount_Id(account.getId()).orElseGet(() -> {
            Users created = new Users();
            created.setAccount(account);
            created.setGender(0);
            created.setTotalPoint(0);
            created.setRank(requireDefaultRank());
            created.setCreatedAt(Instant.now());
            return created;
        });
    }

    private Ranks requireDefaultRank() {
        return rankRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException("Default rank not found"));
    }

    private Map<String, Object> toProfileMap(Accounts account) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", account.getUsername());
        profile.put("email", account.getEmail());
        profile.put("createdAt", account.getCreatedAt());

        usersRepository.findByAccount_Id(account.getId()).ifPresent(users -> {
            profile.put("fullname", users.getFullName());
            profile.put("photo", users.getAvatar());
            profile.put("avatar", users.getAvatar());
            profile.put("phone", users.getPhone());
            profile.put("createDate", users.getCreatedAt());
            profile.put("totalPoint", users.getTotalPoint() != null ? users.getTotalPoint() : 0);
            if (users.getRank() != null) {
                profile.put("rankId", users.getRank().getId());
                profile.put("rankName", users.getRank().getRankName());
                profile.put("rankDiscountPercent", users.getRank().getDiscountPercent());
                profile.put("rankMinPoint", users.getRank().getMinPoint());
            } else {
                profile.put("rankId", null);
                profile.put("rankName", null);
                profile.put("rankDiscountPercent", null);
                profile.put("rankMinPoint", null);
            }
        });

        return profile;
    }
}
