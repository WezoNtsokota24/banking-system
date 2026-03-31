# TestContainers Integration Tests - Quick Start Guide

## 📋 Prerequisites Checklist

- [ ] Docker installed and running
- [ ] Maven installed (`mvn --version` works)
- [ ] Java 8+ installed
- [ ] Git (to clone if needed)

**Verify Docker is running:**
```bash
docker --version    # Should show version
docker ps           # Should show running containers (or empty list)
```

If Docker is not running, start Docker Desktop (Windows/Mac) or Docker daemon (Linux).

---

## 🚀 Running the Tests

### Option 1: Run All TestContainers Tests (Recommended)
```bash
cd backend-spring
mvn clean test -Dtest=WithdrawalControllerTestContainersIT
```

**Expected output:**
```
[INFO] Running com.banking.adapter.in.web.WithdrawalControllerTestContainersIT
[INFO] Tests run: 13, Failures: 0, Skipped: 0, Errors: 0, Time elapsed: 45.123 s
[INFO] BUILD SUCCESS
```

### Option 2: Run a Single Test
```bash
mvn test -Dtest=WithdrawalControllerTestContainersIT#testSuccessfulWithdrawal_shouldReduceBalance
```

### Option 3: Run All Tests (Both H2 and TestContainers)
```bash
mvn clean test
```

### Option 4: Run from IDE
1. **IntelliJ IDEA**:
   - Open `WithdrawalControllerTestContainersIT.java`
   - Right-click on class → "Run 'WithdrawalControllerTestContainersIT'"
   - Or right-click on test method → "Run 'testName()'"

2. **Eclipse/VSCode**:
   - Right-click test class → "Run as" → "JUnit Test"

---

## 📊 Test Execution Timeline

### First Run (with Docker image download)
```
[00:00] Maven clean
[00:05] Downloading MySQL 8.0 image (~400MB)
[00:30] Starting MySQL container
[00:35] Schema creation & Spring startup
[00:35] Test 1: testSuccessfulWithdrawal_shouldReduceBalance
[00:38] Test 2: testSuccessfulWithdrawal_withExactCalculation
... (more tests)
[00:50] All 13 tests complete
[01:00] Container cleanup & Docker stop
```

**Total: ~60 seconds**

### Subsequent Runs (image cached)
```
[00:00] Maven clean
[00:05] Starting MySQL container (cached image)
[00:10] Schema creation & Spring startup
[00:10] Test 1-13 execute
[00:20] Container cleanup
```

**Total: ~15-20 seconds**

---

## ✅ What Gets Tested

### Scenario 1: Successful Withdrawal (4 tests)
- ✅ Basic withdrawal reduces balance
- ✅ Decimal arithmetic is accurate
- ✅ Can withdraw entire balance
- ✅ Multiple withdrawals accumulate correctly

### Scenario 2: Insufficient Funds (3 tests)
- ✅ Returns HTTP 402 Payment Required
- ✅ Error message includes account ID, requested, available amounts
- ✅ Balance unchanged on failure

### Scenario 3: Inactive Account (3 tests)
- ✅ Returns HTTP 403 Forbidden
- ✅ Error message indicates account is inactive
- ✅ Cannot withdraw even with sufficient balance

### Other Tests (3 tests)
- ✅ GET account balance works
- ✅ Non-existent account returns error
- ✅ TestContainers connectivity verified

---

## 🔍 Verifying Test Success

### Expected Output:
```bash
$ mvn clean test -Dtest=WithdrawalControllerTestContainersIT

[INFO] Running com.banking.adapter.in.web.WithdrawalControllerTestContainersIT
[INFO] Tests run: 13, Failures: 0, Skipped: 0, Errors: 0
[INFO] BUILD SUCCESS
```

### Look For:
- ✅ "BUILD SUCCESS"
- ✅ All 13 tests run
- ✅ 0 failures
- ✅ 0 errors

---

## 🐛 Troubleshooting

### Problem: Docker not found
```
Error: Cannot connect to Docker daemon at unix:///var/run/docker.sock
```

**Solution:**
1. Start Docker Desktop (Windows/Mac)
2. Or start Docker daemon (Linux): `sudo systemctl start docker`
3. Verify: `docker ps`

---

### Problem: Port already in use
```
Error: Address already in use
```

**Solution:**
- Stop any running MySQL: `docker stop $(docker ps -q)`
- TestContainers uses random ports, this shouldn't happen normally

---

### Problem: Image pull error
```
Error: Error pulling image "mysql:8.0": Error response from daemon
```

**Solution:**
- Ensure internet connection
- Verify Docker Hub access
- Try: `docker pull mysql:8.0`

---

### Problem: Test timeout
```
org.testcontainers.containers.ContainerLaunchException: 
Timeout waiting for log output
```

**Solution:**
- Increase timeout in test configuration
- Or: `@Testcontainers(disabledWithoutDocker = true)` to skip in CI/CD

---

### Problem: Tests pass locally but fail in CI/CD
```
Container not found in CI/CD pipeline
```

