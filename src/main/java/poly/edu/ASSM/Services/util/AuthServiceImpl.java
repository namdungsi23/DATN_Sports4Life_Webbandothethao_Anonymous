package poly.edu.ASSM.Services.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Services.core.AccountService;
import poly.edu.ASSM.Services.core.AdminAccessService;
import poly.edu.ASSM.Services.web.SessionService;
import poly.edu.ASSM.security.SpringRoleNames;

@Service
public class AuthServiceImpl implements AuthService {
    @Autowired
    AccountService accountService;

    @Autowired
    AdminAccessService adminAccessService;

    @Autowired
    SessionService sessionService;

    static final String USER_SESSION = "user";

    @Override
    public Accounts login(String username, String password) {
        Accounts user = accountService.findByUsername(username);

        if (user == null) {
            return null;
        }

        if (user.getPasswordHash() == null || !user.getPasswordHash().equals(password)) {
            return null;
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            return null;
        }

        AdminAccessService.AdminAccess access = adminAccessService.resolve(user);
        UserDetails userDetail = User.withUsername(username)
                .password(password)
                .authorities(adminAccessService.toAuthorities(access))
                .build();

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetail, null, userDetail.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        sessionService.setAttribute(USER_SESSION, user);

        return user;
    }

    @Override
    public void logout() {
        sessionService.removeAttribute(USER_SESSION);
    }

    @Override
    public Accounts getUser() {
        return sessionService.getAttribute(USER_SESSION, Accounts.class);
    }

    @Override
    public boolean isLogin() {
        return getUser() != null;
    }

    @Override
    public boolean isAdmin() {
        Accounts user = getUser();
        return user != null
                && adminAccessService.resolve(user).roles().contains(SpringRoleNames.normalize("ADMIN"));
    }
}
