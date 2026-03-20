# Banking System Architecture Documentation

## Overview
This banking system backend implements Domain-Driven Design (DDD) principles within a Hexagonal Architecture (also known as Ports and Adapters). The system provides account withdrawal functionality with a clean separation of concerns between business logic and external dependencies.

## Architectural Patterns

### Hexagonal Architecture (Ports and Adapters)

Hexagonal Architecture structures the application around a core domain, with external concerns adapted through ports and adapters. This creates a testable, maintainable system where business logic is isolated from infrastructure concerns.

#### Core Components:
- **Domain**: The central business logic (Account, WithdrawalService)
- **Ports**: Interfaces defining how the domain interacts with the outside world
- **Adapters**: Implementations of ports for specific technologies (Web controllers, Database repositories)

#### Benefits:
- **Testability**: Domain logic can be tested without external dependencies
- **Technology Independence**: Adapters can be swapped (e.g., REST API → GraphQL, JPA → MongoDB)
- **Maintainability**: Changes to external systems don't affect core business logic

### Domain-Driven Design (DDD)

DDD focuses on modeling the business domain accurately, with the domain model at the center of the application.

#### DDD Building Blocks Used:
- **Entities**: Objects with identity and lifecycle (Account)
- **Value Objects**: Immutable objects representing concepts (BigDecimal for amounts)
- **Domain Services**: Business logic that doesn't belong to a single entity (WithdrawalService)
- **Repositories**: Interfaces for accessing domain objects (AccountRepository)
- **Application Services**: Orchestrate domain objects (Controllers, Configuration)

## Project Structure

```
backend-spring/
├── src/main/java/com/banking/
│   ├── BankingApplication.java          # Spring Boot main class
│   ├── adapter/                         # Infrastructure layer (Adapters)
│   │   ├── in/                          # Input adapters (Driving adapters)
│   │   │   └── web/
│   │   │       └── WithdrawalController.java  # REST API adapter
│   │   └── out/                         # Output adapters (Driven adapters)
│   │       └── persistence/
│   │           ├── AccountEntity.java           # JPA entity (Database mapping)
│   │           ├── AccountPersistenceAdapter.java # Repository implementation
│   │           └── SpringDataAccountRepository.java # Spring Data interface
│   ├── config/                         # Application configuration
│   │   └── DomainConfig.java           # Dependency injection configuration
│   └── domain/                         # Domain layer (Core business logic)
│       ├── model/
│       │   └── Account.java            # Domain entity
│       ├── port/
│       │   └── AccountRepository.java  # Domain port (interface)
│       └── service/
│           └── WithdrawalService.java  # Domain service
└── src/test/java/com/banking/domain/model/
    └── AccountTest.java                # Domain tests
```

## Domain Layer Analysis

### Domain Entity: Account
```java
public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;

    public void withdraw(BigDecimal amount) {
        // Business rules: validation, insufficient funds check
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
    }
}
```

**DDD Concepts**:
- **Entity**: Has identity (id, accountNumber) and encapsulates business rules
- **Business Logic**: Withdrawal logic is encapsulated within the entity
- **Invariants**: Balance cannot go negative, amounts must be positive

### Domain Service: WithdrawalService
```java
public class WithdrawalService {
    private final AccountRepository accountRepository;

    public void withdrawMoney(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.withdraw(amount);  // Domain logic
        accountRepository.save(account);  // Persistence
    }
}
```

**DDD Concepts**:
- **Domain Service**: Orchestrates domain objects for complex operations
- **Repository Pattern**: Abstracts data access through interface
- **Transaction Script**: Coordinates entity behavior

### Domain Port: AccountRepository
```java
public interface AccountRepository {
    Optional<Account> findById(Long id);
    Account save(Account account);
}
```

**Hexagonal Architecture Concepts**:
- **Port**: Defines the contract for data access
- **Dependency Inversion**: Domain depends on abstraction, not implementation
- **Testability**: Can be mocked for domain testing

## Adapter Layer Analysis

### Input Adapter: WithdrawalController (Driving Adapter)
```java
@RestController
@RequestMapping("/api/accounts")
public class WithdrawalController {
    private final WithdrawalService withdrawalService;
    private final AccountRepository accountRepository;

    @PostMapping("/{accountId}/withdraw")
    public void withdraw(@PathVariable Long accountId, @RequestBody WithdrawalRequest request) {
        withdrawalService.withdrawMoney(accountId, request.getAmount());
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<Account> getAccount(@PathVariable Long accountId) {
        Optional<Account> account = accountRepository.findById(accountId);
        return account.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
```

