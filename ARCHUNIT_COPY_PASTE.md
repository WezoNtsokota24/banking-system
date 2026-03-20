# 🚀 ArchUnit Setup - Copy & Paste Reference

**Status**: ✅ Production Ready  
**Date**: March 20, 2026  

---

## 📦 MAVEN DEPENDENCY (Already Added)

Verify in `backend-spring/pom.xml`:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.0.1</version>
    <scope>test</scope>
</dependency>
```

---

## 📍 TEST FILE LOCATION

```
backend-spring/src/test/java/com/banking/architecture/HexagonalArchitectureTest.java
```

✅ **Already Created**

---

## 🧪 QUICK COMMANDS

### Run Architecture Tests
```bash
mvn test -Dtest=HexagonalArchitectureTest
```

### Run All Tests
```bash
mvn test
```

### Run with Verbose Output
```bash
mvn test -Dtest=HexagonalArchitectureTest -X
```

### Run in IDE
- Right-click on HexagonalArchitectureTest.java
- Select: Run Tests or Debug

---

## ✅ EXPECTED SUCCESS OUTPUT

```
[INFO] Running com.banking.architecture.HexagonalArchitectureTest
[INFO] Tests run: 3, Failures: 0, Errors: 0
[INFO] BUILD SUCCESS
```

---

## ❌ COMMON ERRORS & FIXES

### Error: "Test class not found"
```
Fix:
1. Check file location: src/test/java/com/banking/architecture/
2. Run: mvn clean test
3. Check package name in file
```

### Error: "Rule violated"
```
Fix:
1. Read error message carefully
2. Find the violating class
3. Remove forbidden dependency
4. Re-run tests
```

---

## 🏗️ THE 3 RULES

### Rule 1: No Domain → Adapter Dependencies
```
❌ Domain classes CANNOT import com.banking.adapter.*
✅ Adapters CAN import com.banking.domain.*
```

### Rule 2: No Domain → Spring
```
❌ Domain classes CANNOT use org.springframework.*
❌ Domain CANNOT have: @Service, @Autowired, @Component, etc.
✅ Adapters CAN use all Spring annotations
```

### Rule 3: No Domain → JPA
```
❌ Domain classes CANNOT use jakarta.persistence.*
❌ Domain CANNOT have: @Entity, @Id, @Column, etc.
✅ Adapters CAN use all JPA annotations
```

---

## ✅ PASSES (Correct Domain)

```java
// Pure Java, no framework
public class Account {
    private Long id;
    private BigDecimal balance;
    
    public void withdraw(BigDecimal amount) {
        // Business logic
    }
}

// Constructor injection only
public class WithdrawalService {
    private final AccountRepository repository;
    
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }
}
```

---

## ❌ FAILS (Violates Rules)

```java
// ❌ WRONG - Spring in domain
@Service
public class WithdrawalService { }

// ❌ WRONG - JPA in domain
@Entity
public class Account { }

// ❌ WRONG - Adapter in domain
import com.banking.adapter.AccountRepositoryImpl;

// ❌ WRONG - Autowired in domain
@Autowired
private AccountRepository repository;
```

---

## 🔧 COMMON VIOLATIONS & FIXES

### Violation 1: @Service in Domain
```java
// ❌ WRONG
@Service
public class WithdrawalService { }

// ✅ CORRECT
public class WithdrawalService {
    private final AccountRepository repository;
    public WithdrawalService(AccountRepository repository) {
        this.repository = repository;
    }
}
```

### Violation 2: @Entity in Domain
```java
// ❌ WRONG
@Entity
public class Account { }

// ✅ CORRECT - Domain Model
public class Account {
    private Long id;
}

// ✅ CORRECT - Persistence Entity in Adapter
@Entity
public class AccountEntity {
    public Account toDomain() { }
}
```

### Violation 3: Adapter Import in Domain
```java
// ❌ WRONG
import com.banking.adapter.AccountRepositoryImpl;

// ✅ CORRECT - Use Port (Interface)
import com.banking.domain.port.AccountRepository;
```

---

## 📋 QUICK CHECKLIST

- [ ] Run: `mvn test -Dtest=HexagonalArchitectureTest`
- [ ] See: 3 tests run, 0 failures
- [ ] Read: ARCHUNIT_QUICK_REFERENCE.md (quick start)
- [ ] Read: ARCHUNIT_TEST_GUIDE.md (comprehensive)
- [ ] Integrate: Add to CI/CD pipeline
- [ ] Train: Share with team

---

## 🎯 CI/CD INTEGRATION

### GitHub Actions
```yaml
- name: Architecture Tests
  run: mvn test -Dtest=HexagonalArchitectureTest
```

### Jenkins
```groovy
sh 'mvn test -Dtest=HexagonalArchitectureTest'
```

### GitLab CI
```yaml
script:
  - mvn test -Dtest=HexagonalArchitectureTest
```

---

## 📞 DOCUMENTATION FILES

| File | Use When |
|------|----------|
| **ARCHUNIT_QUICK_REFERENCE.md** | Need quick start |
| **ARCHUNIT_TEST_GUIDE.md** | Need detailed guide |
| **ARCHUNIT_DELIVERY.md** | Need complete info |
| **This file** | Need copy-paste examples |

---

## 🚀 ONE-MINUTE SETUP

```bash
# 1. Build
mvn clean install

# 2. Run tests
mvn test -Dtest=HexagonalArchitectureTest

# 3. See success
# Output: Tests run: 3, Failures: 0, Errors: 0 ✅
```

**That's it! Architecture is protected!** 🛡️

---

## ✨ KEY POINTS

✅ **Already Set Up**: Dependency added, test created  
✅ **Ready to Run**: Just execute `mvn test`  
✅ **3 Critical Rules**: Enforced automatically  
✅ **0 Configuration**: Works out of the box  
✅ **Fast Feedback**: ~0.5 second execution  

---

## 🎉 You're Done!

Your hexagonal architecture is now:
- ✅ Automatically enforced
- ✅ Protected from violations
- ✅ Testable in CI/CD
- ✅ Production-ready

**Go build clean architecture!** 🚀

