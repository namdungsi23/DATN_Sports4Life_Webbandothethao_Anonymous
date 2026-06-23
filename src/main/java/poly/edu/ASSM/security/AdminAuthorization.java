package poly.edu.ASSM.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import poly.edu.ASSM.Services.core.AdminAccessService;

@Component("adminAuth")
public class AdminAuthorization {

    @Autowired
    private AdminAccessService adminAccessService;

    public boolean has(String permission) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return false;
        }
        AdminAccessService.AdminAccess access = adminAccessService.resolve(auth.getName());
        return adminAccessService.hasPermission(access, permission);
    }
}
