package poly.edu.ASSM.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Accounts;

@Repository
public interface AccountRepository extends JpaRepository<Accounts, Long> {

    boolean existsByUsername(String username);

    @EntityGraph(attributePaths = "role")
    Accounts findByUsername(String username);

    @EntityGraph(attributePaths = "role")
    Accounts findFirstByEmailIgnoreCase(String email);

    @Query("""
            SELECT a
            FROM Accounts a
            LEFT JOIN a.users u
            WHERE a.username LIKE %:keyword%
               OR u.fullName LIKE %:keyword%
               OR a.email LIKE %:keyword%
            """)
    Page<Accounts> search(@Param("keyword") String keyword, Pageable pageable);

    @EntityGraph(attributePaths = { "role", "users" })
    @Query("""
            SELECT a
            FROM Accounts a
            JOIN a.role r
            WHERE UPPER(COALESCE(r.name, '')) LIKE '%ADMIN%'
               OR UPPER(COALESCE(r.name, '')) LIKE '%STAFF%'
            """)
    List<Accounts> findPanelAccounts();
}
