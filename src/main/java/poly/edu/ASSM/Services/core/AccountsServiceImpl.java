package poly.edu.ASSM.Services.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import poly.edu.ASSM.entity.Accounts;
import poly.edu.ASSM.entity.Ranks;
import poly.edu.ASSM.entity.Roles;
import poly.edu.ASSM.entity.Users;
import poly.edu.ASSM.repository.AccountRepository;
import poly.edu.ASSM.repository.RankRepository;
import poly.edu.ASSM.repository.RoleRepository;
import poly.edu.ASSM.repository.UsersRepository;
import poly.edu.ASSM.security.SpringRoleNames;

@Service
public class AccountsServiceImpl implements AccountService {

    private static final String ROLE_USER = "ROLE_USER";

    @Autowired
    private AccountRepository repo;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RankRepository rankRepository;

    @Override
    public List<Accounts> findAll() {
        return repo.findAll();
    }

    @Override
    public Page<Accounts> findAll(int page, int size) {
        return repo.findAll(PageRequest.of(page, size));
    }

    @Override
    public Accounts findByUsername(String username) {
        return repo.findByUsername(username);
    }

    @Override
    @Transactional
    public Accounts update(Accounts acc) {
        if (acc == null || acc.getUsername() == null || acc.getUsername().isBlank()) {
            return null;
        }

        Accounts exist = acc.getId() != null ? repo.findById(acc.getId()).orElse(null) : null;
        if (exist == null) {
            exist = repo.findByUsername(acc.getUsername());
        }

        if (exist == null) {
            return createAccount(acc);
        }

        if (acc.getEmail() != null) {
            exist.setEmail(acc.getEmail());
        }
        if (acc.getPasswordHash() != null) {
            exist.setPasswordHash(acc.getPasswordHash());
        }
        if (acc.getIsActive() != null) {
            exist.setIsActive(acc.getIsActive());
        }
        if (acc.getRole() != null) {
            exist.setRole(acc.getRole());
        }
        exist.setUpdatedAt(Instant.now());
        return repo.save(exist);
    }

    @Override
    @Transactional
    public Accounts saveOAuthLogin(String email, String fullName, String avatarUrl) {
        Accounts account = repo.findByUsername(email);
        if (account == null) {
            account = new Accounts();
            account.setUsername(email);
            account.setEmail(email);
            account.setPasswordHash(null);
            account.setIsActive(true);
            account.setRole(requireRole(ROLE_USER));
            account.setCreatedAt(Instant.now());
            account = repo.save(account);
            createCustomerProfile(account, fullName, avatarUrl);
            return account;
        }

        updateCustomerProfile(account, fullName, avatarUrl);
        account.setUpdatedAt(Instant.now());
        return repo.save(account);
    }

    @Override
    @Transactional
    public Accounts updateCustomerProfile(String username, String fullName, String email, String avatar,
            Boolean isActive) {
        Accounts account = repo.findByUsername(username);
        if (account == null) {
            return null;
        }

        if (email != null) {
            account.setEmail(email);
        }
        if (isActive != null) {
            account.setIsActive(isActive);
        }

        updateCustomerProfile(account, fullName, avatar);
        account.setUpdatedAt(Instant.now());
        return repo.save(account);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    public Accounts login(String username, String password) {
        Accounts acc = repo.findByUsername(username);
        if (acc == null) {
            return null;
        }
        if (!Boolean.TRUE.equals(acc.getIsActive())) {
            return null;
        }
        if (acc.getPasswordHash() == null || !acc.getPasswordHash().equals(password)) {
            return null;
        }
        return acc;
    }

    @Override
    public Page<Accounts> search(String keyword, int page, int size) {
        return repo.search(keyword, PageRequest.of(page, size));
    }

    private Accounts createAccount(Accounts acc) {
        Accounts newAcc = new Accounts();
        newAcc.setUsername(acc.getUsername());
        newAcc.setEmail(acc.getEmail());
        newAcc.setPasswordHash(acc.getPasswordHash());
        newAcc.setIsActive(acc.getIsActive() != null ? acc.getIsActive() : true);
        newAcc.setRole(acc.getRole() != null ? acc.getRole() : requireRole(ROLE_USER));
        newAcc.setCreatedAt(Instant.now());
        newAcc = repo.save(newAcc);
        if (isCustomerRole(newAcc.getRole())) {
            createCustomerProfile(newAcc, null, null);
        }
        return newAcc;
    }

    private void createCustomerProfile(Accounts account, String fullName, String avatarUrl) {
        Users user = newUsersProfile(account);
        user.setFullName(fullName);
        user.setAvatar(avatarUrl);
        usersRepository.save(user);
    }

    private void updateCustomerProfile(Accounts account, String fullName, String avatarUrl) {
        Users user = usersRepository.findByAccount_Id(account.getId()).orElseGet(() -> newUsersProfile(account));

        if (fullName != null) {
            user.setFullName(fullName);
        }
        if (avatarUrl != null) {
            user.setAvatar(avatarUrl);
        }
        user.setUpdatedAt(Instant.now());
        usersRepository.save(user);
    }

    private Roles requireRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
    }

    private Users newUsersProfile(Accounts account) {
        Users user = new Users();
        user.setAccount(account);
        user.setGender(0);
        user.setTotalPoint(0);
        user.setTotalSpending(BigDecimal.ZERO);
        user.setRank(requireDefaultRank());
        user.setCreatedAt(Instant.now());
        return user;
    }

    private Ranks requireDefaultRank() {
        return rankRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException("Default rank not found"));
    }

    private boolean isCustomerRole(Roles role) {
        if (role == null || role.getName() == null) {
            return false;
        }
        return SpringRoleNames.normalize("USER").equals(SpringRoleNames.normalize(role.getName()));
    }
}
