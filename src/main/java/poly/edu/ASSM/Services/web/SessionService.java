package poly.edu.ASSM.Services.web;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
@Service
public class SessionService {
	    @Autowired
	    HttpSession session;

	    // Set session
	    public void setAttribute(String name, Object value) {
	        session.setAttribute(name, value);
	    }

	    // Get session (generic)
	    @SuppressWarnings("unchecked")
	    public <T> T getAttribute(String name) {
	        return (T) session.getAttribute(name);
	    }

	    // Get session với kiểu dữ liệu rõ ràng (an toàn hơn)
	    public <T> T getAttribute(String name, Class<T> type) {
	        Object value = session.getAttribute(name);
	        if (value == null) return null;
	        return type.cast(value);
	    }

	    // Remove 1 session
	    public void removeAttribute(String name) {
	        session.removeAttribute(name);
	    }

	    // Kiểm tra session tồn tại
	    public boolean exists(String name) {
	        return session.getAttribute(name) != null;
	    }

	    // Xóa toàn bộ session (logout)
	    public void clear() {
	        session.invalidate();
	    }

	    // Lấy session id (debug hoặc bảo mật)
	    public String getSessionId() {
	        return session.getId();
	    }
	}


