package poly.edu.ASSM.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import poly.edu.ASSM.Entity.Inventory;
import poly.edu.ASSM.Entity.ProductVariants;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByVariant(ProductVariants variant);
}
