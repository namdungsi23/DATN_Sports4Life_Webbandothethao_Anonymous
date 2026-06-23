package poly.edu.ASSM.Services.core;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.Orders;
import poly.edu.ASSM.Repository.OrdersRepository;

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
            order.setCreateDate(LocalDate.now());
        }
        if (order.getStatus() == null) {
            order.setStatus("NEW");
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
        return repo.findByUsername(username);
    }

    @Override
    public List<Orders> findByDate(LocalDate date) {
        return repo.findByCreateDate(date);
    }

    @Override
    public List<Orders> findByDateRange(LocalDate from, LocalDate to) {
        return repo.findByCreateDateBetween(from, to);
    }


    @Override
    public Orders updateStatus(int orderId, String status) {
        Orders order = findById(orderId);
        order.setStatus(status);
        return repo.save(order);
    }

    @Override
    public long countTodayOrders() {
        return repo.countTodayOrders(LocalDate.now());
    }
	
}
