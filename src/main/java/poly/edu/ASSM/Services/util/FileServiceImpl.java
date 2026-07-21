package poly.edu.ASSM.Services.util;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
public class FileServiceImpl implements FileService {

	@Autowired
	Cloudinary cloudinary;
	@Override
	public String upload(MultipartFile file) {
		try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "ASSM",
                            "resource_type", "image"
                    )
            );

            // URL ảnh
            return result.get("secure_url").toString();

        } catch (IOException e) {
            throw new RuntimeException("Upload file failed", e);
        }
	}

	@Override
	public void delete(String publicId) {
		try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Delete file failed", e);
        }
    }
	}


