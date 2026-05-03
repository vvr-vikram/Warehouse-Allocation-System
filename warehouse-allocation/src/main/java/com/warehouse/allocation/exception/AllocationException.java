package com.warehouse.allocation.exception;

public class AllocationException extends RuntimeException {
    
    private final String errorCode;
    
    public AllocationException(String message) {
        super(message);
        this.errorCode = "ALLOCATION_ERROR";
    }
    
    public AllocationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AllocationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "ALLOCATION_ERROR";
    }
    
    public AllocationException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    // Nested exception classes - these are public and accessible
    public static class InsufficientStockException extends AllocationException {
        public InsufficientStockException(String message) {
            super(message, "INSUFFICIENT_STOCK");
        }
    }
    
    public static class WarehouseNotFoundException extends AllocationException {
        public WarehouseNotFoundException(String message) {
            super(message, "WAREHOUSE_NOT_FOUND");
        }
    }
    
    public static class ProductNotFoundException extends AllocationException {
        public ProductNotFoundException(String message) {
            super(message, "PRODUCT_NOT_FOUND");
        }
    }
    
    public static class InventoryNotFoundException extends AllocationException {
        public InventoryNotFoundException(String message) {
            super(message, "INVENTORY_NOT_FOUND");
        }
    }
    
    public static class AllocationNotFoundException extends AllocationException {
        public AllocationNotFoundException(String message) {
            super(message, "ALLOCATION_NOT_FOUND");
        }
    }
    
    public static class CapacityExceededException extends AllocationException {
        public CapacityExceededException(String message) {
            super(message, "CAPACITY_EXCEEDED");
        }
    }
    
    public static class ConcurrentModificationException extends AllocationException {
        public ConcurrentModificationException(String message) {
            super(message, "CONCURRENT_MODIFICATION");
        }
    }
    
    public static class InvalidStateException extends AllocationException {
        public InvalidStateException(String message) {
            super(message, "INVALID_STATE");
        }
    }
}