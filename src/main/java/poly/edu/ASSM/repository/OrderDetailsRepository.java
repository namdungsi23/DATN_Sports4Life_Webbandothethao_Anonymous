package poly.edu.ASSM.Repository;

import java.time.Instant;
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

    @Query(value = """
            SELECT TOP 8
                   p.Id,
                   p.Name,
                   SUM(od.Quantity) AS qty,
                   SUM(CAST(od.Price AS DECIMAL(18,2)) * od.Quantity) AS revenue
            FROM OrderDetails od
            INNER JOIN Orders o ON o.Id = od.OrderId
            INNER JOIN ProductVariants pv ON pv.Id = od.VariantId
            INNER JOIN Products p ON p.Id = pv.ProductId
            WHERE o.CreateDate >= :start
              AND o.OrderStatus <> :cancelled
            GROUP BY p.Id, p.Name
            ORDER BY qty DESC
            """, nativeQuery = true)
    List<Object[]> topProductsSince(@Param("start") Instant start, @Param("cancelled") String cancelled);

    @Query(value = """
            SELECT TOP 8
                   c.Name,
                   SUM(CAST(od.Price AS DECIMAL(18,2)) * od.Quantity) AS revenue
            FROM OrderDetails od
            INNER JOIN Orders o ON o.Id = od.OrderId
            INNER JOIN ProductVariants pv ON pv.Id = od.VariantId
            INNER JOIN Products p ON p.Id = pv.ProductId
            INNER JOIN Categories c ON c.Id = p.CategoryId
            WHERE o.CreateDate >= :start
              AND o.OrderStatus <> :cancelled
            GROUP BY c.Name
            ORDER BY revenue DESC
            """, nativeQuery = true)
    List<Object[]> sumRevenueByCategorySince(@Param("start") Instant start, @Param("cancelled") String cancelled);
}
