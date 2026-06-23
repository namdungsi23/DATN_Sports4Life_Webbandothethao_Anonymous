package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.Accounts;
import poly.edu.ASSM.Repository.AccountRepository;

@Service
public class AccountsServiceImpl implements AccountService {

    @Autowired
    AccountRepository repo;

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
    public Accounts update(Accounts acc) {
        Accounts exist = acc.getId() != null ? repo.findById(acc.getId()).orElse(null) : null;

        if (exist == null) {
            Accounts newAcc = new Accounts();
            newAcc.setUsername(acc.getUsername());
            newAcc.setEmail(acc.getEmail());
            newAcc.setFullName(acc.getFullName());
            newAcc.setPasswordHash(acc.getPasswordHash());
            newAcc.setAvatar(acc.getAvatar());
            newAcc.setIsActive(true);
            newAcc.setAdmin(false);
            return repo.save(newAcc);
        }

        exist.setFullName(acc.getFullName());
        exist.setEmail(acc.getEmail());
        exist.setAvatar(acc.getAvatar());
        exist.setIsActive(acc.getIsActive());
        return repo.save(exist);
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
}
