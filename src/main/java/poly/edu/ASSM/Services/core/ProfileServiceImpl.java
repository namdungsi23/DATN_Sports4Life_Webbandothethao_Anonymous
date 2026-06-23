package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.UsersRepository;
import poly.edu.ASSM.Services.util.CloudinaryService;
import poly.edu.ASSM.dto.request.ProfileUpdateRequest;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Override
    public Map<String, Object> getProfile(String username) {
        Accounts account = requireAccount(username);
        return Map.of("profile", toProfileMap(account));
    }

    @Override
    public Map<String, Object> updateProfile(String username, ProfileUpdateRequest request) {
        Accounts account = requireAccount(username);

        if (request.getFullname() != null) {
            account.setFullName(request.getFullname().trim());
        }
        if (request.getEmail() != null) {
            account.setEmail(request.getEmail().trim());
        }
        if (request.getPhoto() != null && !request.getPhoto().isBlank()) {
            account.setAvatar(request.getPhoto().trim());
        }

        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        if (request.getPhone() != null) {
            Users users = usersRepository.findByAccount_Id(account.getId()).orElseGet(() -> {
                Users created = new Users();
                created.setAccount(account);
                created.setCreatedAt(Instant.now());
                return created;
            });
            users.setPhone(request.getPhone().trim());
            users.setUpdatedAt(Instant.now());
            usersRepository.save(users);
        }

        return Map.of("profile", toProfileMap(account), "message", "Cập nhật hồ sơ thành công");
    }

    @Override
    public Map<String, Object> uploadAvatar(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh");
        }

        Accounts account = requireAccount(username);
        String url = cloudinaryService.uploadAvatar(file);
        account.setAvatar(url);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        return Map.of(
                "profile", toProfileMap(account),
                "photo", url,
                "message", "Cập nhật ảnh đại diện thành công");
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

    private Map<String, Object> toProfileMap(Accounts account) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", account.getUsername());
        profile.put("fullname", account.getFullName());
        profile.put("email", account.getEmail());
        profile.put("photo", account.getAvatar());
        profile.put("avatar", account.getAvatar());
        profile.put("createdAt", account.getCreatedAt());

        usersRepository.findByAccount_Id(account.getId()).ifPresent(users -> {
            profile.put("phone", users.getPhone());
            profile.put("createDate", users.getCreatedAt());
        });

        return profile;
    }
}
