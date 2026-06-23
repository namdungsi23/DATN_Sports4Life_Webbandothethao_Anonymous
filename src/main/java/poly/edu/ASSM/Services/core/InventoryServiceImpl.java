package poly.edu.ASSM.Services.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import poly.edu.ASSM.exception.OutOfStockException;

@Service
public class InventoryServiceImpl implements InventoryService{
	@Autowired
	JdbcTemplate jdbcTemplate;

	@Override
	public void checkInventory(int productId, int amount) {
		// TODO Auto-generated method stub
		try {
			jdbcTemplate.update("EXEC sp_CheckInventory @product_id=?, @amount=?", productId, amount);
		}catch(DataAccessException e) {
			/* if(e.getMessage().contains("hết hàng")) {
				throw new OutOfStockException("Hết hàng");
			}
			*/
			
			e.printStackTrace();
			
			Throwable root = e.getRootCause();
			
			if(root instanceof SQLServerException sqlEx) {
				if(sqlEx.getErrorCode() == 50003) {
					throw new OutOfStockException("Số lượng không hợp lệ!");
				}
			}
			
		}
	}

	@Override
	public void checkAndUpdateInventory(int productId, int amount) {
			jdbcTemplate.update("EXEC sp_CheckAndUpdateInventory @product_id=?, @amount=?", productId, amount);
	}
	
	

}
