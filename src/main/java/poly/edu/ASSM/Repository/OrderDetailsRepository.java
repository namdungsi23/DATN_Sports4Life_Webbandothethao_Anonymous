package poly.edu.ASSM.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.OrderDetails;

@Repository
public interface OrderDetailsRepository extends JpaRepository<OrderDetails, Integer> {

    List<OrderDetails> findByOrders_Id(Integer orderId);

    @Query("""
            SELECT SUM(od.quantity)
            FROM OrderDetails od
            WHERE od.orders.id = :orderId
            """)
    Integer sumQuantityByOrder(@Param("orderId") Integer orderId);

    void deleteByOrders_Id(Integer orderId);
}
