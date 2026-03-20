# ArchUnit Hexagonal Architecture Test Suite

**Date**: March 20, 2026  
**Component**: Architecture Testing  
**Framework**: ArchUnit + JUnit 5  
**Purpose**: Enforce strict hexagonal architecture rules  

---

## 📋 Overview

This document describes the architecture test suite implemented with **ArchUnit** to enforce hexagonal (ports & adapters) architecture rules in the banking system.

### What is ArchUnit?

ArchUnit is a library for testing architecture rules in Java applications. It allows you to:
- Define architectural rules programmatically
- Run them as JUnit tests
- Prevent architectural violations automatically
- Document architectural decisions in code

### Why Architecture Testing?

Architecture violations can:
- Leak framework dependencies into domain logic
- Create tight coupling
- Make testing harder
- Reduce code reusability
- Make future changes risky

**ArchUnit catches these before they reach production.** ✅

---

## 🏗️ Hexagonal Architecture Rules Enforced

### The Core Principle

```
    External Systems (HTTP, Database, UI)
              ↓ ↑
    ┌─ Adapters Layer ─────────────────┐
    │ (Spring, JPA, Controllers)       │
    │ ALLOWED dependencies:             │
    │ • Spring framework                │
    │ • Database/JPA                    │
    │ • External libraries              │
    └─────── ↑ ↓ ────────────────────┘
             │ │
             │ └─ Can depend on Domain ✅
             │
    ┌─ Domain Layer ────────────────────┐
    │ (Pure Business Logic)             │
    │ FORBIDDEN dependencies:           │
    │ • Spring framework              ❌
    │ • Adapters                      ❌
    │ • Any external framework        ❌
    │ ALLOWED dependencies:            │
    │ • Java standard library          │
    │ • Custom domain code             │
    └───────────────────────────────────┘
```

---

## 📐 Architectural Rules

### Rule 1: Domain Must NOT Depend on Adapters

**What it prevents:**
```java
// ❌ VIOLATES RULE - Domain depends on adapter
package com.banking.domain.service;

import com.banking.adapter.out.persistence.AccountRepositoryAdapter;

public class WithdrawalService {
    private AccountRepositoryAdapter adapter;  // ❌ FAILS
}
```

**Why it matters:**
- Domain should be independent of specific implementations
- Domain logic should be reusable across different adapters
- Testing domain becomes hard if it depends on adapters

**Correct approach:**
```java
// ✅ CORRECT - Domain depends on port (interface)
package com.banking.domain.service;

import com.banking.domain.port.AccountRepository;  // Port, not adapter

public class WithdrawalService {
    private final AccountRepository repository;  // Depends on interface
    
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }
}
```

---

### Rule 2: Domain Must NOT Depend on Spring

**What it prevents:**
```java
// ❌ VIOLATES RULE - Domain has Spring annotations
package com.banking.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class WithdrawalService {
    @Autowired
    private AccountRepository repository;  // ❌ FAILS
}
```

**Why it matters:**
- Domain logic should work without Spring
- Can reuse domain in non-Spring contexts (CLI, batch, other frameworks)
- Domain tests run faster without Spring bootstrap
- Framework-agnostic code is more maintainable

**Correct approach:**
```java
// ✅ CORRECT - Domain is pure Java
package com.banking.domain.service;

public class WithdrawalService {
    private final AccountRepository repository;  // No Spring!
    
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }
    
    public void withdrawMoney(Long accountId, BigDecimal amount) {
        Account account = repository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        
        account.withdraw(amount);
        
        repository.save(account);
    }
}
```

**Where Spring IS used (correctly):**
```java
// ✅ CORRECT - Adapter uses Spring
package com.banking.adapter.in.web;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Component;
import com.banking.domain.service.WithdrawalService;

@RestController
@Component
public class WithdrawalController {
    private final WithdrawalService service;  // Uses domain service
    
    public WithdrawalController(WithdrawalService service) {
        this.service = service;
    }
}
```

---

### Rule 3: Domain Must NOT Depend on JPA/Jakarta

