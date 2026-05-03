package com.warehouse.allocation.controller;

import com.warehouse.allocation.dto.*;
import com.warehouse.allocation.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;

@RestController
@RequestMapping("/products")
@Validated
@Tag(name = "Product Management", description = "APIs for managing products")
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    
    public ProductController(ProductService productService) {
        this.productService = productService;
    }
    
    @PostMapping
    @Operation(summary = "Create a new product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "409", description = "Product already exists")
    })
    public ResponseEntity<ResponseWrapper<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request) {
        log.info("Creating product: {}", request.getName());
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ResponseWrapper.success(response, "Product created successfully"));
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ResponseWrapper<ProductResponse>> getProduct(
            @PathVariable @Positive Long id) {
        log.info("Fetching product with ID: {}", id);
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Product retrieved successfully"));
    }
    
    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ResponseWrapper<ProductResponse>> getProductBySku(
            @PathVariable String sku) {
        log.info("Fetching product with SKU: {}", sku);
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Product retrieved successfully"));
    }
    
    @GetMapping
    @Operation(summary = "Get all products")
    @ApiResponse(responseCode = "200", description = "List of products")
    public ResponseEntity<ResponseWrapper<List<ProductResponse>>> getAllProducts() {
        log.info("Fetching all products");
        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(ResponseWrapper.success(products, "Products retrieved successfully"));
    }
    
    @GetMapping("/search/{name}")
    @Operation(summary = "Search products by name")
    @ApiResponse(responseCode = "200", description = "Search results")
    public ResponseEntity<ResponseWrapper<List<ProductResponse>>> searchProducts(
            @PathVariable String name) {
        log.info("Searching products with name: {}", name);
        List<ProductResponse> products = productService.searchProducts(name);
        return ResponseEntity.ok(ResponseWrapper.success(products, "Products found"));
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ResponseWrapper<ProductResponse>> updateProduct(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CreateProductRequest request) {
        log.info("Updating product with ID: {}", id);
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ResponseWrapper.success(response, "Product updated successfully"));
    }
}