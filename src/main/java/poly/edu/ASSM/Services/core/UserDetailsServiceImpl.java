package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.Accounts;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private AccountsServiceImpl accService;

    @Autowired
    private AdminAccessService adminAccessService;

    @Override
    public UserDetails loadUserByUsername(String username) {
        Accounts account = accService.findByUsername(username);

        if (account == null) {
            throw new UsernameNotFoundException("User not found");
        }

        AdminAccessService.AdminAccess access = adminAccessService.resolve(account);
        String password = account.getPasswordHash() != null ? account.getPasswordHash() : "";

        return new User(
                account.getUsername(),
                password,
                adminAccessService.toAuthorities(access));
    }
}
