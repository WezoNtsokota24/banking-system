# Domain-Driven Design (DDD) & Hexagonal Architecture Guide

## Table of Contents
1. [Core Concepts](#core-concepts)
2. [DDD Building Blocks](#ddd-building-blocks)
3. [Hexagonal Architecture](#hexagonal-architecture)
4. [Project Implementation](#project-implementation)
5. [Best Practices](#best-practices)

---

## Core Concepts

### What is Domain-Driven Design (DDD)?

Domain-Driven Design is an approach to software development that focuses on understanding and modeling the business domain. Instead of thinking about databases, frameworks, or APIs first, DDD starts with **what the business actually does**.

**Key Principle**: The domain model should be **testable without external dependencies**.

### What is Hexagonal Architecture?

Also known as "Ports and Adapters", Hexagonal Architecture structures applications with a clear boundary between the **core business logic** and **external concerns** (databases, web frameworks, external APIs).

**Key Benefit**: External technologies can be swapped without affecting the core domain logic.

---

## DDD Building Blocks

### 1. **Entity**
An object with a unique identity that persists over time.

**Characteristics**:
- Has an identity (ID)
- Lifecycle and state changes
- Contains business logic that protects invariants
- Mutable

**Example in Our Project**:
```java
public class Account {
    private Long id;                    // Identity
    private String accountNumber;
    private BigDecimal balance;

    // Business Logic: Withdrawal validation
    public void withdraw(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        this.balance = this.balance.subtract(amount);
    }
}
```

### 2. **Value Object**
An immutable object that represents a concept in the domain, identified by its attributes (not by an ID).

**Characteristics**:
- No identity
- Immutable
- Equality based on attribute values
- Used to make code more expressive

**Example**:
```java
// Instead of passing BigDecimal everywhere:
withdrawalService.withdrawMoney(accountId, new BigDecimal("50.00"));

// Better: Create a Money value object
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money(BigDecimal amount, Currency currency) {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Money cannot be negative");
        }
        this.amount = amount;
        this.currency = currency;
    }
}
```

### 3. **Aggregate**
A cluster of domain objects (entities and value objects) treated as a single unit for data changes. The aggregate has:
- **Aggregate Root**: The main entity through which external objects access the aggregate
- **Boundary**: Defines what's inside and outside the aggregate

**Example in Our Project**:
```
Account (Aggregate Root)
├── accountNumber (Value Object: String)
├── balance (Value Object: BigDecimal)
└── Business Rules
    └── Insufficient funds check
```

### 4. **Domain Service**
Business logic that doesn't belong to a single entity. It orchestrates entities and value objects to perform business operations.

**When to Use**: When logic involves multiple entities or when it's not a natural responsibility of any entity.

**Example in Our Project**:
```java
@Service
public class WithdrawalService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public void withdrawMoney(Long accountId, BigDecimal amount) {
        // Step 1: Retrieve the account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Step 2: Execute withdrawal (business logic in the entity)
        account.withdraw(amount);

        // Step 3: Persist the account
        accountRepository.save(account);

        // Step 4: Record the transaction
        Transaction transaction = new Transaction(
            null, accountId, amount, 
            TransactionType.WITHDRAWAL, 
            TransactionStatus.PENDING
        );
        transactionRepository.save(transaction);
    }
}
```

### 5. **Repository**
An interface that abstracts data access. It makes the domain believe it's working with in-memory collections, not databases.

**Example in Our Project**:
```java
public interface AccountRepository {
    Optional<Account> findById(Long id);
    Account save(Account account);
}
```

**Benefits**:
- Domain code doesn't know about SQL, JPA, or databases
- Easy to mock for testing
- Can swap implementations (JPA → MongoDB → REST API)

### 6. **Factory**
A design pattern for creating complex objects. Encapsulates object creation logic.

**Example** (Could be used in your Transaction creation):
```java
public class TransactionFactory {
    public static Transaction createWithdrawal(Long accountId, BigDecimal amount) {
        return new Transaction(
            null,
            accountId,
            amount,
            TransactionType.WITHDRAWAL,
            TransactionStatus.PENDING
        );
    }
}
```

### 7. **Event**
Something that happened in the domain that other parts of the system might care about.

**Example**:
```java
public class WithdrawalCompletedEvent {
    public final Long accountId;
    public final BigDecimal amount;
    public final LocalDateTime occurredAt;

    public WithdrawalCompletedEvent(Long accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
        this.occurredAt = LocalDateTime.now();
    }
}
```

---

## Hexagonal Architecture

### Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    EXTERNAL WORLD                           │
│  (HTTP Clients, Databases, Message Queues, External APIs)   │
└─────────────────────────────────────────────────────────────┘
              ↑ ↓ Adapters connect here
┌─────────────────────────────────────────────────────────────┐
│                     PORTS & ADAPTERS                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Input        │  │ Output       │  │ Input        │      │
│  │ Adapter 1    │  │ Adapter 1    │  │ Adapter 2    │      │
│  │ (REST)       │  │ (Database)   │  │ (Schedule)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
└─────────────────────────────────────────────────────────────┘
              ↑ ↓ Domain Interfaces (Ports)
┌─────────────────────────────────────────────────────────────┐
│                   DOMAIN LAYER (Core)                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Domain Models & Services (Pure Java)                │  │
│  │ - Account (Entity)                                  │  │
│  │ - Transaction (Entity)                              │  │
│  │ - WithdrawalService (Domain Service)                │  │
│  │ - TransactionService (Domain Service)               │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Domain Ports (Interfaces)                           │  │
│  │ - AccountRepository                                 │  │
│  │ - TransactionRepository                             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Types of Adapters

#### **Input Adapters (Driving Adapters)**
Accept requests from external actors and translate them into domain calls.

**Examples**:
- REST Controllers (HTTP requests)
- Scheduled Jobs (time-based triggers)
- Message Queue Listeners (async events)

**In Our Project**:
```java
@RestController
@RequestMapping("/api/accounts")
public class WithdrawalController {
    // Translates HTTP POST → Domain Service call
    @PostMapping("/{accountId}/withdraw")
    public void withdraw(@PathVariable Long accountId, @RequestBody WithdrawalRequest request) {
        withdrawalService.withdrawMoney(accountId, request.getAmount());
    }
}

@Component
public class NightlyTransactionBatch {
    // Translates scheduled time → Domain Service call
    @Scheduled(cron = "0 59 23 * * ?")
    public void runNightlyBatch() {
        transactionService.processNightlyBatch();
    }
}
```

#### **Output Adapters (Driven Adapters)**
Implement domain ports to interact with external systems.

**Examples**:
- JPA Repository implementations (database)
- HTTP clients (external APIs)
- Message queue producers (async)

**In Our Project**:
```java
@Component
public class AccountPersistenceAdapter implements AccountRepository {
    private final SpringDataAccountRepository springRepository;

    // Domain calls this method, thinks it's abstract
    // Adapter handles the actual database persistence
    @Override
    public Account save(Account account) {
        AccountEntity entity = new AccountEntity(
            account.getId(), 
            account.getAccountNumber(), 
            account.getBalance()
        );
        AccountEntity saved = springRepository.save(entity);
        
        // Translate back to domain object
        return new Account(saved.getId(), saved.getAccountNumber(), saved.getBalance());
    }
}
```

### The Dependency Flow

```
External Layer (Spring, JPA, HTTP)
    ↓ (depends on)
Adapter Layer (Spring Boot components)
    ↓ (depends on)
Port Layer (Interfaces)
    ↓ (realized by)
Domain Layer (Pure Java)

KEY: Domain depends on Ports, NOT on Adapters
```

This is called **Dependency Inversion**: High-level modules (domain) don't depend on low-level modules (adapters); both depend on abstractions (ports).

---

## Project Implementation

### Project Structure

```
banking-system/
├── backend-spring/
│   ├── src/main/java/com/banking/
│   │   ├── BankingApplication.java
│   │   ├── adapter/
│   │   │   ├── in/
│   │   │   │   ├── web/
│   │   │   │   │   └── WithdrawalController.java ......... INPUT ADAPTER
│   │   │   │   └── schedule/
│   │   │   │       └── NightlyTransactionBatch.java ..... INPUT ADAPTER
│   │   │   └── out/
│   │   │       └── persistence/
│   │   │           ├── AccountEntity.java ............. JPA ENTITY
│   │   │           ├── TransactionEntity.java ......... JPA ENTITY
│   │   │           ├── SpringDataAccountRepository.java  SPRING DATA
│   │   │           ├── SpringDataTransactionRepository.java SPRING DATA
│   │   │           ├── AccountPersistenceAdapter.java ... OUTPUT ADAPTER
│   │   │           └── TransactionPersistenceAdapter.java OUTPUT ADAPTER
│   │   ├── config/
│   │   │   └── DomainConfig.java .................. DEPENDENCY INJECTION
│   │   └── domain/
│   │       ├── model/
│   │       │   ├── Account.java ................. ENTITY
│   │       │   ├── Transaction.java ............ ENTITY
│   │       │   ├── TransactionType.java ........ ENUM
│   │       │   └── TransactionStatus.java ...... ENUM
│   │       ├── port/
│   │       │   ├── AccountRepository.java ....... PORT (Interface)
│   │       │   └── TransactionRepository.java ... PORT (Interface)
│   │       └── service/
│   │           ├── WithdrawalService.java ........ DOMAIN SERVICE
│   │           └── TransactionService.java ....... DOMAIN SERVICE
│   └── pom.xml
│
├── gateway-fastapi/
│   ├── main.py ........................... FastAPI APPLICATION
│   ├── config.py ......................... CONFIGURATION
│   ├── requirements.txt .................. PYTHON DEPENDENCIES
│   └── Dockerfile
│
└── docker-compose.yml
```

### Key DDD Concepts in This Project

#### 1. **Domain Models Are Pure Java**
```java
// Account.java - NO Spring annotations, NO @Entity decorator here
public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;

    // Business logic lives inside entities
    public void withdraw(BigDecimal amount) {
        // Validation rules specific to the domain
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("....");
        }
        if (this.balance.compareTo(amount) < 0) {
            throw new IllegalStateException("....");
        }
        this.balance = this.balance.subtract(amount);
    }
}
```

#### 2. **Repositories Abstract Data Access**
```java
// Port: The domain calls this
public interface AccountRepository {
    Optional<Account> findById(Long id);
    Account save(Account account);
}

// Adapter: The implementation details
@Component
public class AccountPersistenceAdapter implements AccountRepository {
    private final SpringDataAccountRepository springRepository;

    @Override
    public Account save(Account account) {
        // Translate domain → database entity
        // Execute persistence
        // Translate database entity → domain
    }
}
```

#### 3. **Domain Services Orchestrate**
```java
@Service
public class WithdrawalService {
    // Domain services depend on ports, not Spring components
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public void withdrawMoney(Long accountId, BigDecimal amount) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(...);
        
        account.withdraw(amount);  // Business logic in entity
        
        accountRepository.save(account);
        
        Transaction transaction = new Transaction(
            null, accountId, amount,
            TransactionType.WITHDRAWAL,
            TransactionStatus.PENDING
        );
        transactionRepository.save(transaction);
    }
}
```

#### 4. **Controllers Are Thin Input Adapters**
```java
@RestController
@RequestMapping("/api/accounts")
public class WithdrawalController {
    private final WithdrawalService withdrawalService;

    // Translate HTTP → Domain
    @PostMapping("/{accountId}/withdraw")
    public void withdraw(@PathVariable Long accountId, @RequestBody WithdrawalRequest request) {
        // Minimal logic - just delegate to domain
        withdrawalService.withdrawMoney(accountId, request.getAmount());
    }
}
```

#### 5. **Scheduled Tasks Are Input Adapters**
```java
@Component
public class NightlyTransactionBatch {
    private final TransactionService transactionService;

    // Translate scheduled time → Domain
    @Scheduled(cron = "0 59 23 * * ?")  // 23:59 every day
    public void runNightlyBatch() {
        transactionService.processNightlyBatch();
    }
}
```

---

## Best Practices

### ✅ DO

1. **Keep domain models free of framework annotations**
   ```java
   // GOOD
   public class Account { ... }
   
   // BAD
   @Entity
   @Table(name = "accounts")
   public class Account { ... }
   ```

2. **Use repositories to abstract data access**
   ```java
   // GOOD
   Account account = accountRepository.findById(id);
   
   // BAD
   Account account = accountManager.getFromDatabase(id);
   ```

3. **Put business logic in entities and services**
   ```java
   // GOOD - Logic in domain
   account.withdraw(amount);
   
   // BAD - Logic in controller
   if (balance >= amount) {
       balance -= amount;
   }
   ```

4. **Use enums for fixed sets of values**
   ```java
   // GOOD
   private TransactionType type;  // DEPOSIT, WITHDRAWAL
   
   // BAD
   private String type;  // "deposit", "Deposit", "DEPOSIT"?
   ```

5. **Translate at boundaries (adapters)**
   ```java
   // Adapter translates HTTP → Domain → Database
   @PostMapping("/withdraw")
   public void withdraw(WithdrawalRequest request) {  // HTTP model
       withdrawalService.withdrawMoney(request.getAmount());  // Domain
   }
   ```

### ❌ DON'T

1. **Don't leak infrastructure into domain**
   ```java
   // BAD
   public class WithdrawalService extends BaseService { ... }
   
   // GOOD
   public class WithdrawalService { ... }
   ```

2. **Don't use domain entities as DTOs**
   ```java
   // BAD
   @GetMapping
   public Account getAccount() { return account; }
   
   // GOOD
   @GetMapping
   public AccountDTO getAccount() { return accountMapper.toDTO(account); }
   ```

3. **Don't let domain depend on Spring**
   ```java
   // BAD
   @Autowired
   private AccountRepository repo;
   
   // GOOD
   public WithdrawalService(AccountRepository repo) {
       this.repo = repo;
   }
   ```

4. **Don't mix different layers' concerns**
   ```java
   // BAD - Controller with business logic
   @PostMapping
   public void withdraw() {
       if (balance >= amount) {  // Business logic!
           balance -= amount;
       }
   }
   
   // GOOD - Controller delegates to domain service
   @PostMapping
   public void withdraw() {
       withdrawalService.withdrawMoney(accountId, amount);
   }
   ```

5. **Don't expose JPA entities directly**
   ```java
   // BAD
   @GetMapping
   public AccountEntity getAccount() { return entity; }
   
   // GOOD
   @GetMapping
   public AccountDTO getAccount() { return mapper.toDTO(entity); }
   ```

---

## Testing Benefits of DDD & Hexagonal Architecture

### Unit Testing Domain Logic (No Framework Needed)

```java
@Test
public void testWithdrawalWithInsufficientFunds() {
    // Pure Java - no Spring, no database
    Account account = new Account(1L, "123456", new BigDecimal("100.00"));
    
    assertThrows(IllegalStateException.class, () -> {
        account.withdraw(new BigDecimal("200.00"));
    });
}
```

### Integration Testing with Mocked Repositories

```java
@Test
public void testWithdrawalService() {
    // Mock the adapter
    AccountRepository mockRepo = mock(AccountRepository.class);
    Account testAccount = new Account(1L, "123456", new BigDecimal("500.00"));
    when(mockRepo.findById(1L)).thenReturn(Optional.of(testAccount));
    
    WithdrawalService service = new WithdrawalService(mockRepo, transactionRepo);
    service.withdrawMoney(1L, new BigDecimal("50.00"));
    
    verify(mockRepo).save(any(Account.class));
}
```

### No Database Needed for Domain Tests
- Tests run in milliseconds
- No configuration required
- Tests focus on business logic
- Easy to understand what's being tested

---

## Conclusion

By following DDD and Hexagonal Architecture, this banking system achieves:

1. **Testability**: Domain logic is testable without Spring or databases
2. **Maintainability**: Business logic is centralized and easy to find
3. **Flexibility**: Can swap databases, REST → GraphQL, etc. without touching domain
4. **Clarity**: Code structure reflects the business domain
5. **Scalability**: Easy to add new adapters for new use cases

The key insight: **Your business logic should not depend on your technology choices.**

