package poly.edu.ASSM.api;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Roles;
import poly.edu.ASSM.Repository.RoleRepository;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.dto.request.RegisterRequest;

@RestController
@RequestMapping("/api/public")
public class PublicAuthApiController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim();

        if (accountService.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại");
        }

        Roles userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Hệ thống chưa cấu hình vai trò ROLE_USER"));

        Accounts acc = new Accounts();
        acc.setUsername(username);
        acc.setEmail(email);
        acc.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        acc.setIsActive(true);
        acc.setRole(userRole);
        acc.setCreatedAt(Instant.now());
        accountService.update(acc);
        accountService.updateCustomerProfile(
                username,
                request.getFullname().trim(),
                email,
                null,
                true);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Đăng ký thành công");
        body.put("username", username);
        return body;
    }
}
