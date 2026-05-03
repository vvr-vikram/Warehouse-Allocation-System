package com.warehouse.allocation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "Warehouse Allocation System API",
        version = "1.0.0",
        description = "Enterprise Level Warehouse Allocation System - Manages product distribution across multiple warehouses",
        contact = @Contact(
            name = "Warehouse Team",
            email = "support@warehouse.com"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0.html"
        )
    )
)
public class WarehouseAllocationApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(WarehouseAllocationApplication.class, args);
    }
}