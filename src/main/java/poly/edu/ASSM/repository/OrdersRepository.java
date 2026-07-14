package poly.edu.ASSM.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import poly.edu.ASSM.entity.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {

    List<Orders> findByAccount_Username(String username);

    @EntityGraph(attributePaths = { "orderDetails", "orderDetails.variant", "orderDetails.variant.product" })
    List<Orders> findByAccount_UsernameOrderByCreateDateDesc(String username);

    @EntityGraph(attributePaths = { "orderDetails", "orderDetails.variant", "orderDetails.variant.product" })
    Optional<Orders> findByIdAndAccount_Username(Integer id, String username);

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

    @Query(value = """
            SELECT FORMAT(o.CreateDate, 'yyyy-MM') AS ym, SUM(o.TotalAmount) AS revenue
            FROM Orders o
            WHERE o.CreateDate >= :start
              AND o.OrderStatus <> :cancelled
            GROUP BY FORMAT(o.CreateDate, 'yyyy-MM')
            ORDER BY ym
            """, nativeQuery = true)
    List<Object[]> sumRevenueByMonthSince(@Param("start") Instant start, @Param("cancelled") String cancelled);

    @Query(value = """
            SELECT o.OrderStatus, COUNT(*) AS cnt
            FROM Orders o
            WHERE o.CreateDate >= :start
            GROUP BY o.OrderStatus
            """, nativeQuery = true)
    List<Object[]> countGroupByOrderStatusSince(@Param("start") Instant start);

    @Query(value = """
            SELECT TOP 8
                   a.Id,
                   a.Username,
                   u.FullName,
                   SUM(o.TotalAmount) AS spending,
                   COUNT(*) AS orderCount
            FROM Orders o
            INNER JOIN Accounts a ON a.Id = o.AccountId
            LEFT JOIN Users u ON u.AccountId = a.Id
            WHERE o.CreateDate >= :start
              AND o.OrderStatus <> :cancelled
            GROUP BY a.Id, a.Username, u.FullName
            ORDER BY spending DESC
            """, nativeQuery = true)
    List<Object[]> topCustomersSince(@Param("start") Instant start, @Param("cancelled") String cancelled);

    @Query("""
            SELECT DISTINCT o
            FROM Orders o
            LEFT JOIN o.account a
            LEFT JOIN o.orderAddresses addr
            WHERE (:idExact IS NOT NULL AND o.id = :idExact)
               OR LOWER(COALESCE(a.username, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(COALESCE(a.email, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(COALESCE(addr.receiverName, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
               OR LOWER(COALESCE(addr.receiverPhone, '')) LIKE LOWER(CONCAT('%', :kw, '%'))
            ORDER BY o.createDate DESC
            """)
    List<Orders> searchByKeyword(
            @Param("kw") String kw,
            @Param("idExact") Integer idExact,
            org.springframework.data.domain.Pageable pageable);
}
