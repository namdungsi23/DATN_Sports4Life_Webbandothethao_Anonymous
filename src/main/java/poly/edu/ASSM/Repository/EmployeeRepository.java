package poly.edu.ASSM.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Employees;

@Repository
public interface EmployeeRepository extends JpaRepository<Employees, Long> {

    @EntityGraph(attributePaths = { "account", "position" })
    Optional<Employees> findByAccount_Id(Long accountId);
}
