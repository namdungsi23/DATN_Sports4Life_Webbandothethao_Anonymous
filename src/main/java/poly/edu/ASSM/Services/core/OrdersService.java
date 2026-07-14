package poly.edu.ASSM.Services.core;

import java.time.LocalDate;
import java.util.List;

import poly.edu.ASSM.entity.Orders;

public interface OrdersService {
    List<Orders> findAll();

    Orders findById(int id);

    Orders create(Orders order);

    Orders update(Orders order);

    void delete(int id);

    List<Orders> findByUsername(String username);

    List<Orders> findByDate(LocalDate date);

    List<Orders> findByDateRange(LocalDate from, LocalDate to);

    Orders updateStatus(int orderId, String status);

    long countTodayOrders();

    long countPendingOrders();

    List<Orders> findByOrderStatus(String status);
}
