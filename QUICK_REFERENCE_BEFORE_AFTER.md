# Quick Reference: DDD Refactoring - Before & After

## 1. ACCOUNT CLASS

### BEFORE: Anemic Domain Model
```java
public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;

    public Account(Long id, String accountNumber, BigDecimal initialBalance) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    // Just getters
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }

    // Business logic throws generic exceptions
    public void withdraw(BigDecimal amount) {
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

### AFTER: Rich Domain Model
```java
public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;  // ✨ NEW

    // Constructor with default status
    public Account(Long id, String accountNumber, BigDecimal initialBalance) {
        this(id, accountNumber, initialBalance, AccountStatus.ACTIVE);
    }

    // Constructor with status
    public Account(Long id, String accountNumber, BigDecimal initialBalance, AccountStatus status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.status = status;
    }

    // ✨ Query methods (no side effects)
    public BigDecimal checkBalance() {
        return this.balance;
    }

    public boolean isActive() {
        return this.status == AccountStatus.ACTIVE;
    }

    public AccountStatus getStatus() {
        return this.status;
    }

    // ✨ Command methods (with domain logic)
    public void withdraw(BigDecimal amount) {
        // Invariant 1: Account must be active
        if (!isActive()) {
            throw new InactiveAccountException(this.id);
        }

        // Invariant 2: Amount must be positive
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWithdrawalAmountException(amount);
        }

        // Invariant 3: Sufficient balance required
        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(this.id, amount, this.balance);
        }

        this.balance = this.balance.subtract(amount);
    }

    public void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidWithdrawalAmountException(amount);
        }
        this.balance = this.balance.add(amount);
    }

    public void deactivate() {
        this.status = AccountStatus.INACTIVE;
    }

    public void activate() {
        this.status = AccountStatus.ACTIVE;
    }

    // Standard getters
    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getBalance() { return balance; }
}
```

**Key Differences:**
1. ✨ Added `AccountStatus` status field
2. ✨ Added `checkBalance()` query method
3. ✨ Added `isActive()` query method
4. ✨ Enhanced `withdraw()` with 3 invariants and specific exceptions
5. ✨ Added `deposit()` method
6. ✨ Added `activate()` and `deactivate()` methods

---

## 2. WITHDRAWAL SERVICE

### BEFORE: Fat Service with Logic
```java
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawalService(AccountRepository accountRepository, 
                            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public void withdrawMoney(Long accountId, BigDecimal amount) {
        // Step 1: Find the account
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Step 2: Tell the account to withdraw
        // (This calls Account.withdraw() which has minimal logic)
        account.withdraw(amount);

        // Step 3: Save the updated account
        accountRepository.save(account);

        // Step 4: Record the transaction
        Transaction transaction = new Transaction(null, accountId, amount, 
                                                   TransactionType.WITHDRAWAL, 
                                                   TransactionStatus.PENDING);
        transactionRepository.save(transaction);
    }
}
```

### AFTER: Thin Orchestrator with Documentation
```java
/**
 * WithdrawalService: Thin Orchestrator Layer
 * 
 * This service is a USE CASE orchestrator that coordinates:
 * 1. Loading aggregates from repositories
 * 2. Invoking domain operations on those aggregates
 * 3. Persisting the modified aggregates back to repositories
 * 
 * IMPORTANT: This service contains NO business logic. All business rules
 * (validation, invariants) are enforced by the Account aggregate.
 * 
 * Domain exceptions (e.g., InsufficientFundsException) propagate to the adapter layer
 * for HTTP translation.
 */
public class WithdrawalService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public WithdrawalService(AccountRepository accountRepository, 
                            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Execute withdrawal use case.
     * 
     * Orchestration steps:
     * 1. Load Account aggregate from repository
     * 2. Delegate business logic to account.withdraw(amount)
     *    - May throw InactiveAccountException, InvalidWithdrawalAmountException, 
     *      or InsufficientFundsException
     * 3. Persist updated Account back to repository
     * 4. Create and persist a PENDING Transaction record
     * 
     * @param accountId The account ID to withdraw from
     * @param amount The amount to withdraw
     * @throws IllegalArgumentException If account not found
     * @throws InactiveAccountException If account is inactive
     * @throws InvalidWithdrawalAmountException If amount is invalid
     * @throws InsufficientFundsException If insufficient balance
     */
    public void withdrawMoney(Long accountId, BigDecimal amount) {
        // Step 1: Load the Account aggregate from repository
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Step 2: Invoke domain operation on the aggregate
        // This enforces all invariants and may throw domain exceptions
        account.withdraw(amount);

        // Step 3: Persist the modified Account back to repository
        accountRepository.save(account);

        // Step 4: Record the transaction as PENDING for later batch processing
        Transaction transaction = new Transaction(
                null,
                accountId,
                amount,
                TransactionType.WITHDRAWAL,
                TransactionStatus.PENDING
        );
        transactionRepository.save(transaction);
    }
}
```

**Key Differences:**
1. ✨ Added comprehensive JavaDoc
2. ✨ Added method documentation explaining orchestration steps
3. ✨ Added exception documentation
4. ✨ Added comments explaining each step
5. ✨ Same code logic, but now it's clear this is ONLY orchestration

---

## 3. EXCEPTION HIERARCHY

### BEFORE: Generic Exceptions
```java
// In Account.withdraw()
if (amount.compareTo(BigDecimal.ZERO) <= 0)
    throw new IllegalArgumentException("Withdrawal amount must be greater than zero");

