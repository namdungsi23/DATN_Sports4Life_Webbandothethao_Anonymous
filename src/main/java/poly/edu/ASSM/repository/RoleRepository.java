package poly.edu.ASSM.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.entity.Roles;

public interface RoleRepository extends JpaRepository<Roles, Integer> {

    Optional<Roles> findByName(String name);

    @EntityGraph(attributePaths = "permissions")
    Optional<Roles> findWithPermissionsById(Integer id);
}
