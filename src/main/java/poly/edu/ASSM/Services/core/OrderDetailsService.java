package poly.edu.ASSM.Services.core;

import java.util.List;

import poly.edu.ASSM.entity.OrderDetails;

public interface OrderDetailsService {
    List<OrderDetails> findAll();

    OrderDetails findById(int id);

    OrderDetails create(OrderDetails detail);

    OrderDetails update(OrderDetails detail);

    void delete(int id);

    List<OrderDetails> findByOrder(int orderId);

    void deleteByOrder(int orderId);
}
