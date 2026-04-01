# Banking System: Unified Architecture & Gateway

A comprehensive banking system built with a **DDD/Hexagonal Architecture** backend (Java/Spring Boot) and a high-performance **API Gateway** (Python/FastAPI).

## 🚀 Quick Start (Production Mode)

The entire system is orchestrated with Docker Compose, providing a complete environment including MySQL, the Backend, and the Gateway.

### Prerequisites
- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Deployment (Production)
Run the following command in the project root to build and start all services:
```powershell
docker-compose up -d --build
```
This will start:
- **MySQL Database**: `localhost:3306`
- **Spring Boot Backend**: `localhost:8080`
- **FastAPI Gateway**: `localhost:8000`

Verify the setup by checking the health of the services:
```powershell
docker-compose ps
```

---

## 🧪 Quick Testing Guide (API & Gateway)

Once the system is running, you can test the primary functions using `curl` or any API client (like Postman).

### 1. Authentication (Login)
Retrieve a JWT token using the default credentials (`testuser` / `password`).
```powershell
# Using PowerShell (Invoke-RestMethod)
$response = Invoke-RestMethod -Uri "http://localhost:8000/login" -Method Post -ContentType "application/json" -Body '{"username":"testuser", "password":"password"}'
$token = $response.token
echo "Your Token: $token"
```
```bash
# Using curl
curl -X POST http://localhost:8000/login -H "Content-Type: application/json" -d '{"username":"testuser", "password":"password"}'
```

### 2. Get Account Details
Check the balance of account #1 (Default seeded account with initial balance).
```powershell
# Using PowerShell
Invoke-RestMethod -Uri "http://localhost:8000/api/accounts/1" -Method Get -Headers @{"Authorization" = "Bearer $token"}
```
```bash
# Using curl
curl -X GET http://localhost:8000/api/accounts/1 -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### 3. Withdraw Funds
Perform a withdrawal operation through the gateway. Account #1 is pre-seeded with funds for immediate testing.
```powershell
# Using PowerShell
Invoke-RestMethod -Uri "http://localhost:8000/api/accounts/1/withdraw" -Method Post -Headers @{"Authorization" = "Bearer $token"} -ContentType "application/json" -Body '{"amount": 50.0}'
```
```bash
# Using curl
curl -X POST http://localhost:8000/api/accounts/1/withdraw -H "Authorization: Bearer YOUR_TOKEN_HERE" -H "Content-Type: application/json" -d '{"amount": 50.0}'
```

---

## 🏗️ Project Structure

- `backend-spring/`: Java Spring Boot application (Domain logic, persistence, and core APIs).
- `gateway-fastapi/`: Python FastAPI application (Public API gateway, routing, and authentication).
- `docker-compose.yml`: Root orchestration for all components.

---

## 🧪 Testing the Whole System

Each component can be tested independently. Follow the links below for detailed instructions:

1.  **Java Backend (Spring Boot)**: [backend-spring/README.md](./backend-spring/README.md)
2.  **Python Gateway (FastAPI)**: [gateway-fastapi/README.md](./gateway-fastapi/README.md)

### Integration Sanity Check
Once the system is running via `docker-compose`, you can test the full flow:
1.  **Health Check**: `GET http://localhost:8000/` (Should return gateway status).
2.  **Auth Check**: `POST http://localhost:8000/api/auth/login` (Expect 200/401 based on credentials).

---

## 🛠️ Development & Manual Installation

If you prefer to run the components manually without Docker, please refer to the individual project README files for environment setup, installation, and testing:

- [Java Backend Setup Guide](./backend-spring/README.md)
- [Python Gateway Setup Guide](./gateway-fastapi/README.md)

---

## 🧹 Cleanup
To stop and remove all containers and volumes:
```powershell
docker-compose down -v
```
