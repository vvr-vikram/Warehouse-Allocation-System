package com.warehouse.allocation.repository;

import com.warehouse.allocation.entity.Allocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    
    Optional<Allocation> findByReferenceId(String referenceId);
    
    List<Allocation> findByProductIdOrderByAllocatedAtDesc(Long productId);
    
    List<Allocation> findByWarehouseIdOrderByAllocatedAtDesc(Long warehouseId);
    
    Page<Allocation> findByProductIdAndStatusOrderByAllocatedAtDesc(
        Long productId,
        Allocation.AllocationStatus status,
        Pageable pageable
    );
    
    Page<Allocation> findByWarehouseIdAndStatusOrderByAllocatedAtDesc(
        Long warehouseId,
        Allocation.AllocationStatus status,
        Pageable pageable
    );
    
    @Query("SELECT a FROM Allocation a WHERE " +
           "(:productId IS NULL OR a.product.id = :productId) AND " +
           "(:warehouseId IS NULL OR a.warehouse.id = :warehouseId) AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:startDate IS NULL OR a.allocatedAt >= :startDate) AND " +
           "(:endDate IS NULL OR a.allocatedAt <= :endDate) " +
           "ORDER BY a.allocatedAt DESC")
    Page<Allocation> searchAllocations(
        @Param("productId") Long productId,
        @Param("warehouseId") Long warehouseId,
        @Param("status") Allocation.AllocationStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(a) FROM Allocation a WHERE a.status = :status AND a.allocatedAt BETWEEN :startDate AND :endDate")
    Long countAllocationsInPeriod(
        @Param("status") Allocation.AllocationStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}