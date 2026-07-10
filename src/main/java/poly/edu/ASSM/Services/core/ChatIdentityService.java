package poly.edu.ASSM.Services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Entity.Employees;
import poly.edu.ASSM.Entity.Users;
import poly.edu.ASSM.Repository.AccountRepository;
import poly.edu.ASSM.Repository.EmployeeRepository;
import poly.edu.ASSM.Repository.UsersRepository;

/**
 * Resolve identity cho chat từ username (JWT principal).
 */
@Service
public class ChatIdentityService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    public Accounts requireAccount(String username) {
        if (username == null || username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản");
        }
        return account;
    }

    public Users requireUser(String username) {
        Accounts account = requireAccount(username);
        return usersRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Chỉ tài khoản khách hàng mới được sử dụng chat hỗ trợ"));
    }

    public Employees requireEmployee(String username) {
        Accounts account = requireAccount(username);
        return employeeRepository.findByAccount_Id(account.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Chỉ nhân viên mới truy cập được khu vực chat này"));
    }

    public boolean isEmployee(String username) {
        return findEmployee(username).isPresent();
    }

    public java.util.Optional<Employees> findEmployee(String username) {
        if (username == null || username.isBlank()) {
            return java.util.Optional.empty();
        }
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            return java.util.Optional.empty();
        }
        return employeeRepository.findByAccount_Id(account.getId());
    }

    public java.util.Optional<Users> findUser(String username) {
        if (username == null || username.isBlank()) {
            return java.util.Optional.empty();
        }
        Accounts account = accountRepository.findByUsername(username);
        if (account == null) {
            return java.util.Optional.empty();
        }
        return usersRepository.findByAccount_Id(account.getId());
    }
}
