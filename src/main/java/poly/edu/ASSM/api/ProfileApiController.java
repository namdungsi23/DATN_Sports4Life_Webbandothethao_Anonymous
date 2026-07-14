package poly.edu.ASSM.api;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import poly.edu.ASSM.Services.core.ProfileService;
import poly.edu.ASSM.dto.request.ChangePasswordRequest;
import poly.edu.ASSM.dto.request.ProfileUpdateRequest;

@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public Map<String, Object> getProfile(Principal principal) {
        return profileService.getProfile(principal.getName());
    }

    @PutMapping
    public Map<String, Object> updateProfile(
            Principal principal,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateProfile(principal.getName(), request);
    }

    @PostMapping("/avatar")
    public Map<String, Object> uploadAvatar(
            Principal principal,
            @RequestParam("file") MultipartFile file) {
        return profileService.uploadAvatar(principal.getName(), file);
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            Principal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        return profileService.changePassword(principal.getName(), request);
    }
}