**Hexagonal Architecture Concepts**:
- **Driving Adapter**: Translates external requests (HTTP) to domain calls
- **Thin Layer**: Minimal logic, delegates to domain
- **API Design**: RESTful endpoints for withdrawal and account retrieval

### Output Adapter: AccountPersistenceAdapter (Driven Adapter)
```java
@Component
public class AccountPersistenceAdapter implements AccountRepository {
    private final SpringDataAccountRepository springRepository;

    @Override
    public Optional<Account> findById(Long id) {
        Optional<AccountEntity> entity = springRepository.findById(id);
        if (entity.isPresent()) {
            AccountEntity e = entity.get();
            return Optional.of(new Account(e.getId(), e.getAccountNumber(), e.getBalance()));
        }
        return Optional.empty();
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = new AccountEntity(account.getId(), account.getAccountNumber(), account.getBalance());
        AccountEntity saved = springRepository.save(entity);
        return new Account(saved.getId(), saved.getAccountNumber(), saved.getBalance());
    }
}
```

**Hexagonal Architecture Concepts**:
- **Driven Adapter**: Implements domain ports for external systems
- **Anti-Corruption Layer**: Translates between domain objects and database entities
- **Dependency Injection**: Injected into domain services

### Infrastructure Components

#### JPA Entity: AccountEntity
```java
@Entity
@Table(name = "accounts")
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private BigDecimal balance;
}
```

**Purpose**: Database mapping layer, separate from domain model

#### Spring Data Repository: SpringDataAccountRepository
```java
@Repository
public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, Long> {
    // Spring provides all basic CRUD operations automatically
}
```

**Purpose**: Infrastructure-level data access using Spring Data JPA

## Dependency Flow

```
HTTP Request
    ↓
WithdrawalController (Input Adapter)
    ↓
WithdrawalService (Domain Service)
    ↓
AccountRepository (Domain Port)
    ↓
AccountPersistenceAdapter (Output Adapter)
    ↓
SpringDataAccountRepository (Infrastructure)
    ↓
Database (H2/MySQL)
```

## Configuration Layer

### DomainConfig.java
```java
@Configuration
public class DomainConfig {
    @Bean
    public WithdrawalService withdrawalService(AccountRepository accountRepository) {
        return new WithdrawalService(accountRepository);
    }
}
```

**Purpose**: Dependency injection configuration, wires domain services with their dependencies

## Testing Strategy

### Domain Testing
- **AccountTest.java**: Tests domain entity behavior in isolation
- **Focus**: Business rules, validation, state changes
- **Approach**: Unit tests without external dependencies

### Integration Testing
- **Spring Context Tests**: Test adapter wiring
- **API Tests**: Test controller endpoints
- **Repository Tests**: Test data persistence

## Key Design Principles Demonstrated

### 1. Dependency Inversion Principle
- Domain depends on abstractions (ports), not concretions
- High-level modules don't depend on low-level modules

### 2. Single Responsibility Principle
- Each class has one reason to change
- Controllers handle HTTP, Services handle business logic, Repositories handle data access

### 3. Separation of Concerns
- Domain logic isolated from infrastructure
- Business rules protected from external changes

### 4. Testability
- Domain can be tested without Spring context
- Adapters can be mocked for controller testing

## Benefits of This Architecture

### For Development:
- **Parallel Development**: Teams can work on different layers independently
- **Easier Testing**: Domain logic tested in isolation
- **Technology Flexibility**: Can change databases, frameworks, or APIs without affecting business logic

### For Maintenance:
- **Easier Refactoring**: Changes to one layer don't cascade to others
- **Clear Boundaries**: Responsibilities are well-defined
- **Evolutionary Architecture**: Can evolve different parts at different speeds

### For Business:
- **Domain Focus**: Code reflects business concepts
- **Reliability**: Business rules are protected and tested
- **Scalability**: Clean architecture supports growth

## Future Enhancements

### Additional Domain Features:
- Transfer between accounts
- Deposit functionality
- Account creation
- Transaction history

### Infrastructure Improvements:
- Database migration scripts
- Caching layer
- Event publishing
- API documentation

### Architecture Extensions:
- CQRS pattern for read/write separation
- Event sourcing for audit trails
- Microservices decomposition

This architecture provides a solid foundation for a scalable, maintainable banking system that can evolve with business needs while keeping the core domain logic clean and protected.</content>
<parameter name="filePath">C:\Users\qfenama\Documents\banking-system\ARCHITECTURE.md
