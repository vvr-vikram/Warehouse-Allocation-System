package com.warehouse.allocation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transfer", indexes = {
    @Index(name = "idx_source_warehouse_id", columnList = "source_warehouse_id"),
    @Index(name = "idx_target_warehouse_id", columnList = "target_warehouse_id"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_transfer_date", columnList = "transfer_date"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id", nullable = false)
    private Warehouse sourceWarehouse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_warehouse_id", nullable = false)
    private Warehouse targetWarehouse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Long quantity;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransferStatus status = TransferStatus.PENDING;
    
    @Column(name = "transfer_date", nullable = false)
    private LocalDateTime transferDate;
    
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    
    @Column(name = "reference_id", unique = true)
    private String referenceId;
    
    @Column(name = "notes", length = 500)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum TransferStatus {
        PENDING,
        IN_TRANSIT,
        COMPLETED,
        CANCELLED
    }
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = TransferStatus.PENDING;
        }
        if (transferDate == null) {
            transferDate = LocalDateTime.now();
        }
    }
    
    public void validateTransfer() {
        if (sourceWarehouse.getId().equals(targetWarehouse.getId())) {
            throw new IllegalArgumentException("Source and target warehouses cannot be the same");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Transfer quantity must be positive");
        }
    }
}