# Meeting Preparation: What to Present

**Date**: March 20, 2026  
**Meeting Duration**: 20-30 minutes  
**Attendee**: Senior Engineer/Architect

---

## 📋 Agenda

### Part 1: Context (2-3 min)
- Current state: Anemic domain model
- Problem: Business logic scattered in services
- Solution: Rich domain model with DDD

### Part 2: Changes Overview (3-5 min)
- 4 new exception classes
- 1 new enum (AccountStatus)
- 3 refactored domain classes
- 2 refactored services

### Part 3: Technical Deep Dive (10-15 min)
- Show Account.java with new methods
- Show exception hierarchy
- Show orchestrator pattern
- Discuss invariants

### Part 4: Discussion (5-10 min)
- Benefits and tradeoffs
- Production readiness
- Next steps
- Questions

---

## 🎯 Opening Statement

**What to say:**
> "I've refactored the banking system from an anemic domain model to a rich domain model following Domain-Driven Design principles. The key insight is that Account should own the business logic, not the service. This makes the code more cohesive, testable, and self-documenting."

**Show them this progression:**

```
BEFORE: Anemic Account
┌────────────────┐
│ Account        │  Just a data container
│ - id           │  No behavior
│ - accountNo    │  Logic lives elsewhere
│ - balance      │
└────────────────┘

          ↓

AFTER: Rich Account
┌────────────────────────────────┐
│ Account                        │
│ - id                           │
│ - accountNo                    │
│ - balance                      │
│ - status                       │  Has behavior!
│                                │  Validates itself!
│ + withdraw(amount)             │  Owns business rules!
│ + deposit(amount)              │
│ + checkBalance()               │
│ + isActive()                   │
│ + activate()                   │
│ + deactivate()                 │
└────────────────────────────────┘
```

---

## 📊 Talking Points

### Point 1: Move Logic to the Right Place

**Show this diagram:**

```
Anemic Model (❌ WRONG):
┌─────────────────────────┐      ┌────────────────┐
│ WithdrawalService       │      │ Account        │
│ - if balance >= amount? │ ───► │ - balance      │
│ - validate active?      │      │ - getBalance() │
│ - check positive?       │      └────────────────┘
│ - calculate new balance │
└─────────────────────────┘
Logic here! (Wrong!)       Just data! (Wrong!)

Rich Model (✅ CORRECT):
┌──────────────────────────┐    ┌──────────────────────────┐
│ WithdrawalService        │    │ Account                  │
│ - Get account from repo  │    │ - balance                │
│ - Call account.withdraw()├───►│ - status                 │
│ - Save account to repo   │    │ - withdraw()   ✨ Logic! │
│ - Create transaction     │    │ - isActive()   ✨ Logic! │
│                          │    │ - activate()   ✨ Logic! │
└──────────────────────────┘    └──────────────────────────┘
Pure orchestration! (Right!) Domain logic! (Right!)
```

**Say:**
> "The service's job is to say 'I need to withdraw money from this account.' The account's job is to say 'I'll validate this and do it, or throw a specific error if I can't.'"

---

### Point 2: Invariants are Explicit

**Show the code:**
```java
public void withdraw(BigDecimal amount) {
    // Invariant 1: Must be active
    if (!isActive()) 
        throw new InactiveAccountException(this.id);
    
    // Invariant 2: Amount must be positive
    if (amount.compareTo(BigDecimal.ZERO) <= 0)
        throw new InvalidWithdrawalAmountException(amount);
    
    // Invariant 3: Sufficient balance
    if (this.balance.compareTo(amount) < 0)
        throw new InsufficientFundsException(this.id, amount, this.balance);
    
    this.balance = this.balance.subtract(amount);
}
```

**Say:**
> "Every business rule is now explicit in one place. Anyone reading this method knows exactly what an account validates. This is the essence of DDD - making implicit rules explicit."

---

### Point 3: Specific Exceptions Enable Better Error Handling

**Show the mapping:**

| Exception | Cause | HTTP Status |
|-----------|-------|------------|
| InsufficientFundsException | Balance < Amount | 402 Payment Required |
| InactiveAccountException | Status != ACTIVE | 403 Forbidden |
| InvalidWithdrawalAmountException | Amount <= 0 | 400 Bad Request |

**Say:**
> "With generic exceptions (IllegalStateException), the controller doesn't know whether to return 402, 403, or 400. With specific domain exceptions, each error type maps naturally to the correct HTTP status code."

---

### Point 4: Testability Without Spring

**Show the comparison:**

