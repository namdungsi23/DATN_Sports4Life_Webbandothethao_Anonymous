package poly.edu.ASSM.Services.core;

import org.springframework.stereotype.Service;

public interface InventoryService {
	void checkInventory(int productId, int amount);
	
	void checkAndUpdateInventory(int productId, int amount);
}
