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

    long countByRank_Id(Integer rankId);

    @Query("SELECT u.rank.id, COUNT(u) FROM Users u WHERE u.rank IS NOT NULL GROUP BY u.rank.id")
    List<Object[]> countMembersGroupByRank();
}