```java
// BEFORE: Had to mock everything
@Test
public void testWithdrawal() {
    WithdrawalService service = new WithdrawalService(
        mockAccountRepository,
        mockTransactionRepository
    );
    service.withdrawMoney(1L, BigDecimal.valueOf(100));
    verify(mockAccountRepository).save(...);
}

// AFTER: Can test domain directly, no mocks!
@Test
public void testInsufficientFundsException() {
    Account account = new Account(1L, "ACC001", BigDecimal.valueOf(50));
    
    assertThrows(InsufficientFundsException.class, () ->
        account.withdraw(BigDecimal.valueOf(100))
    );
}
```

**Say:**
> "The domain logic is so well encapsulated that we can test it without any Spring, without any mocking, without any database. This is a hallmark of good domain-driven design."

---

### Point 5: Hexagonal Architecture Alignment

**Show this:**

```
┌──────────────────────────────────────────┐
│         HTTP/REST Layer (Spring)         │
│      - Controllers                       │
│      - Global Exception Handler          │
└────────────┬─────────────────────────────┘
             │  Maps domain exceptions
             │  to HTTP status codes
┌────────────▼─────────────────────────────┐
│      Domain Layer (Pure DDD)             │
│   ✨ NO Spring dependencies              │
│   ✨ NO HTTP knowledge                   │
│   ✨ Business logic only                 │
│                                          │
│   Account                                │
│   ├─ withdraw(amount)                    │
│   ├─ deposit(amount)                     │
│   └─ activate()/deactivate()            │
│                                          │
│   Transaction                           │
│   ├─ isPending()                        │
│   ├─ isCompleted()                      │
│   └─ complete()                         │
│                                          │
│   WithdrawalService (orchestrator)      │
│   └─ Calls domain methods               │
│                                          │
│   DomainException hierarchy             │
│   ├─ InsufficientFundsException         │
│   ├─ InactiveAccountException           │
│   └─ InvalidWithdrawalAmountException   │
└──────────────────────────────────────────┘
```

**Say:**
> "This is the beauty of hexagonal architecture. The domain layer knows nothing about HTTP, Spring, or databases. It's pure business logic. The adapters layer (Spring) translates between HTTP and domain concepts."

---

## 🎓 SOLID Principles Demonstrated

**When asked about SOLID:**

```
S - Single Responsibility
  ✅ Account owns account logic
  ✅ Service coordinates
  ✅ Exceptions represent specific failures

O - Open/Closed
  ✅ Can add new exceptions without modifying existing code
  ✅ Can add new Account methods without breaking existing ones

L - Liskov Substitution
  ✅ All domain exceptions extend DomainException
  ✅ Can handle them uniformly if needed

I - Interface Segregation
  ✅ AccountRepository interface is focused, not bloated
  ✅ Services depend on focused interfaces

D - Dependency Inversion
  ✅ Services depend on ports (AccountRepository interface)
  ✅ Not concrete implementations
  ✅ Can swap MySQL for MongoDB later
```

---

## ❓ Likely Questions & Answers

### Q1: "Isn't this overengineering for a simple withdrawal?"

**Answer:**
> "Today it's simple. Tomorrow it might need to check daily limits, check fraud patterns, check if the account is locked, apply fees, etc. The rich domain model makes these easy to add. The anemic model makes them scattered across services."

**Show:**
```java
// Easy to add new business rules
public void withdraw(BigDecimal amount, User user, Location location) {
    if (!isActive()) throw new InactiveAccountException(this.id);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new InvalidWithdrawalAmountException(amount);
    if (this.balance.compareTo(amount) < 0) throw new InsufficientFundsException(this.id, amount, this.balance);
    if (hasExceededDailyLimit(amount)) throw new DailyLimitExceededException(this.id, amount);  // NEW!
    if (isFraudulent(location)) throw new FraudulentActivityException(this.id, location);     // NEW!
    if (isAccountLocked()) throw new LockedAccountException(this.id);                         // NEW!
    
    this.balance = this.balance.subtract(amount);
}
```

---

### Q2: "What about backward compatibility with existing code?"

**Answer:**
> "The public interface is compatible. Services still call account.withdraw(). Getters still work. The difference is internal - the logic is now where it belongs."

**Show:**
```java
// Old code that called account.withdraw() still works
account.withdraw(amount);  // ✅ Still works

// New code can use query methods
if (account.isActive()) {  // ✅ New functionality
    // ...
}

// Setters still exist for JPA/ORM
account.setStatus(ACTIVE);  // ✅ Still works (marked @Deprecated)
```

---

### Q3: "How do we handle complex business rules?"

**Answer:**
> "Create a domain service for cross-aggregate logic. Single-aggregate logic stays on the aggregate. Here's an example."

