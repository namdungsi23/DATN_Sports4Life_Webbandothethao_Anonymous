package poly.edu.ASSM.Services.util;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {
	@Autowired
	Cloudinary cloudinary;
	
	public String uploadImage(MultipartFile file) {
		String original = file.getOriginalFilename();
		String fileName = original.substring(0, original.lastIndexOf('.'));
		try {
			Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
				ObjectUtils.asMap( "folder", "product", 
						           "resource_type", "image", 
						           "format", "jpg",
						           "public_id", fileName,
						            "overwrite", true));
			return uploadResult.get("secure_url").toString();
		}catch(IOException e) {
			throw new RuntimeException("Upload image failed", e);
		}
	}

	public String uploadAvatar(MultipartFile file) {
		String original = file.getOriginalFilename();
		if (original == null || !original.contains(".")) {
			original = "avatar.jpg";
		}
		String fileName = "avatar_" + System.currentTimeMillis();
		try {
			Map<?, ?> uploadResult = cloudinary.uploader().upload(
					file.getBytes(),
					ObjectUtils.asMap(
							"folder", "avatars",
							"resource_type", "image",
							"public_id", fileName,
							"overwrite", true));
			return uploadResult.get("secure_url").toString();
		} catch (IOException e) {
			throw new RuntimeException("Upload avatar failed", e);
		}
	}
}
