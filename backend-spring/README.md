# Java Backend (Spring Boot) - DDD / Hexagonal Architecture

This project is the core banking engine, built with a **Rich Domain Model** and **Hexagonal (Ports and Adapters) Architecture**. It provides domain logic, persistence, and REST APIs for the gateway.

## 🚀 How to Run (Development)

### Prerequisites
- [Java JDK 17+](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/download.cgi)
- [MySQL 8.0](https://www.mysql.com/downloads/) (Running on `localhost:3306`)

### Manual Start
1.  **Configure DB**: Ensure `src/main/resources/application.properties` points to your MySQL instance.
2.  **Build and Run**:
    ```powershell
    mvn spring-boot:run
    ```
The server will start on `http://localhost:8080`.

---

## 🧪 How to Test

This project includes unit, integration, and **ArchUnit** tests to enforce Hexagonal Architecture rules.

### Run All Tests
To execute all tests (including ArchUnit):
```powershell
mvn test
```

### Run Specific Integration Tests
To verify the withdrawal functionality specifically:
```powershell
mvn test -Dtest=WithdrawalControllerH2IT
```

### Key Test Categories
1.  **Unit Tests**: Core domain logic (Withdrawal, Account).
2.  **Integration Tests**: Port-Adapter interactions (Persistence, Controllers).
3.  **ArchUnit Tests**: Architectural validation to ensure the Domain remains pure and independent of the framework (`com.banking.architecture.HexagonalArchitectureTest`).

---

## 🏗️ Architecture: Hexagonal Layers

The project follows Domain-Driven Design (DDD) principles with a Hexagonal architecture:

-   **Domain (`com.banking.domain`)**: Contains the Rich Domain Model (Entities, Value Objects, Domain Services) and **Ports** (Interfaces). **Zero dependencies on Spring**.
-   **Adapters (`com.banking.adapter`)**: Implements Ports.
    -   `in.web`: REST Controllers (e.g., `WithdrawalController`).
    -   `out.persistence`: Repository implementations (JPA, `TransactionPersistenceAdapter`).
-   **Configuration (`com.banking.adapter.config`)**: Spring configuration to wire Domain Services as beans.

### Withdrawal Flow Detail
1.  **Web Adapter**: `WithdrawalController` receives the POST request.
2.  **Domain Service**: `WithdrawalService` handles the business logic (validating account state, checking funds).
3.  **Domain Model**: `Account.withdraw()` updates the internal state (rich model).
4.  **Persistence Adapter**: `TransactionPersistenceAdapter` saves the transaction record as `PENDING`.
5.  **Output**: Returns 200 OK or appropriate domain error (e.g., 402 for insufficient funds).

---

## 🌍 Installation in Production

### Using Docker (Recommended)
Build and run the containerized backend:
```powershell
docker build -t banking-backend:latest .
docker run -p 8080:8080 banking-backend:latest
```
*Note: Ensure the `SPRING_DATASOURCE_URL` environment variable is set to point to your production database.*

### Using JAR File
1.  **Package**:
    ```powershell
    mvn clean package -DskipTests
    ```
2.  **Run**:
    ```powershell
    java -jar target/backend-spring-1.0-SNAPSHOT.jar
    ```

---

## 🧹 Cleanup
To clean the build artifacts:
```powershell
mvn clean
```
