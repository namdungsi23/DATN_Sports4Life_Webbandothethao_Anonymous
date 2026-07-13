package poly.edu.ASSM.Services.util;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

	private static final Pattern VARIANT_PATTERN =
			Pattern.compile("_(\\d+)(?:_|$)");

	@Autowired
	Cloudinary cloudinary;
	
	public String uploadImage(MultipartFile file) {
		String original = file.getOriginalFilename();
		String fileName = original.substring(0, original.lastIndexOf('.'));

		// Kiểm tra kiểu file - Đúng định dạng là ảnh
		String contentType = file.getContentType();

		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
		}

		try {
			Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
				ObjectUtils.asMap( "folder", this.uploadFolderBaseName(file),
						           "resource_type", "image", 
						           //"format", "jpg",
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

	public String uploadFolderBaseName (MultipartFile file) {
		String original = file.getOriginalFilename();

		if (original == null || !original.contains(".")) {
			throw new IllegalArgumentException("Tên file không hợp lệ.");
		}

		String fileName = original.substring(0, original.lastIndexOf('.'));

		String baseName = fileName.replaceFirst("_\\d+(_.*)?$", "").trim();
		String baseNameWithoutUnderscore = baseName.replace("_", " ");

		String variantName = "variant";

		Matcher matcher = VARIANT_PATTERN.matcher(fileName);

		if(matcher.find()) {
			variantName += "_"+ matcher.group(1);
		}

		return baseNameWithoutUnderscore + "/" + variantName;
	}

 }
