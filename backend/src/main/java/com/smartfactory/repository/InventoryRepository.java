package com.smartfactory.repository;

import com.smartfactory.entity.Inventory;
import com.smartfactory.enums.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByMaterialId(Long materialId);

    List<Inventory> findByStatus(InventoryStatus status);

    @Query("SELECT i FROM Inventory i WHERE i.currentQuantity <= i.minimumThreshold")
    List<Inventory> findLowStockItems();

    @Query("SELECT i FROM Inventory i WHERE i.currentQuantity <= 0")
    List<Inventory> findOutOfStockItems();

    @Query("SELECT i FROM Inventory i JOIN FETCH i.material m WHERE m.materialCode = :code")
    Optional<Inventory> findByMaterialCode(@Param("code") String materialCode);

    @Query("SELECT i FROM Inventory i WHERE i.currentQuantity - i.reservedQuantity >= :quantity AND i.material.id = :materialId")
    Optional<Inventory> findAvailableStock(@Param("materialId") Long materialId, @Param("quantity") BigDecimal quantity);
}
