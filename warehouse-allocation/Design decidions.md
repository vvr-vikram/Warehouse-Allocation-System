# Warehouse Allocation System - Design Decisions & Assumptions

## Document Overview

This document outlines all architectural decisions, assumptions, and trade-offs made during the design and implementation of the Warehouse Allocation System.

---

## 1. Architecture & Design Patterns

### 1.1 Layered Architecture

**Decision**: Implement a **4-layer architecture**
- Controller (REST API)
- Service (Business Logic)
- Repository (Data Access)
- Database (Persistence)

**Rationale**:
- Separation of concerns for maintainability
- Easy testing (can mock repository layer)
- Follows Spring Boot best practices
- Enables scaling individual layers

**Trade-offs**:
- More classes and files to manage
- Additional boilerplate code
- Slight performance overhead from layering

---

### 1.2 Concurrency Control Strategy

**Decision**: **Optimistic Locking** using JPA `@Version` annotation

**Implementation**:
```java
@Version
@Column(name = "version")
private Long version = 0L;
```

**Rationale**:
- Better performance for low-contention scenarios
- Avoids database-level locks that block other operations
- Suitable for REST API with stateless requests
- Easy to implement in Spring Data JPA

**Alternatives Considered**:
- Pessimistic Locking: Would block entire inventory row during allocation
- Database-level Mutex: Complex to implement across multiple services
- No Locking: Data corruption risk in concurrent scenarios

**Trade-off**: 
- Client must retry on `ObjectOptimisticLockingFailureException`
- Not suitable for very high-contention scenarios (>1000 concurrent requests on same inventory)

---

## 2. Database Design Decisions

### 2.1 Soft Delete for Warehouses

**Decision**: Use `isDeleted` boolean flag instead of physical deletion

**Implementation**:
```sql
ALTER TABLE warehouse ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE;
```

**Rationale**:
- Preserves historical data for audit purposes
- Allows recovery if deletion was accidental
- Maintains referential integrity with foreign keys
- Complies with data retention policies

**Queries Updated**:
```java
findByStatusAndIsDeletedFalse(status);
findByNameAndIsDeletedFalse(name);
```

**Trade-off**: Database storage increases slightly, but gain immense value in auditability

---

### 2.2 Inventory Tracking Model

**Decision**: Separate `warehouse_inventory` table instead of denormalizing stock in product table

**Structure**:
```
Product (1) ──── (N) WarehouseInventory (N) ──── (1) Warehouse
```

**Rationale**:
- Supports multiple warehouses per product naturally
- Enables per-warehouse stock tracking with version control
- Efficient queries for warehouse-specific operations
- Simplifies allocation logic

**Alternative Considered**: Single `total_stock` in Product table
- Would require separate transfer/adjustment table
- More complex queries for warehouse-specific operations
- Cannot track per-warehouse stock levels

---

### 2.3 Allocation Status State Machine

**Decision**: 4-state model: `PENDING → CONFIRMED → COMPLETED → (or ROLLED_BACK)`

**States**:
| State | Meaning | Can Transition To |
|-------|---------|------------------|
| PENDING | Created but not confirmed | CONFIRMED, ROLLED_BACK |
| CONFIRMED | Approved and reserved | COMPLETED |
| COMPLETED | Fulfilled and delivered | (Terminal) |
| ROLLED_BACK | Cancelled, stock returned | (Terminal) |

**Rationale**:
- Provides clear workflow for allocation lifecycle
- Enables status-based queries and filtering
- Supports rollback for error scenarios
- Prevents invalid state transitions

**Implementation**:
```java
@Enumerated(EnumType.STRING)
private AllocationStatus status;
```

---

### 2.4 Indexing Strategy

**Applied Indexes**:

| Table | Column | Reason |
|-------|--------|--------|
| warehouse | name, status, created_at | Fast lookups and filtering |
| warehouse_inventory | warehouse_id, product_id | Join optimization |
| allocation | product_id, warehouse_id, status, allocated_at | Complex queries |
| stock_transfer | source/target warehouse_id, product_id | Transfer lookups |
| audit_log | entity_type, entity_id, action, created_at | Audit trail queries |

**Trade-off**: Faster reads, slower writes (INSERT/UPDATE cost increases)

---

## 3. API Design Decisions

### 3.1 RESTful Resource Naming

**Decision**: Use plural nouns with hierarchical resources

