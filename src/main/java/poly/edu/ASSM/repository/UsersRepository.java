package poly.edu.ASSM.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {

    @EntityGraph(attributePaths = { "account", "rank" })
    Optional<Users> findByAccount_Id(Long accountId);

    @EntityGraph(attributePaths = { "account", "rank" })
    Optional<Users> findFirstByPhone(String phone);

    long countByRank_Id(Integer rankId);
}
