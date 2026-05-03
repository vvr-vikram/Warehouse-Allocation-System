package com.warehouse.allocation.service;

import com.warehouse.allocation.entity.AuditLog;
import com.warehouse.allocation.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        // Configure ObjectMapper for Java 8 dates
        this.objectMapper = objectMapper.copy();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
   
     //Log an action
   
    public void logAction(String entityType, Long entityId, AuditLog.AuditAction action,
                         Object oldValues, Object newValues, String description) {
        try {
            String oldValuesJson = null;
            String newValuesJson = null;
            
            if (oldValues != null) {
                try {
                    oldValuesJson = objectMapper.writeValueAsString(oldValues);
                } catch (Exception e) {
                    log.warn("Could not serialize oldValues: {}", e.getMessage());
                    oldValuesJson = "{\"error\":\"Serialization failed\"}";
                }
            }
            
            if (newValues != null) {
                try {
                    newValuesJson = objectMapper.writeValueAsString(newValues);
                } catch (Exception e) {
                    log.warn("Could not serialize newValues: {}", e.getMessage());
                    newValuesJson = "{\"error\":\"Serialization failed\"}";
                }
            }
            
            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValues(oldValuesJson)
                .newValues(newValuesJson)
                .description(description)
                .build();
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for entity {} with action {}", entityType, action);
        } catch (Exception ex) {
            log.error("Failed to create audit log", ex);
        }
    }
    
  

    
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogs(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId);
    }
    
   
     // Get audit logs by action
 
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByAction(AuditLog.AuditAction action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action);
    }
    
    
     //Get audit logs by entity type with pagination
     
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogRepository.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
    }
    
   
     // Get audit logs for a date range
    
    @Transactional(readOnly = true)
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findAuditLogsByDateRange(startDate, endDate);
    }
}