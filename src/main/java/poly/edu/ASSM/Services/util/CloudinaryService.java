package poly.edu.ASSM.Services.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CloudinaryService {

	public record VariantImageUpload(String secureUrl, String detectedColor) {
	}

	private static final Pattern VARIANT_PATTERN =
			Pattern.compile("_(\\d+)(?:_|$)");

	@Autowired
	Cloudinary cloudinary;

	/**
	 * Lấy ArrayList URL ảnh trong folder Cloudinary (kèm subfolder, vd: product/sp1/variant_1).
	 */
	public List<String> listFolderImageUrls(String folderPath) {
		List<String> urls = new ArrayList<>();
		if (folderPath == null || folderPath.isBlank()) {
			return urls;
		}
		String folder = folderPath.trim().replaceAll("^/+|/+$", "");
		try {
			String nextCursor = null;
			do {
				Map<String, Object> options = ObjectUtils.asMap(
						"type", "upload",
						"prefix", folder.endsWith("/") ? folder : folder + "/",
						"max_results", 100);
				if (nextCursor != null) {
					options.put("next_cursor", nextCursor);
				}
				Map<?, ?> result = cloudinary.api().resources(options);
				Object resourcesObj = result.get("resources");
				if (resourcesObj instanceof List<?> resources) {
					for (Object item : resources) {
						if (!(item instanceof Map<?, ?> resource)) {
							continue;
						}
						Object url = resource.get("secure_url");
						if (url == null) {
							url = resource.get("url");
						}
						if (url != null) {
							String s = String.valueOf(url).trim();
							if (!s.isEmpty() && !urls.contains(s)) {
								urls.add(s);
							}
						}
					}
				}
				Object cursor = result.get("next_cursor");
				nextCursor = cursor != null ? String.valueOf(cursor) : null;
			} while (nextCursor != null && !nextCursor.isBlank());
		} catch (Exception e) {
			throw new RuntimeException("List Cloudinary folder failed: " + folder, e);
		}
		return urls;
	}

	/** ArrayList ảnh theo tên sản phẩm: folder {@code product/{productName}} (+ subfolder). */
	public List<String> listProductFolderImageUrls(String productName) {
		if (productName == null || productName.isBlank()) {
			return new ArrayList<>();
		}
		List<String> urls = listFolderImageUrls("product/" + productName.trim());
		if (urls.isEmpty() && productName.trim().matches("(?i)sp\\d+")) {
			urls = listFolderImageUrls(productName.trim().toLowerCase());
		}
		return urls;
	}

	/** ArrayList ảnh asset gốc {@code sp{id}/...} (upload ngoài folder product). */
	public List<String> listProductAssetImageUrls(Long productId) {
		if (productId == null) {
			return new ArrayList<>();
		}
		List<String> urls = listFolderImageUrls("product/sp" + productId);
		if (urls.isEmpty()) {
			urls = listFolderImageUrls("sp" + productId);
		}
		return urls;
	}

	/**
	 * Upload ảnh biến thể theo chuẩn {@code Sp{productId}_{variantNo}_{sortOrder}}.
	 * Folder: {@code product/sp{id}/variant_{n}}
	 */
	@SuppressWarnings("rawtypes")
	public VariantImageUpload uploadVariantImage(
			MultipartFile file,
			String productName,
			String colorHint,
			Long productId,
			int variantNo,
			int sortOrder) {
		String original = file.getOriginalFilename();
		if (original == null || !original.contains(".")) {
			throw new IllegalArgumentException("Tên file không hợp lệ.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
		}

		Optional<String> fromName = ImageColorUtils.fromFilename(original);
		String detected = fromName.orElse(null);

		byte[] bytes;
		try {
			bytes = file.getBytes();
		} catch (IOException e) {
			throw new RuntimeException("Upload image failed", e);
		}

		if (detected == null) {
			detected = ImageColorUtils.fromImageBytes(bytes).orElse(null);
		}

		String folder;
		String publicId;
		if (productId != null && productId > 0 && variantNo > 0 && sortOrder > 0) {
			folder = "product/sp" + productId + "/variant_" + variantNo;
			publicId = "sp" + productId + "_" + variantNo + "_" + sortOrder;
		} else {
			String colorFolder = ImageColorUtils.sanitizeFolderSegment(
					!ImageColorUtils.isPlaceholder(colorHint) ? colorHint
							: (detected != null ? detected : "variant"));
			String productFolder = ImageColorUtils.sanitizeFolderSegment(
					productName != null && !productName.isBlank() ? productName : uploadFolderBaseName(file));
			folder = productName != null && !productName.isBlank()
					? "product/" + productFolder + "/" + colorFolder
					: uploadFolderBaseName(file);
			publicId = original.substring(0, original.lastIndexOf('.'));
		}

		try {
			Map uploadResult = cloudinary.uploader().upload(bytes,
					ObjectUtils.asMap(
							"folder", folder,
							"resource_type", "image",
							"public_id", publicId,
							"overwrite", true,
							"colors", true));
			String url = uploadResult.get("secure_url").toString();
			if (detected == null) {
				detected = ImageColorUtils.fromCloudinaryColors(uploadResult.get("colors"))
						.orElse(null);
			}
			if (detected == null && !ImageColorUtils.isPlaceholder(colorHint)) {
				detected = colorHint.trim();
			}
			return new VariantImageUpload(url, detected);
		} catch (IOException e) {
			throw new RuntimeException("Upload image failed", e);
		}
	}

	public VariantImageUpload uploadVariantImage(MultipartFile file, String productName, String colorHint) {
		return uploadVariantImage(file, productName, colorHint, null, 0, 0);
	}

	/**
	 * Upload ảnh từ URL ngoài vào Cloudinary (đồng bộ kho file trước khi ghi SQL).
	 * Folder: {@code product/sp{id}/variant_{n}} — public_id {@code sp{id}_{n}_{sort}}.
	 */
	@SuppressWarnings("rawtypes")
	public VariantImageUpload uploadRemoteImageUrl(
			String sourceUrl,
			Long productId,
			int variantNo,
			int sortOrder) {
		if (sourceUrl == null || sourceUrl.isBlank()) {
			throw new IllegalArgumentException("URL ảnh không hợp lệ.");
		}
		if (productId == null || productId <= 0 || variantNo <= 0 || sortOrder <= 0) {
			throw new IllegalArgumentException("Thiếu productId/variantNo/sortOrder để đồng bộ Cloudinary.");
		}
		String folder = "product/sp" + productId + "/variant_" + variantNo;
		String publicId = "sp" + productId + "_" + variantNo + "_" + sortOrder;
		try {
			Map uploadResult = cloudinary.uploader().upload(sourceUrl.trim(),
					ObjectUtils.asMap(
							"folder", folder,
							"resource_type", "image",
							"public_id", publicId,
							"overwrite", true,
							"colors", true));
			String url = uploadResult.get("secure_url").toString();
			String detected = ImageColorUtils.fromCloudinaryColors(uploadResult.get("colors")).orElse(null);
			return new VariantImageUpload(url, detected);
		} catch (IOException e) {
			throw new RuntimeException("Upload ảnh từ URL thất bại", e);
		}
	}

	public String uploadImage(MultipartFile file) {
		return uploadVariantImage(file, null, null).secureUrl();
	}

	/**
	 * Xóa asset trên Cloudinary theo secure_url (đồng bộ với SQL khi admin xóa ảnh).
	 * URL ngoài Cloudinary thì bỏ qua.
	 */
	public void deleteByImageUrl(String imageUrl) {
		String publicId = extractPublicId(imageUrl);
		if (publicId == null || publicId.isBlank()) {
			return;
		}
		try {
			cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
		} catch (Exception e) {
			throw new RuntimeException("Xóa ảnh Cloudinary thất bại: " + publicId, e);
		}
	}

	/** Từ URL dạng .../upload/v123/product/sp1/variant_1/sp1_1_1.jpg → product/sp1/variant_1/sp1_1_1 */
	public static String extractPublicId(String imageUrl) {
		if (imageUrl == null || imageUrl.isBlank()) {
			return null;
		}
		String url = imageUrl.trim();
		int uploadIdx = url.indexOf("/upload/");
		if (uploadIdx < 0 || !url.contains("res.cloudinary.com")) {
			return null;
		}
		String path = url.substring(uploadIdx + "/upload/".length());
		// bỏ version v123456 / transforms
		String[] parts = path.split("/");
		int start = 0;
		while (start < parts.length) {
			String p = parts[start];
			if (p.matches("v\\d+") || p.contains(",") || p.matches("[a-z]_\\w+")) {
				start++;
				continue;
			}
			break;
		}
		if (start >= parts.length) {
			return null;
		}
		StringBuilder id = new StringBuilder();
		for (int i = start; i < parts.length; i++) {
			if (i > start) {
				id.append('/');
			}
			id.append(parts[i]);
		}
		String publicId = id.toString();
		int dot = publicId.lastIndexOf('.');
		if (dot > 0) {
			publicId = publicId.substring(0, dot);
		}
		return publicId.isBlank() ? null : publicId;
	}

	public String uploadAvatar(MultipartFile file) {
		return uploadAvatar(file, null);
	}

	/**
	 * Upload avatar lên Cloudinary: folder {@code avatars}, public_id {@code avatar_{username}}.
	 */
	@SuppressWarnings("rawtypes")
	public String uploadAvatar(MultipartFile file, String username) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Vui lòng chọn ảnh đại diện.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Chỉ chấp nhận file ảnh.");
		}
		String publicId = avatarPublicId(username);
		try {
			Map uploadResult = cloudinary.uploader().upload(
					file.getBytes(),
					ObjectUtils.asMap(
							"folder", "avatars",
							"resource_type", "image",
							"public_id", publicId,
							"overwrite", true,
							"invalidate", true));
			return uploadResult.get("secure_url").toString();
		} catch (IOException e) {
			throw new RuntimeException("Upload avatar failed", e);
		}
	}

	/**
	 * Đồng bộ URL ngoài (Drive/http) vào Cloudinary rồi trả secure_url để ghi SQL.
	 */
	@SuppressWarnings("rawtypes")
	public String uploadAvatarFromUrl(String sourceUrl, String username) {
		if (sourceUrl == null || sourceUrl.isBlank()) {
			throw new IllegalArgumentException("URL ảnh không hợp lệ.");
		}
		String publicId = avatarPublicId(username);
		try {
			Map uploadResult = cloudinary.uploader().upload(
					sourceUrl.trim(),
					ObjectUtils.asMap(
							"folder", "avatars",
							"resource_type", "image",
							"public_id", publicId,
							"overwrite", true,
							"invalidate", true));
			return uploadResult.get("secure_url").toString();
		} catch (IOException e) {
			throw new RuntimeException("Upload avatar từ URL thất bại", e);
		}
	}

	public boolean isOurCloudinaryUrl(String url) {
		return url != null && url.contains("res.cloudinary.com/pnam233/");
	}

	private static String avatarPublicId(String username) {
		String safe = username == null ? "" : username.trim().toLowerCase()
				.replaceAll("[^a-z0-9._-]", "_");
		if (safe.isBlank()) {
			safe = "user_" + System.currentTimeMillis();
		}
		return "avatar_" + safe;
	}

	public String uploadFolderBaseName(MultipartFile file) {
		String original = file.getOriginalFilename();

		if (original == null || !original.contains(".")) {
			throw new IllegalArgumentException("Tên file không hợp lệ.");
		}

		String fileName = original.substring(0, original.lastIndexOf('.'));

		String baseName = fileName.replaceFirst("_\\d+(_.*)?$", "").trim();
		String baseNameWithoutUnderscore = baseName.replace("_", " ");

		String variantName = "variant";

		Matcher matcher = VARIANT_PATTERN.matcher(fileName);

		if (matcher.find()) {
			variantName += "_" + matcher.group(1);
		}

		return baseNameWithoutUnderscore + "/" + variantName;
	}

}
