package com.warehouse.allocation.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.warehouse.allocation.dto.CreateProductRequest;
import com.warehouse.allocation.dto.ProductResponse;
import com.warehouse.allocation.entity.AuditLog;
import com.warehouse.allocation.entity.Product;
import com.warehouse.allocation.exception.AllocationException.ProductNotFoundException;
import com.warehouse.allocation.repository.ProductRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class ProductService {
    
    private final ProductRepository productRepository;
    private final AuditService auditService;
    
    public ProductService(ProductRepository productRepository, AuditService auditService) {
        this.productRepository = productRepository;
        this.auditService = auditService;
    }
    
    
     // Create a new product
     
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());
        
        // Check if product with same name or SKU already exists
        productRepository.findByName(request.getName())
            .ifPresent(p -> {
                throw new IllegalArgumentException("Product with name '" + request.getName() + "' already exists");
            });
        
        productRepository.findBySku(request.getSku())
            .ifPresent(p -> {
                throw new IllegalArgumentException("Product with SKU '" + request.getSku() + "' already exists");
            });
        
        Product product = Product.builder()
            .name(request.getName())
            .sku(request.getSku())
            .totalStock(request.getTotalStock())
            .build();
        
        Product saved = productRepository.save(product);
        log.info("Product created with ID: {}", saved.getId());
        
        auditService.logAction(
            "Product",
            saved.getId(),
            AuditLog.AuditAction.CREATE,
            null,
            saved,
            "Product created"
        );
        
        return mapToProductResponse(saved);
    }
    
    
     //Get product by ID
     
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        return mapToProductResponse(product);
    }
    
    
      //Get product by SKU
     
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with SKU: " + sku));
        return mapToProductResponse(product);
    }
    
    
     // Get all products
     
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
            .map(this::mapToProductResponse)
            .collect(Collectors.toList());
    }
    
    
     // Search products by name
     
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name).stream()
            .map(this::mapToProductResponse)
            .collect(Collectors.toList());
    }
    
    
     // Update product details
     
    public ProductResponse updateProduct(Long id, CreateProductRequest request) {
        log.info("Updating product with ID: {}", id);
        
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException("Product not found with ID: " + id));
        
        // Check if new name or SKU conflicts
        if (!product.getName().equals(request.getName())) {
            productRepository.findByName(request.getName())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Product with name '" + request.getName() + "' already exists");
                });
        }
        
        if (!product.getSku().equals(request.getSku())) {
            productRepository.findBySku(request.getSku())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Product with SKU '" + request.getSku() + "' already exists");
                });
        }
        
        Product originalCopy = Product.builder()
            .id(product.getId())
            .name(product.getName())
            .sku(product.getSku())
            .totalStock(product.getTotalStock())
            .build();
        
        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setTotalStock(request.getTotalStock());
        
        Product updated = productRepository.save(product);
        
        auditService.logAction(
            "Product",
            updated.getId(),
            AuditLog.AuditAction.UPDATE,
            originalCopy,
            updated,
            "Product updated"
        );
        
        return mapToProductResponse(updated);
    }
    
    
     // Map Product entity to DTO
     
    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .sku(product.getSku())
            .totalStock(product.getTotalStock())
            .createdAt(product.getCreatedAt())
            .updatedAt(product.getUpdatedAt())
            .build();
    }
}