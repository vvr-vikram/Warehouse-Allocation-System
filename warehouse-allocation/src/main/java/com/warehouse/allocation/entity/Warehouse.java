package com.warehouse.allocation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse", indexes = {
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, unique = true)
    private String name;
    
    @Column(name = "location", nullable = false)
    private String location;
    
    @Column(name = "capacity", nullable = false)
    private Long capacity;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;
    
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum WarehouseStatus {
        ACTIVE,
        INACTIVE
    }
    
    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = WarehouseStatus.ACTIVE;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
    }
}