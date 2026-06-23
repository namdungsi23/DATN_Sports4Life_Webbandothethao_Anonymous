package poly.edu.ASSM.Services.core;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import poly.edu.ASSM.dto.request.ProfileUpdateRequest;

public interface ProfileService {

    Map<String, Object> getProfile(String username);

    Map<String, Object> updateProfile(String username, ProfileUpdateRequest request);

    Map<String, Object> uploadAvatar(String username, MultipartFile file);
}