**Solution:**
- Ensure Docker is available in CI/CD environment
- For GitHub Actions: use `ubuntu-latest` runner (has Docker pre-installed)
- Example workflow:
  ```yaml
  runs-on: ubuntu-latest
  steps:
    - uses: actions/checkout@v3
    - uses: actions/setup-java@v3
      with:
        java-version: '11'
    - run: mvn clean test -Dtest=WithdrawalControllerTestContainersIT
  ```

---

## 📚 Test File Structure

```
backend-spring/
├── src/
│   ├── main/
│   │   └── java/com/banking/
│   │       ├── adapter/
│   │       │   ├── in/
│   │       │   │   ├── web/
│   │       │   │   │   ├── WithdrawalController.java
│   │       │   │   │   └── WithdrawalRequest.java
│   │       │   │   └── exception/
│   │       │   │       └── GlobalExceptionHandler.java
│   │       │   └── out/
│   │       │       └── persistence/
│   │       │           └── [Persistence adapters]
│   │       └── domain/
│   │           ├── model/
│   │           │   ├── Account.java
│   │           │   └── AccountStatus.java
│   │           ├── port/
│   │           │   └── AccountRepository.java
│   │           ├── service/
│   │           │   └── WithdrawalService.java
│   │           └── exception/
│   │               ├── InsufficientFundsException.java
│   │               ├── InactiveAccountException.java
│   │               └── [Other exceptions]
│   └── test/
│       └── java/com/banking/
│           ├── adapter/in/web/
│           │   ├── WithdrawalControllerIntegrationTest.java     (H2 version)
│           │   └── WithdrawalControllerTestContainersIT.java     (TestContainers version)
│           └── TestcontainersConfiguration.java
└── pom.xml  (with TestContainers dependencies)
```

---

## 🔄 Test Execution Flow

```
1. Test starts
   ↓
2. @Testcontainers annotation detected
   ↓
3. MySQL container started (via Docker)
   ↓
4. Spring Boot context initialized
   ↓
5. Datasource auto-configured (via @ServiceConnection)
   ↓
6. JPA schema created (Hibernate ddl-auto)
   ↓
7. @BeforeEach setUp() creates test data
   ↓
8. Test method executes
   ├─ Send HTTP request
   ├─ Verify response
   └─ Validate database state
   ↓
9. @Transactional rollback
   ↓
10. Test data removed from database
    ↓
11. Next test... or
    ↓
12. All tests complete
    ↓
13. Container stopped & cleaned up
    ↓
14. Test results reported
```

---

## 📈 Performance Tips

### For Local Development:
```bash
# Run only the tests you're working on
mvn test -Dtest=WithdrawalControllerTestContainersIT#testSuccessfulWithdrawal_shouldReduceBalance
```

### For CI/CD Pipeline:
```bash
# Skip Docker tests if Docker unavailable
mvn test -Dtest=AccountTest  # Run fast unit tests
# Then in another job:
mvn test -Dtest=WithdrawalControllerTestContainersIT  # Run integration tests
```

### To Run All Tests Together:
```bash
mvn clean test  # Runs H2 tests + TestContainers tests + unit tests
```

---

## 🎯 What to Show Your Senior

1. **Test class** shows:
   - TestContainers setup with `@Container` annotation
   - MySQL container initialization
   - Proper test isolation with `@Transactional`
   - 13 comprehensive test cases
   - Clear test names with `@DisplayName`

2. **Test scenarios** cover:
   - ✅ Happy path (successful withdrawal)
   - ✅ Error cases (insufficient funds)
   - ✅ Business rule validation (inactive account)
   - ✅ Edge cases (exact balance, multiple ops)
   - ✅ Error responses (HTTP status, message format)

3. **Integration validation**:
   - HTTP → Controller → Service → Domain → Database flow
   - Exception handling (domain → HTTP mapping)
   - Database persistence verified
   - Response structure validated

4. **Professional practices**:
   - Production-realistic testing (real MySQL)
   - Test isolation (fresh data per test)
   - Proper assertions (HTTP + DB state)
   - Clear documentation and naming
   - Error case coverage

---

## 📚 Additional Resources

### TestContainers Documentation:
- https://www.testcontainers.org/
- https://www.testcontainers.org/modules/databases/mysql/

### Spring Boot + TestContainers:
- https://spring.io/blog/2023/06/23/improved-testcontainers-support-in-spring-boot-3-1

### Example GitHub Actions CI/CD:
```yaml
name: Integration Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '11'
      - run: mvn clean test -Dtest=WithdrawalControllerTestContainersIT
```

---

## ✨ Summary

Your integration tests with TestContainers demonstrate:

1. **Professional Testing** - Using production-realistic database
2. **Comprehensive Coverage** - All scenarios and edge cases
3. **Proper Architecture** - Full hexagonal flow validation
4. **Code Quality** - Clean, well-documented test code
5. **DevOps Ready** - CI/CD compatible, Docker native

This is **enterprise-grade** test coverage that will impress your senior review.

---

## Need Help?

If tests fail:
1. Check Docker is running: `docker ps`
2. Check error message in test output
3. See "Troubleshooting" section above
4. Verify pom.xml has TestContainers dependencies
5. Check MySQL port is not in use

If all else fails, fall back to H2 tests:
```bash
mvn test -Dtest=WithdrawalControllerIntegrationTest
```

