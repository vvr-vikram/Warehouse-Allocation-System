package com.warehouse.allocation.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.warehouse.allocation.dto.ResponseWrapper;
import com.warehouse.allocation.exception.AllocationException;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.error("Validation error occurred", ex);
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseWrapper.<Map<String, String>>builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .errorCode("VALIDATION_ERROR")
                .timestamp(LocalDateTime.now())
                .build());
    }
    
    @ExceptionHandler(AllocationException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleAllocationException(
            AllocationException ex,
            WebRequest request) {
        
        log.error("Allocation exception occurred: {}", ex.getMessage(), ex);
        
        HttpStatus status = mapExceptionToHttpStatus(ex.getErrorCode());
        
        return ResponseEntity.status(status)
            .body(ResponseWrapper.error(ex.getMessage(), ex.getErrorCode()));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.error("Illegal argument exception occurred: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResponseWrapper.error(ex.getMessage(), "INVALID_ARGUMENT"));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {
        
        log.error("Illegal state exception occurred: {}", ex.getMessage(), ex);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ResponseWrapper.error(ex.getMessage(), "INVALID_STATE"));
    }
    
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ResponseWrapper<Object>> handleOptimisticLockingException(
            org.springframework.orm.ObjectOptimisticLockingFailureException ex,
            WebRequest request) {
        
        log.error("Optimistic locking exception occurred", ex);
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ResponseWrapper.error(
                "Concurrent modification detected. Please retry the operation.",
                "CONCURRENT_MODIFICATION"
            ));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseWrapper<Object>> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected exception occurred", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ResponseWrapper.error(
                "An unexpected error occurred. Please contact support.",
                "INTERNAL_SERVER_ERROR"
            ));
    }
    
    private HttpStatus mapExceptionToHttpStatus(String errorCode) {
        if (errorCode == null) return HttpStatus.INTERNAL_SERVER_ERROR;
        
        switch (errorCode) {
            case "INSUFFICIENT_STOCK": return HttpStatus.BAD_REQUEST;
            case "WAREHOUSE_NOT_FOUND":
            case "PRODUCT_NOT_FOUND":
            case "INVENTORY_NOT_FOUND":
            case "ALLOCATION_NOT_FOUND":
            case "TRANSFER_NOT_FOUND":
                return HttpStatus.NOT_FOUND;
            case "CAPACITY_EXCEEDED": return HttpStatus.BAD_REQUEST;
            case "CONCURRENT_MODIFICATION": return HttpStatus.CONFLICT;
            case "INVALID_STATE": return HttpStatus.CONFLICT;
            default: return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}