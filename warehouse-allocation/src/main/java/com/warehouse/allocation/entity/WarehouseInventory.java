package com.warehouse.allocation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_inventory", indexes = {
    @Index(name = "idx_warehouse_id", columnList = "warehouse_id"),
    @Index(name = "idx_product_id", columnList = "product_id"),
    @Index(name = "idx_available_quantity", columnList = "available_quantity")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseInventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "available_quantity", nullable = false)
    private Long availableQuantity = 0L;
    
    @Version
    @Column(name = "version")
    private Long version = 0L;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
   
     //Allocate quantity from this inventory
     //Throws exception if not enough stock
    
    public void allocate(Long quantity) {
        if (this.availableQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Insufficient stock. Available: %d, Requested: %d", 
                    this.availableQuantity, quantity)
            );
        }
        this.availableQuantity -= quantity;
    }
    
    
    //Deallocate quantity back to inventory
    
    public void deallocate(Long quantity) {
        this.availableQuantity += quantity;
    }
    
    
     //Receive stock from transfer
     
    public void receiveStock(Long quantity) {
        this.availableQuantity += quantity;
    }
    
    
     // Transfer stock out
     
    public void transferStock(Long quantity) {
        if (this.availableQuantity < quantity) {
            throw new IllegalStateException(
                String.format("Insufficient stock for transfer. Available: %d, Requested: %d",
                    this.availableQuantity, quantity)
            );
        }
        this.availableQuantity -= quantity;
    }
}