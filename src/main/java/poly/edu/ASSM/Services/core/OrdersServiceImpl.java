package poly.edu.ASSM.Services.core;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.entity.Orders;
import poly.edu.ASSM.repository.OrdersRepository;
import poly.edu.ASSM.domain.OrderStatus;

@Service
public class OrdersServiceImpl implements OrdersService {

	@Autowired
	OrdersRepository repo;

	@Override
    public List<Orders> findAll() {
        return repo.findAll();
    }

    @Override
    public Orders findById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng ID = " + id));
    }

    @Override
    public Orders create(Orders order) {
        if (order.getCreateDate() == null) {
            order.setCreateDate(Instant.now());
        }
        if (order.getOrderStatus() == null) {
            order.setOrderStatus(OrderStatus.PENDING.name());
        }
        return repo.save(order);
    }

    @Override
    public Orders update(Orders order) {
        if (!repo.existsById(order.getId())) {
            throw new RuntimeException("Không tồn tại đơn hàng để cập nhật");
        }
        return repo.save(order);
    }

    @Override
    public void delete(int id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Không tồn tại đơn hàng để xóa");
        }
        repo.deleteById(id);
    }

 

    @Override
    public List<Orders> findByUsername(String username) {
        return repo.findByAccount_Username(username);
    }

    @Override
    public List<Orders> findByDate(LocalDate date) {
        return repo.findByCreateDateRange(dayStart(date), dayEnd(date));
    }

    @Override
    public List<Orders> findByDateRange(LocalDate from, LocalDate to) {
        return repo.findByCreateDateRange(dayStart(from), dayEnd(to.plusDays(1)));
    }


    @Override
    public Orders updateStatus(int orderId, String status) {
        Orders order = findById(orderId);
        order.setOrderStatus(status);
        return repo.save(order);
    }

    @Override
    public long countTodayOrders() {
        LocalDate today = LocalDate.now();
        return repo.countTodayOrders(dayStart(today), dayEnd(today));
    }

    @Override
    public long countPendingOrders() {
        return repo.countByOrderStatus(OrderStatus.PENDING.name());
    }

    @Override
    public List<Orders> findByOrderStatus(String status) {
        return repo.findByOrderStatusOrderByCreateDateDesc(status);
    }

    private Instant dayStart(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private Instant dayEnd(LocalDate date) {
        return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
	
}
