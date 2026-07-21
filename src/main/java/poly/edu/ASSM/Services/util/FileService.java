package poly.edu.ASSM.Services.util;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
public interface FileService {
	  String upload(MultipartFile file);

	    void delete(String publicId);
	}


