package poly.edu.ASSM.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import poly.edu.ASSM.entity.Permissions;

public interface PermissionRepository extends JpaRepository<Permissions, Integer> {

    @Query("SELECT DISTINCT p FROM Permissions p JOIN p.roles r WHERE r.id = :roleId")
    List<Permissions> findByRoleId(@Param("roleId") Integer roleId);
}
