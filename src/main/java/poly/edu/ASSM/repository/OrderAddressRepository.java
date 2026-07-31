package poly.edu.ASSM.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.OrderAddresses;

@Repository
public interface OrderAddressRepository extends JpaRepository<OrderAddresses, Integer> {

    Optional<OrderAddresses> findByOrder_Id(Integer orderId);
}