**Examples**:
- `POST /allocations` - Create allocation
- `GET /allocations/1` - Get specific allocation
- `GET /allocations/search` - Search with filters
- `PATCH /allocations/1/confirm` - Action-based endpoint

**Rationale**:
- Standard REST conventions
- Intuitively understandable by API consumers
- Follows RFC 3986 URI guidelines

---

### 3.2 Pagination & Sorting

**Decision**: Implement limit-offset pagination with custom sorting

**Query Parameters**:
```
?page=0&size=20&sortBy=allocatedAt&direction=DESC
```

**Rationale**:
- Standard for REST APIs
- Allows large datasets without loading everything
- Prevents memory exhaustion
- Familiar to API consumers

**Alternative Considered**: Cursor-based pagination
- Better for very large datasets
- More efficient with constantly changing data
- Not implemented due to increased complexity

---

### 3.3 Error Response Format

**Decision**: Consistent `ApiResponse<T>` wrapper with standardized error codes

**Example Error Response**:
```json
{
  "success": false,
  "message": "Insufficient stock available",
  "errorCode": "INSUFFICIENT_STOCK",
  "timestamp": "2026-04-30T10:30:00"
}
```

**Error Code Mapping**:
| Error Code | HTTP Status | Meaning |
|-----------|-------------|---------|
| INSUFFICIENT_STOCK | 400 | Not enough inventory |
| WAREHOUSE_NOT_FOUND | 404 | Warehouse ID invalid |
| CONCURRENT_MODIFICATION | 409 | Optimistic lock failure |
| INVALID_STATE | 409 | Operation not allowed in current state |
| VALIDATION_ERROR | 400 | Input validation failed |

**Rationale**:
- Standardized responses make client integration easier
- Error codes enable programmatic handling
- Clear distinction between client and server errors

---

## 4. Service Layer Decisions

### 4.1 Auto-Allocation Algorithm

**Decision**: Select warehouse with **smallest sufficient stock** (ascending order)

**Algorithm**:
```
1. Find warehouses with sufficient stock
2. Sort by available_quantity ASC
3. Select first warehouse
4. Return allocation from that warehouse
```

**Rationale**:
- Prevents inventory imbalance
- Distributes stock consumption evenly
- Preserves larger stock buffers for future demands
- Reduces risk of stockouts in smaller warehouses

**Alternative Strategies**:
- **Closest Warehouse**: Not implemented (no distance data available)
- **Largest Stock First**: Would deplete one warehouse quickly
- **Random**: No optimization benefit

---

### 4.2 Audit Logging Strategy

**Decision**: Log all CREATE, UPDATE, DELETE, ALLOCATE, TRANSFER operations

**Captured Data**:
```java
AuditLog {
  entityType: "Allocation",
  entityId: 1L,
  action: AuditAction.ALLOCATE,
  oldValues: {...},     // Previous state
  newValues: {...},     // Current state
  description: "...",   // Human-readable message
  createdAt: timestamp
}
```

**Rationale**:
- Complete audit trail for compliance
- Enables root cause analysis
- Supports data recovery scenarios
- Non-intrusive (separate table)

**Trade-off**: Additional storage and slight write performance impact

---

## 5. Testing Strategy

### 5.1 Unit Test Approach

**Decision**: Test service layer extensively using Mockito

**Coverage Target**: 70%+

**Tests Included**:
- Happy path scenarios
- Exception cases
- State transitions
- Concurrent modifications
- Boundary conditions

**Example Test**:
```java
@Test
void testAllocateFromWarehouse_InsufficientStock() {
    // Arrange: Set up mocks with insufficient stock
    // Act: Call allocation service
    // Assert: Verify exception thrown
}
```

**Not Tested** (by design):
- Spring Data JPA repository queries (tested by framework)
- Database constraints (DBA responsibility)
- Full integration tests (can be added separately)

---

### 5.2 Test Data Management

**Decision**: Use fresh mocks in `@BeforeEach` for isolation

**Rationale**:
- Each test is independent
- No test state contamination
- Faster test execution
- Easier to debug failures

**Alternative**: Test database fixtures
- Would require database setup
- Slower test execution
- More complex cleanup

---

## 6. Performance & Scalability

### 6.1 Response Time Target

**Requirement**: < 500ms for allocation API