**What it prevents:**
```java
// ❌ VIOLATES RULE - Domain model depends on JPA
package com.banking.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Account {
    @Id
    private Long id;  // ❌ FAILS
}
```

**Why it matters:**
- Domain models should be persistence-agnostic
- Easy to switch from JPA to MongoDB, GraphQL, etc.
- Domain models are reusable in different contexts
- Testing doesn't require database setup

**Correct approach:**
```java
// ✅ CORRECT - Domain is pure Java
package com.banking.domain.model;

public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private AccountStatus status;
    
    public Account(Long id, String accountNumber, BigDecimal balance) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.status = AccountStatus.ACTIVE;
    }
    
    public void withdraw(BigDecimal amount) {
        if (!isActive()) throw new InactiveAccountException(this.id);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) 
            throw new InvalidWithdrawalAmountException(amount);
        if (this.balance.compareTo(amount) < 0)
            throw new InsufficientFundsException(this.id, amount, this.balance);
        
        this.balance = this.balance.subtract(amount);
    }
}
```

**Where JPA IS used (correctly):**
```java
// ✅ CORRECT - Adapter uses JPA
package com.banking.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import com.banking.domain.model.Account;

@Entity
public class AccountEntity {
    @Id
    private Long id;
    
    public Account toDomain() {
        return new Account(id, accountNumber, balance);
    }
}
```

---

## 🧪 Test File Structure

### File Location
```
backend-spring/
└── src/test/java/com/banking/architecture/
    └── HexagonalArchitectureTest.java
```

### Test Class Components

**1. Annotations:**
```java
@AnalyzeClasses(packages = "com.banking")
public class HexagonalArchitectureTest {
    // AnalyzeClasses tells ArchUnit which packages to scan
}
```

**2. Rules (static fields):**
```java
@ArchTest
static final ArchRule domain_must_not_depend_on_adapters = 
    noClasses()
        .that()
        .resideInAPackage("com.banking.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.banking.adapter..");
```

**3. Rule Structure:**
- `@ArchTest` - Marks it as an architecture test
- `static final` - Rules must be static
- Returns `ArchRule` - The architectural constraint

---

## 🚀 Running the Tests

### Command Line
```bash
# Run all tests (including architecture tests)
mvn test

# Run only architecture tests
mvn test -Dtest=HexagonalArchitectureTest

# Run only this test class
mvn test -Dtest=HexagonalArchitectureTest#domain_must_not_depend_on_adapters
```

### In IDE
1. Right-click on `HexagonalArchitectureTest.java`
2. Select "Run Tests" or "Debug Tests"
3. See results in test runner

### Expected Output (Success)
```
[INFO] Running com.banking.architecture.HexagonalArchitectureTest
[INFO] Tests run: 3, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.456 s - in com.banking.architecture.HexagonalArchitectureTest
[INFO] BUILD SUCCESS
```

### Expected Output (Violation)
```
[ERROR] domain_must_not_depend_on_adapters:
Rule violated:
  - Class com.banking.domain.service.WithdrawalService 
    should not depend on classes that reside in package 
    'com.banking.adapter..' but does depend on 
    'com.banking.adapter.out.persistence.AccountRepositoryAdapter'
    
[INFO] BUILD FAILURE
```

---

## 📦 Maven Dependency

The `pom.xml` includes:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.0.1</version>
    <scope>test</scope>
</dependency>
```

**Why JUnit 5 version?**
- Native JUnit 5 support
- Uses `@ArchTest` annotation
- No extra configuration needed
- Better integration with modern test runners

---

## 🔍 Detailed Rule Explanation

### Rule 1: No Domain → Adapter Dependencies
```java
@ArchTest
static final ArchRule domain_must_not_depend_on_adapters =
    noClasses()                    // No classes...
        .that()
        .resideInAPackage("com.banking.domain..")  // ...in domain package
        .should()
        .dependOnClassesThat()     // ...should depend on classes that...
        .resideInAPackage("com.banking.adapter..");  // ...are in adapter package
