package com.warehouse.allocation.repository;

import com.warehouse.allocation.entity.StockTransfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    
    Optional<StockTransfer> findByReferenceId(String referenceId);
    
    List<StockTransfer> findBySourceWarehouseIdOrderByTransferDateDesc(Long sourceWarehouseId);
    
    List<StockTransfer> findByTargetWarehouseIdOrderByTransferDateDesc(Long targetWarehouseId);
    
    Page<StockTransfer> findBySourceWarehouseIdAndStatusOrderByTransferDateDesc(
        Long sourceWarehouseId,
        StockTransfer.TransferStatus status,
        Pageable pageable
    );
    
    Page<StockTransfer> findByTargetWarehouseIdAndStatusOrderByTransferDateDesc(
        Long targetWarehouseId,
        StockTransfer.TransferStatus status,
        Pageable pageable
    );
    
    @Query("SELECT st FROM StockTransfer st WHERE " +
           "(:sourceWarehouseId IS NULL OR st.sourceWarehouse.id = :sourceWarehouseId) AND " +
           "(:targetWarehouseId IS NULL OR st.targetWarehouse.id = :targetWarehouseId) AND " +
           "(:productId IS NULL OR st.product.id = :productId) AND " +
           "(:status IS NULL OR st.status = :status) " +
           "ORDER BY st.transferDate DESC")
    Page<StockTransfer> searchTransfers(
        @Param("sourceWarehouseId") Long sourceWarehouseId,
        @Param("targetWarehouseId") Long targetWarehouseId,
        @Param("productId") Long productId,
        @Param("status") StockTransfer.TransferStatus status,
        Pageable pageable
    );
}