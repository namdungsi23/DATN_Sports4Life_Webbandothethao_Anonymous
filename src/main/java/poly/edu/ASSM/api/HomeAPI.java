package poly.edu.ASSM.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.RoleRepository;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.core.AdminNotificationService;
import poly.edu.ASSM.Services.core.PublicProductService;
import poly.edu.ASSM.dto.request.PublicRegisterRequest;
import poly.edu.ASSM.exception.InvalidInputException;
import poly.edu.ASSM.security.PasswordPolicy;

@RestController
@RequestMapping("/api/public")
public class HomeAPI {

    @Autowired
    private PublicProductService publicProductService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AdminNotificationService adminNotificationService;

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of("message", "API is running", "status", "ok");
    }

    @GetMapping("/products")
    public Map<String, Object> products(
            @RequestParam(required = false) String cat,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Double min,
            @RequestParam(required = false) Double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "createDate") String sort,
            @RequestParam(defaultValue = "desc") String dir) {

        return publicProductService.getProductsPage(cat, keyword, min, max, page, sort, dir);
    }

    @GetMapping("/products/{id}")
    public Map<String, Object> productDetail(@org.springframework.web.bind.annotation.PathVariable Long id) {
        return publicProductService.getProductDetail(id);
    }

    @GetMapping("/brands")
    public Map<String, Object> brands() {
        return publicProductService.getBrands();
    }

    @GetMapping("/categories")
    public List<Map<String, String>> categories() {
        return publicProductService.getCategories();
    }

    /** Lớp 1+3+5: đăng ký công khai — validate + PasswordPolicy + BCrypt + ROLE_USER */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody PublicRegisterRequest request) {
        String pwdError = PasswordPolicy.validate(request.getPassword());
        if (pwdError != null) {
            throw new InvalidInputException(pwdError);
        }
        String username = request.getUsername().trim();
        if (accountService.findByUsername(username) != null) {
            throw new InvalidInputException("Tên đăng nhập đã tồn tại.");
        }
        String email = request.getEmail().trim();
        if (accountRepository.findFirstByEmailIgnoreCase(email) != null) {
            throw new InvalidInputException("Email đã được sử dụng.");
        }
        Accounts acc = new Accounts();
        acc.setUsername(username);
        acc.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        acc.setEmail(email);
        acc.setIsActive(true);
        acc.setRole(roleRepository.findByName("ROLE_USER").orElseThrow(
                () -> new InvalidInputException("Hệ thống chưa cấu hình ROLE_USER.")));
        accountService.update(acc);
        accountService.updateCustomerProfile(username, request.getFullname().trim(), request.getEmail().trim(), null, true);

        try {
            adminNotificationService.notifyPanelUsers(
                    "User mới đăng ký",
                    "Tài khoản mới: " + username + " (" + request.getEmail().trim() + ")",
                    "/admin/user?keyword=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(HomeAPI.class)
                    .warn("Gửi thông báo đăng ký thất bại: {}", ex.getMessage());
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("message", "Đăng ký thành công. Vui lòng đăng nhập.");
        return ResponseEntity.ok(body);
    }
}
