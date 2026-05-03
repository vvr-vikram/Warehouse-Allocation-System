package com.warehouse.allocation.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_entity_type", columnList = "entity_type"),
    @Index(name = "idx_entity_id", columnList = "entity_id"),
    @Index(name = "idx_action", columnList = "action"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "old_values", columnDefinition = "JSON")
    private String oldValues;
    
    @Column(name = "new_values", columnDefinition = "JSON")
    private String newValues;
    
    @Column(name = "description", length = 2000)
    private String description;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        ALLOCATE,
        TRANSFER,
        CONFIRM,
        ROLLBACK
    }
}