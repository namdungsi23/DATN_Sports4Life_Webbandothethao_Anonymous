package poly.edu.ASSM.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Accounts;

@Repository
public interface AccountRepository extends JpaRepository<Accounts, Long> {

    boolean existsByUsername(String username);

    Accounts findByUsername(String username);

    @Query("""
            SELECT a
            FROM Accounts a
            WHERE a.username LIKE %:keyword%
               OR a.fullName LIKE %:keyword%
               OR a.email LIKE %:keyword%
            """)
    Page<Accounts> search(@Param("keyword") String keyword, Pageable pageable);
}