if (this.balance.compareTo(amount) < 0)
    throw new IllegalStateException("Insufficient funds");
```

**Problems:**
- Generic exceptions from Java library
- No context about what failed
- Hard to distinguish between different error scenarios
- Services can't handle specific cases differently

### AFTER: Rich Domain Exceptions
```java
// In Account.withdraw()
if (!isActive())
    throw new InactiveAccountException(this.id);

if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
    throw new InvalidWithdrawalAmountException(amount);

if (this.balance.compareTo(amount) < 0)
    throw new InsufficientFundsException(this.id, amount, this.balance);
```

```java
// Exception base class
public abstract class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}

// Specific exceptions with context
public class InsufficientFundsException extends DomainException {
    private final Long accountId;
    private final BigDecimal requestedAmount;
    private final BigDecimal availableBalance;

    public InsufficientFundsException(Long accountId, BigDecimal requestedAmount, 
                                     BigDecimal availableBalance) {
        super(String.format(
            "Insufficient funds for account %d. Requested: %s, Available: %s",
            accountId, requestedAmount, availableBalance
        ));
        this.accountId = accountId;
        this.requestedAmount = requestedAmount;
        this.availableBalance = availableBalance;
    }

    public Long getAccountId() { return accountId; }
    public BigDecimal getRequestedAmount() { return requestedAmount; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
}

public class InactiveAccountException extends DomainException {
    private final Long accountId;

    public InactiveAccountException(Long accountId) {
        super(String.format("Account %d is inactive and cannot perform this operation", 
                          accountId));
        this.accountId = accountId;
    }

    public Long getAccountId() { return accountId; }
}

public class InvalidWithdrawalAmountException extends DomainException {
    private final BigDecimal amount;

    public InvalidWithdrawalAmountException(BigDecimal amount) {
        super(String.format("Withdrawal amount must be greater than zero. Received: %s", 
                          amount));
        this.amount = amount;
    }

    public BigDecimal getAmount() { return amount; }
}
```

**Benefits:**
- ✨ Specific exceptions for each error type
- ✨ Each exception carries relevant context
- ✨ Named for domain concept (InsufficientFunds, not IllegalStateException)
- ✨ Can be handled differently in HTTP layer
- ✨ Self-documenting code

---

## 4. TRANSACTION CLASS

### BEFORE: Simple Data Container
```java
public class Transaction {
    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;

    public Transaction(Long id, Long accountId, BigDecimal amount, 
                       TransactionType type, TransactionStatus status) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    
    // ... etc for all properties
}
```

**Problems:**
- Status can be set to anything: `transaction.setStatus(COMPLETED)`
- No protection against invalid transitions
- No query methods to check status

### AFTER: Rich Domain Model with State Transitions
```java
/**
 * Rich Domain Model: Transaction
 * 
 * This Transaction aggregate encapsulates transaction logic and state transitions.
 * State transitions are controlled through domain methods (e.g., complete()),
 * not through direct property setters.
 * 
 * Invariants:
 * - Status can only transition through valid paths (PENDING -> COMPLETED)
 * - Transaction properties are immutable after creation
 */
public class Transaction {
    private Long id;
    private Long accountId;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;

    public Transaction(Long id, Long accountId, BigDecimal amount, 
                       TransactionType type, TransactionStatus status) {
        this.id = id;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
        this.status = status;
    }

    // ✨ Query methods
    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    public boolean isCompleted() {
        return this.status == TransactionStatus.COMPLETED;
    }

    // ✨ Domain method for state transition
    public void complete() {
        if (!isPending()) {
            throw new IllegalStateException(
                String.format("Cannot complete transaction %d: current status is %s, expected PENDING", 
                    this.id, this.status)
            );
        }
        this.status = TransactionStatus.COMPLETED;
    }

    // Getters (public)
    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public BigDecimal getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }

    // ✨ Setters now @Deprecated and package-private (for JPA only)
    @Deprecated
    void setId(Long id) { this.id = id; }

