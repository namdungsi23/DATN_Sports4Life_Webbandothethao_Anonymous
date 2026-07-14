package poly.edu.ASSM.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.PublicProductService;
import poly.edu.ASSM.Services.core.RegisterVerificationService;
import poly.edu.ASSM.dto.request.PublicRegisterRequest;
import poly.edu.ASSM.dto.request.RegisterResendOtpRequest;
import poly.edu.ASSM.dto.request.RegisterVerifyOtpRequest;

@RestController
@RequestMapping("/api/public")
public class HomeAPI {

    @Autowired
    private PublicProductService publicProductService;

    @Autowired
    private RegisterVerificationService registerVerificationService;

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

    /** Đăng ký JSON (không ảnh). */
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody PublicRegisterRequest request) {
        return ResponseEntity.ok(registerVerificationService.register(request));
    }

    /** Đăng ký multipart — ảnh đại diện lên Cloudinary rồi lưu Users.Avatar (SQL). */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> registerWithPhoto(
            @Valid @ModelAttribute PublicRegisterRequest request,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        return ResponseEntity.ok(registerVerificationService.register(request, photo));
    }

    @PostMapping("/register/verify-otp")
    public ResponseEntity<Map<String, Object>> registerVerifyOtp(@Valid @RequestBody RegisterVerifyOtpRequest request) {
        return ResponseEntity.ok(registerVerificationService.verifyOtp(request));
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<Map<String, Object>> registerResendOtp(@Valid @RequestBody RegisterResendOtpRequest request) {
        return ResponseEntity.ok(registerVerificationService.resendOtp(
                request.getUsername(),
                request.getVerifyChannel(),
                request.getEmail(),
                request.getPhone()));
    }
}
