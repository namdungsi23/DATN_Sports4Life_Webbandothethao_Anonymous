package poly.edu.ASSM.Services.util;

import poly.edu.ASSM.Entity.Accounts;

public interface AuthService {
    Accounts login(String username, String password);

    void logout();

    Accounts getUser();

    boolean isLogin();

    boolean isAdmin();
}
