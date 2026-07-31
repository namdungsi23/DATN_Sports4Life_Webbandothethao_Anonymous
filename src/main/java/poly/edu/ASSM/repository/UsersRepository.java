package poly.edu.ASSM.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {

    @EntityGraph(attributePaths = { "account", "rank" })
    Optional<Users> findByAccount_Id(Long accountId);

    @EntityGraph(attributePaths = { "account", "rank" })
    Optional<Users> findFirstByPhone(String phone);

    long countByRank_Id(Integer rankId);

    /** User đã bật nhận tin voucher qua Gmail. */
    @EntityGraph(attributePaths = { "account" })
    @Query("""
            SELECT u FROM Users u
            JOIN u.account a
            WHERE u.newsletterOptIn = true
              AND a.isActive = true
              AND a.email IS NOT NULL
              AND a.email <> ''
            """)
    List<Users> findNewsletterSubscribers();

    /** Khách hàng (không phải ADMIN/STAFF) — nhận chuông thông báo trên trang user. */
    @EntityGraph(attributePaths = { "account", "account.role" })
    @Query("""
            SELECT u FROM Users u
            JOIN u.account a
            JOIN a.role r
            WHERE a.isActive = true
              AND UPPER(COALESCE(r.name, '')) NOT LIKE '%ADMIN%'
              AND UPPER(COALESCE(r.name, '')) NOT LIKE '%STAFF%'
            """)
    List<Users> findCustomerUsers();
}
