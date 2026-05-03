package com.warehouse.allocation.repository;

import com.warehouse.allocation.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByNameAndIsDeletedFalse(String name);
    List<Warehouse> findByStatusAndIsDeletedFalse(Warehouse.WarehouseStatus status);
    Page<Warehouse> findByIsDeletedFalse(Pageable pageable);
}