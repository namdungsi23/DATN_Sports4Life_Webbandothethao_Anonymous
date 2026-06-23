package poly.edu.ASSM.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Users;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {

    @EntityGraph(attributePaths = "roles")
    Optional<Users> findByAccount_Id(Long accountId);
}
