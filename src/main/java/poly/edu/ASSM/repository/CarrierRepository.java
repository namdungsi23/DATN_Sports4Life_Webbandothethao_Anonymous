package poly.edu.ASSM.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Carriers;

@Repository
public interface CarrierRepository extends JpaRepository<Carriers, Integer> {
    List<Carriers> findByActiveTrueOrderByNameAsc();
}