```

**Translation:** If a class is in `com.banking.domain.*`, it cannot import or use any class from `com.banking.adapter.*`.

**How it's checked:**
- Scans all `.class` files in domain package
- Checks their bytecode for dependencies
- If ANY domain class imports adapter class → FAILS

---

### Rule 2: No Domain → Spring Dependencies
```java
@ArchTest
static final ArchRule domain_must_not_depend_on_spring =
    noClasses()
        .that()
        .resideInAPackage("com.banking.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "org.springframework.boot..",
            "org.springframework.stereotype..",
            "org.springframework.beans..",
            "org.springframework.context..",
            "org.springframework.data..",
            "org.springframework.web.."
        );
```

**Translation:** Domain cannot use ANY Spring packages.

**Packages blocked:**
- `org.springframework` - Core
- `org.springframework.boot` - Boot-specific
- `org.springframework.stereotype` - Annotations (@Service, @Component)
- `org.springframework.beans` - Bean annotations (@Autowired)
- `org.springframework.context` - Context annotations
- `org.springframework.data` - Data JPA
- `org.springframework.web` - Web MVC

---

### Rule 3: No Domain → JPA Dependencies
```java
@ArchTest
static final ArchRule domain_must_not_depend_on_jpa =
    noClasses()
        .that()
        .resideInAPackage("com.banking.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "jakarta.persistence..",
            "javax.persistence.."
        );
```

**Translation:** Domain cannot use Jakarta or Javax persistence APIs.

**Why both?**
- `jakarta.persistence` - Newer (JPA 3.0+)
- `javax.persistence` - Legacy (JPA 2.x)

---

## 🎯 Common Violations & Fixes

### Violation #1: @Service in Domain Service
```java
// ❌ WRONG
package com.banking.domain.service;

import org.springframework.stereotype.Service;

@Service
public class WithdrawalService {
    // Error: should not depend on 'org.springframework.stereotype.Service'
}
```

**Fix:**
```java
// ✅ CORRECT
package com.banking.domain.service;

public class WithdrawalService {
    private final AccountRepository repository;
    
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }
}
```

**Then make it a Spring bean in the adapter layer:**
```java
// ✅ CORRECT - In adapter layer
package com.banking.adapter.out.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.banking.domain.service.WithdrawalService;

@Configuration
public class DomainConfig {
    
    @Bean
    public WithdrawalService withdrawalService(AccountRepository repository) {
        return new WithdrawalService(repository);
    }
}
```

---

### Violation #2: @Autowired in Domain Service
```java
// ❌ WRONG
package com.banking.domain.service;

import org.springframework.beans.factory.annotation.Autowired;

public class WithdrawalService {
    @Autowired
    private AccountRepository repository;
    
    // Error: should not depend on 'org.springframework.beans.factory.annotation.Autowired'
}
```

**Fix: Use constructor injection in adapter layer**
```java
// ✅ CORRECT - Domain is constructor-based
package com.banking.domain.service;

public class WithdrawalService {
    private final AccountRepository repository;
    
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }
}

// ✅ CORRECT - Adapter creates with Spring
package com.banking.adapter.out.config;

@Configuration
public class DomainConfig {
    
    @Bean
    public WithdrawalService withdrawalService(AccountRepository repository) {
        return new WithdrawalService(repository);  // Spring creates it
    }
}
```

---

### Violation #3: JPA Annotations in Domain Model
```java
// ❌ WRONG
package com.banking.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Account {
    @Id
    private Long id;
    
    // Error: should not depend on 'jakarta.persistence.Entity'
}
```

**Fix: Separate domain model from persistence entity**
```java
// ✅ CORRECT - Pure domain model
package com.banking.domain.model;

public class Account {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    
    public Account(Long id, String accountNumber, BigDecimal balance) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
}

// ✅ CORRECT - Persistence entity in adapter
package com.banking.adapter.out.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import com.banking.domain.model.Account;

@Entity
public class AccountEntity {
    @Id
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    
    public Account toDomain() {
        return new Account(id, accountNumber, balance);
    }
    
