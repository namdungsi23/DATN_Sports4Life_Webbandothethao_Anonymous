package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.data.domain.Page;

import poly.edu.ASSM.Entity.Accounts;

public interface AccountService {

    List<Accounts> findAll();

    Page<Accounts> findAll(int page, int size);

    Accounts findByUsername(String username);

    Accounts update(Accounts account);

    void delete(Long id);

    Accounts login(String username, String password);

    Page<Accounts> search(String keyword, int page, int size);
}
