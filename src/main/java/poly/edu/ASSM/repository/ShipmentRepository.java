package poly.edu.ASSM.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Shipments;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipments, Integer> {
    Optional<Shipments> findByOrder_Id(Integer orderId);
}