    public static AccountEntity fromDomain(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.id = account.getId();
        entity.accountNumber = account.getAccountNumber();
        entity.balance = account.getBalance();
        return entity;
    }
}
```

---

## 📊 Test Coverage

The test suite covers:

| Rule | Coverage | Risk Level |
|------|----------|------------|
| Domain ↛ Adapters | All domain packages to all adapter packages | 🔴 Critical |
| Domain ↛ Spring | All domain packages to all Spring packages | 🔴 Critical |
| Domain ↛ JPA | All domain packages to JPA packages | 🔴 Critical |

**Why critical?**
- These are the core architecture constraints
- Violations severely impact testability and reusability
- Hard to refactor once violated

---

## 🔄 Integration with CI/CD

### GitHub Actions Example
```yaml
name: Architecture Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run Architecture Tests
        run: mvn test -Dtest=HexagonalArchitectureTest
```

### Jenkins Pipeline Example
```groovy
stage('Architecture Tests') {
    steps {
        sh 'mvn test -Dtest=HexagonalArchitectureTest'
    }
}
```

### GitLab CI Example
```yaml
architecture-tests:
  stage: test
  script:
    - mvn test -Dtest=HexagonalArchitectureTest
  allow_failure: false
```

---

## 📈 Metrics & Monitoring

### Pre-Commit Hook (optional)
```bash
#!/bin/bash
# .git/hooks/pre-commit

mvn test -Dtest=HexagonalArchitectureTest
if [ $? -ne 0 ]; then
    echo "Architecture tests failed! Commit blocked."
    exit 1
fi
```

### Pull Request Check
- Include architecture tests in PR checks
- Fail build if architecture rules violated
- Require team review if relaxing rules

---

## 🎓 Best Practices

### 1. Run Tests Frequently
```bash
# Run before committing
mvn test -Dtest=HexagonalArchitectureTest

# Run in IDE before commit
Right-click → Run Tests
```

### 2. Document Violations
If a violation is found:
- Understand why it happened
- Refactor to fix it
- Document the architectural decision
- Add to team guidelines

### 3. Evolve Rules Over Time
```java
// Add new rules as architecture evolves
@ArchTest
static final ArchRule no_util_classes_in_domain =
    noClasses()
        .that()
        .resideInAPackage("com.banking.domain..")
        .should()
        .haveSimpleNameEndingWith("Util");
```

### 4. Review Architecture Regularly
- Monthly: Review test results
- Quarterly: Discuss architecture with team
- Annually: Evaluate if rules still make sense

---

## 🆘 Troubleshooting

### "Rule violated" in Test Output
1. Read the error message carefully
2. Find the offending class
3. Refactor to remove the forbidden dependency
4. Re-run tests

### "Test not found" Error
1. Check package name: `com.banking.architecture`
2. Check file location: `src/test/java/...`
3. Run Maven clean: `mvn clean test`

### Slow Test Execution
1. Normal - First run scans all classes
2. Subsequent runs are cached
3. Consider running only when needed

---

## ✅ Quality Assurance Checklist

- [x] ArchUnit dependency added to pom.xml
- [x] Test class created in correct package
- [x] All 3 critical rules implemented
- [x] Rules have comprehensive JavaDoc
- [x] Rules follow naming convention
- [x] No build dependencies broken
- [x] Tests pass with current codebase
- [x] Ready for CI/CD integration

---

## 📚 References

- [ArchUnit Official Documentation](https://www.archunit.org/)
- [ArchUnit JUnit 5 Integration](https://www.archunit.org/userguide/html/000_Index.html#_junit5_integration)
- [Hexagonal Architecture](https://herbertograca.com/2017/09/28/hexagonal-architecture/)
- [Ports and Adapters Pattern](https://en.wikipedia.org/wiki/Hexagonal_architecture)

---

## 📞 Support

For questions about:
- **ArchUnit syntax**: See official documentation
- **Architecture rules**: Ask your team lead
- **Refactoring violations**: See Common Violations section above

---

**Status**: ✅ COMPLETE  
**Date**: March 20, 2026  
**Ready for**: Production Use  
**Next**: Integrate into CI/CD pipeline

