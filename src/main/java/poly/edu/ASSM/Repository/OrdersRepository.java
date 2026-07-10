package poly.edu.ASSM.Repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    List<Orders> findByAccount_Username(String username);

    @Query("""
            SELECT o
            FROM Orders o
            WHERE o.createDate >= :start
              AND o.createDate < :end
            """)
    List<Orders> findByCreateDateRange(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COUNT(o)
            FROM Orders o
            WHERE o.createDate >= :start
              AND o.createDate < :end
            """)
    long countTodayOrders(@Param("start") Instant start, @Param("end") Instant end);

    @Query("""
            SELECT COUNT(o)
            FROM Orders o
            WHERE o.createDate >= :start
              AND o.createDate < :end
              AND o.orderStatus = :status
            """)
    long countTodayOrdersByStatus(
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("status") String status);

    long countByOrderStatus(String orderStatus);

    List<Orders> findByOrderStatusOrderByCreateDateDesc(String orderStatus);
}
