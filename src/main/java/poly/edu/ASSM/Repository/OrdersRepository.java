package poly.edu.ASSM.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.Entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    List<Orders> findByUsername(String username);

    List<Orders> findByCreateDate(LocalDate date);

    List<Orders> findByCreateDateBetween(LocalDate from, LocalDate to);

    @Query("SELECT COUNT(o) FROM Orders o WHERE o.createDate = :today")
    long countTodayOrders(@Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(o)
            FROM Orders o
            WHERE o.createDate = :today
              AND o.status = :status
            """)
    long countTodayOrdersByStatus(
            @Param("today") LocalDate today,
            @Param("status") String status);
}
