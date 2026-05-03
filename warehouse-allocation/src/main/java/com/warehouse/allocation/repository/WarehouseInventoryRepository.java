package com.warehouse.allocation.repository;

import com.warehouse.allocation.entity.WarehouseInventory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {
    Optional<WarehouseInventory> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
    
    List<WarehouseInventory> findByProductId(Long productId);
    
    List<WarehouseInventory> findByWarehouseId(Long warehouseId);
    
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.product.id = :productId AND wi.availableQuantity >= :quantity AND wi.warehouse.status = 'ACTIVE' ORDER BY wi.availableQuantity DESC")
    List<WarehouseInventory> findWarehousesWithSufficientStock(
        @Param("productId") Long productId,
        @Param("quantity") Long quantity,
        Pageable pageable
    );
    
    @Query("SELECT SUM(wi.availableQuantity) FROM WarehouseInventory wi WHERE wi.product.id = :productId")
    Long getTotalAvailableStock(@Param("productId") Long productId);
}