**Show:**
```java
// Simple logic: stays on Account
account.withdraw(amount);  // Account decides if it can withdraw

// Complex logic: create a domain service
@Service
public class ComplexTransferService {
    public void transferWithVerification(Account from, Account to, BigDecimal amount) {
        // Coordinate between two aggregates
        from.withdraw(amount);
        to.deposit(amount);
        
        // Could add: notify external fraud service, update statistics, etc.
    }
}
```

---

### Q4: "How does this work with JPA/Hibernate?"

**Answer:**
> "Perfectly. JPA just needs the setters to map database columns to properties. We mark them @Deprecated to discourage direct use, but they still work. The domain methods take precedence in application code."

**Show:**
```java
public class Account {
    private BigDecimal balance;
    
    // Public domain method (application code)
    public void withdraw(BigDecimal amount) {
        // validation and logic
        this.balance = this.balance.subtract(amount);
    }
    
    // Package-private deprecated setter (JPA only)
    @Deprecated
    void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
```

---

### Q5: "Can we add more status values besides ACTIVE/INACTIVE?"

**Answer:**
> "Yes! The AccountStatus enum is designed to extend. We already have SUSPENDED. You can add more easily."

**Show:**
```java
public enum AccountStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive"),
    SUSPENDED("Suspended"),
    CLOSED("Closed"),           // ✨ NEW
    DORMANT("Dormant"),         // ✨ NEW
    FROZEN("Frozen");           // ✨ NEW
    
    // And the Account.withdraw() already checks for active
    public void withdraw(BigDecimal amount) {
        if (!isActive()) {
            throw new InactiveAccountException(this.id);
        }
        // ...
    }
}
```

---

## 📁 Files to Show During Meeting

**In order of impact:**

1. **DDD_RICH_MODEL_REFACTORING.md** (5 min) - Technical deep dive
2. **QUICK_REFERENCE_BEFORE_AFTER.md** (5 min) - Before/after comparison
3. **Account.java** (5 min) - Show the rich model
4. **WithdrawalService.java** (2 min) - Show thin service
5. **Exception classes** (2 min) - Show hierarchy

**Optional deeper dive:**
- DDD_HEXAGONAL_GUIDE.md - Architecture patterns
- TransactionService.java - Batch process example
- TransactionEntity & Repository - ORM mapping

---

## ✅ Preparation Checklist

- [ ] Read this entire file
- [ ] Read DDD_RICH_MODEL_REFACTORING.md
- [ ] Read QUICK_REFERENCE_BEFORE_AFTER.md
- [ ] Review Account.java in IDE
- [ ] Review WithdrawalService.java in IDE
- [ ] Run mvn clean compile (or check that code compiles)
- [ ] Prepare a simple use case example in mind
- [ ] Think about potential questions
- [ ] Have code repository open during meeting
- [ ] Write down 1-2 things you learned about DDD

---

## ⏰ Timing Guide

```
Total Time: 30 minutes

Opening/Context          : 3 min
Changes Overview         : 4 min
Technical Deep Dive      : 12 min
  ├─ Account methods     : 4 min
  ├─ Exception hierarchy : 3 min
  ├─ Orchestrator        : 2 min
  └─ Benefits            : 3 min
Discussion/Questions     : 8 min
Wrap-up/Next Steps       : 3 min
```

---

## 🎬 Demo Scenario

**If they ask for a demo:**

```java
// Show in IDE:

// 1. Create account
Account acc = new Account(1L, "ACC001", BigDecimal.valueOf(1000));

// 2. Show query methods work
System.out.println("Balance: " + acc.checkBalance());      // 1000
System.out.println("Is Active: " + acc.isActive());         // true

// 3. Show successful withdrawal
acc.withdraw(BigDecimal.valueOf(100));
System.out.println("After withdrawal: " + acc.checkBalance()); // 900

// 4. Show invariant protection - insufficient funds
try {
    acc.withdraw(BigDecimal.valueOf(2000));
} catch (InsufficientFundsException e) {
    System.out.println("Caught: " + e.getMessage());  // "Insufficient funds..."
}

// 5. Show invariant protection - inactive
acc.deactivate();
try {
    acc.withdraw(BigDecimal.valueOf(100));
} catch (InactiveAccountException e) {
    System.out.println("Caught: " + e.getMessage());  // "Account is inactive..."
}
```

---

## 🏆 Key Takeaway

**Practice saying this:**

> "This refactoring demonstrates that good architecture is about clear separation of concerns. Domain objects express business rules. Services coordinate the workflow. Exceptions communicate failures specifically. Each layer knows its job and does it well. This is the essence of Domain-Driven Design applied to a real banking system."

---

**Ready to present!** 🚀

Good luck with your senior review! The code is solid, the architecture is clean, and the documentation is comprehensive. You've got this!

