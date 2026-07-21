package poly.edu.ASSM.Services.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import poly.edu.ASSM.Entity.OrderDetails;
import poly.edu.ASSM.Repository.OrderDetailsRepository;

@Service
public class OrderDetailsServiceImpl implements OrderDetailsService {

    @Autowired
    OrderDetailsRepository repo;

    @Override
    public List<OrderDetails> findAll() {
        return repo.findAll();
    }

    @Override
    public OrderDetails findById(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đơn hàng ID = " + id));
    }

    @Override
    public OrderDetails create(OrderDetails detail) {
        if (detail.getQuantity() == null || detail.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng phải > 0");
        }
        if (detail.getPrice() == null || detail.getPrice() <= 0) {
            throw new RuntimeException("Giá phải > 0");
        }
        return repo.save(detail);
    }

    @Override
    public OrderDetails update(OrderDetails detail) {
        if (detail.getId() == null || !repo.existsById(detail.getId())) {
            throw new RuntimeException("Không tồn tại OrderDetail để cập nhật");
        }
        return repo.save(detail);
    }

    @Override
    public void delete(int id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Không tồn tại OrderDetail để xóa");
        }
        repo.deleteById(id);
    }

    @Override
    public List<OrderDetails> findByOrder(int orderId) {
        return repo.findByOrders_Id(orderId);
    }

    @Override
    public void deleteByOrder(int orderId) {
        repo.deleteByOrders_Id(orderId);
    }
}