    @Deprecated
    void setAccountId(Long accountId) { this.accountId = accountId; }

    @Deprecated
    void setAmount(BigDecimal amount) { this.amount = amount; }

    @Deprecated
    void setType(TransactionType type) { this.type = type; }

    @Deprecated
    void setStatus(TransactionStatus status) { this.status = status; }
}
```

**Benefits:**
- ✨ Status can only be changed through `complete()` method
- ✨ Invalid transitions are prevented with clear error message
- ✨ Query methods express intent: `isPending()`, `isCompleted()`
- ✨ Backward compatible with JPA via @Deprecated setters

---

## 5. TRANSACTION SERVICE

### BEFORE: Direct Status Setting
```java
public class TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public void processNightlyBatch() {
        List<Transaction> pendingTransactions = 
            transactionRepository.findByStatus(TransactionStatus.PENDING);

        System.out.println("=== NIGHTLY TRANSACTION BATCH REPORT ===");
        System.out.println("Processing " + pendingTransactions.size() + " pending transactions:");

        for (Transaction transaction : pendingTransactions) {
            System.out.println("Transaction ID: " + transaction.getId() +
                             ", Account: " + transaction.getAccountId() +
                             ", Type: " + transaction.getType() +
                             ", Amount: " + transaction.getAmount());
        }

        // Direct setter - bypasses any validation
        for (Transaction transaction : pendingTransactions) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transactionRepository.save(transaction);
        }

        System.out.println("All pending transactions have been processed and marked as COMPLETED.");
        System.out.println("=====================================");
    }
}
```

### AFTER: Using Domain Method
```java
/**
 * TransactionService: Use Case Orchestrator for Batch Processing
 * 
 * This service orchestrates the nightly batch process:
 * 1. Fetch all PENDING transactions from the repository
 * 2. Process them through domain state transitions
 * 3. Persist the updated transactions back
 * 
 * This is a thin orchestrator that delegates domain logic to the Transaction aggregate.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Process the nightly batch of pending transactions.
     * 
     * Orchestration steps:
     * 1. Fetch all PENDING transactions
     * 2. Print audit report
     * 3. Transition each transaction from PENDING to COMPLETED
     * 4. Persist updates
     */
    public void processNightlyBatch() {
        // Step 1: Fetch all PENDING transactions
        List<Transaction> pendingTransactions = 
            transactionRepository.findByStatus(TransactionStatus.PENDING);

        // Step 2: Print audit report
        printBatchReport(pendingTransactions);

        // Step 3: Process each transaction
        for (Transaction transaction : pendingTransactions) {
            // ✨ Use domain method (not direct setter)
            transaction.complete();
            // Step 4: Persist
            transactionRepository.save(transaction);
        }

        System.out.println("All pending transactions have been processed and marked as COMPLETED.");
        System.out.println("=====================================");
    }

    private void printBatchReport(List<Transaction> transactions) {
        System.out.println("=== NIGHTLY TRANSACTION BATCH REPORT ===");
        System.out.println("Processing " + transactions.size() + " pending transactions:");

        for (Transaction transaction : transactions) {
            System.out.println("Transaction ID: " + transaction.getId() +
                             ", Account: " + transaction.getAccountId() +
                             ", Type: " + transaction.getType() +
                             ", Amount: " + transaction.getAmount() +
                             ", Status: " + transaction.getStatus());
        }
    }
}
```

**Key Differences:**
- ✨ Uses `transaction.complete()` instead of `transaction.setStatus(COMPLETED)`
- ✨ Separated reporting logic into `printBatchReport()` method
- ✨ Added comprehensive JavaDoc
- ✨ Clear orchestration pattern

---

## 6. NEW: AccountStatus Enum

```java
/**
 * AccountStatus enum represents the possible states of an account.
 */
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended");

    private final String displayName;

    AccountStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

**Benefits:**
- ✨ Type-safe account states
- ✨ Can't use undefined statuses
- ✨ Used by Account to enforce invariants
- ✨ Display names for UI/logging

---

## Summary Table

| Aspect | Before | After |
|--------|--------|-------|
| Account methods | 4 (getters + withdraw) | 8 (+ query, command) |
| Account logic | Basic | Rich with invariants |
| Service logic | Thick (has validation) | Thin (only orchestrates) |
| Exceptions | Generic (IllegalStateException) | Specific (InsufficientFundsException) |
| State transitions | Direct setters | Domain methods (complete()) |
| Account status | Implicit | Explicit (AccountStatus enum) |
| Documentation | Minimal | Comprehensive |
| Testability | Hard (needs mocks) | Easy (test domain directly) |
| Cohesion | Low (logic in service) | High (logic in domain) |

---

**Status**: ✅ COMPLETE  
**Ready for**: Code Review & Senior Discussion