**Optimization Strategies**:
1. **Database Indexes**: On frequently queried columns
2. **Lazy Loading**: For entity relationships
3. **Query Optimization**: Use projections where possible
4. **Connection Pooling**: HikariCP with sensible defaults
5. **Caching**: Not implemented (can add Redis if needed)

**Expected Performance**:
- Simple queries: 10-50ms
- Allocations with validation: 50-200ms
- Complex searches: 100-300ms

---

### 6.2 Scalability Considerations

**Horizontal Scaling**:
- Stateless service layer (can run multiple instances)
- No session affinity required
- Share same MySQL database

**Vertical Scaling**:
- Increase server resources
- Tune Hibernate/JPA settings
- Optimize database indexes

**Future Enhancements**:
- Add caching layer (Redis)
- Database read replicas
- Message queue for async operations

---

## 7. Security Considerations

### 7.1 Input Validation

**Decision**: Validate at controller level using Jakarta Validation annotations

**Examples**:
```java
@NotNull @Positive private Long productId;
@Size(min = 2, max = 255) private String name;
```

**Rationale**:
- Early rejection of invalid data
- Clear error messages to clients
- Standard Spring approach

---

### 7.2 Not Implemented (Out of Scope)

- **Authentication/Authorization**: No user/role system
- **Encryption**: Assume HTTPS at infrastructure level
- **Rate Limiting**: Can add using Spring Cloud
- **SQL Injection Protection**: Handled by JPA/Prepared Statements

---

## 8. Assumptions

### 8.1 Data Assumptions

1. **Product SKUs are unique** across the system
2. **Warehouse names are unique**
3. **Stock quantities are always non-negative**
4. **Capacity values are positive**
5. **Dates are in UTC**

### 8.2 Operational Assumptions

1. **Single data center** (no distributed database considerations)
2. **MySQL is the system of record**
3. **Batch uploads not required** (individual API calls sufficient)
4. **No financial implications** of allocation errors (no transaction rollback needed)

### 8.3 Environmental Assumptions

1. **Java 17+ available** for runtime
2. **MySQL 8.0+ for database**
3. **Maven 3.8+ for build**
4. **Minimum 512MB heap for application**

---

## 9. Known Limitations

### 9.1 Current Limitations

1. **No distributed transactions** - Single database only
2. **No real-time notifications** - Clients must poll
3. **No user authentication** - All API calls accepted
4. **Limited audit queries** - Basic filtering only
5. **No soft inventory** - Allocations are immediate

### 9.2 Future Enhancements

1. **WebSocket support** for real-time updates
2. **Async allocation** using message queues
3. **Role-based access control**
4. **Advanced reporting** with analytics
5. **Predictive stock management** using ML

---

## 10. Dependencies & Versions

### 10.1 Core Dependencies

| Dependency | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.2.0 | Framework |
| JPA Hibernate | (included) | ORM |
| MySQL Connector | 8.0.33 | Database driver |
| Springdoc OpenAPI | 2.1.0 | Swagger/OpenAPI |
| JUnit 5 | (included) | Testing |
| Mockito | (included) | Mocking |

### 10.2 Compatibility Notes

- Requires **Java 17+** (uses record-like features)
- MySQL **8.0+** required for JSON columns
- Spring Boot 3.2 uses Jakarta EE (not javax)

---

## 11. Configuration Management

### 11.1 Environment Configurations

**Development** (`application.yml`):
- Hibernate ddl-auto: validate
- Show SQL: false
- Log level: DEBUG

**Production** (override with -D flags):
- Use encrypted credentials
- Connection pool: 10-20 threads
- Log level: WARN

---

## 12. Change Log

### Version 1.0.0 (Current)
- Initial release with core functionality
- Optimistic locking for concurrency
- Soft delete for warehouses
- Comprehensive audit logging
- 70%+ test coverage

---

## 13. Approval & Sign-off

| Role | Name | Date |
|------|------|------|
| Architect | - | 2026-04-30 |
| Tech Lead | - | 2026-04-30 |
| QA Lead | - | 2026-04-30 |

---

## 14. Related Documentation

- API Documentation: Swagger UI at `/swagger-ui.html`
- Test Coverage Report: `target/site/jacoco/index.html`
- Database Schema: `database-schema.sql`

---

**Last Updated**: April 30, 2026  
**Document Version**: 1.0  
**Status**: Approved