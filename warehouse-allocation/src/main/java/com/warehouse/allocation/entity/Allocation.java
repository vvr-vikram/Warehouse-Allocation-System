package com.warehouse.allocation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "allocation", indexes = {
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_allocated_at", columnList = "allocated_at"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_reference_id", columnList = "reference_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Allocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @Column(name = "quantity", nullable = false)
    private Long quantity;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AllocationStatus status = AllocationStatus.PENDING;
    
    @Column(name = "allocated_at", nullable = false)
    private LocalDateTime allocatedAt;
    
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
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
    
    public enum AllocationStatus {
        PENDING,
        CONFIRMED,
        COMPLETED,
        ROLLED_BACK
    }
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = AllocationStatus.PENDING;
        }
        if (allocatedAt == null) {
            allocatedAt = LocalDateTime.now();
        }
    }
}