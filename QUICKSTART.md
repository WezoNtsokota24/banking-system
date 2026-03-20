# Quick Start Guide - Banking System

## 🚀 Running with Docker Compose (Recommended)

### Prerequisites
- Docker Desktop installed
- From the project root directory: `C:\Users\qfenama\Documents\banking-system`

### Start the System

```powershell
# 1. Navigate to project directory
cd C:\Users\qfenama\Documents\banking-system

# 2. Start all services
docker-compose up -d

# 3. Wait for services to be healthy (about 30 seconds)
docker-compose ps

# 4. Check logs to verify startup
docker-compose logs spring-backend
docker-compose logs fastapi-gateway
```

### Verify Services are Running

```bash
# Check health
curl http://localhost:8000/health

# Expected response:
# {"status": "up", "environment": "docker"}
```

### Test the Endpoints

```bash
# 1. Get account info
curl http://localhost:8000/api/accounts/1

# Expected response:
# {"id": 1, "accountNumber": "999888777", "balance": 500.00}

# 2. Withdraw money
curl -X POST http://localhost:8000/api/accounts/1/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.0}'

# Expected response:
# {"message": "Withdrawal successful", "account_id": 1, "amount": 50.0}

# 3. Check balance after withdrawal
curl http://localhost:8000/api/accounts/1

# Expected response:
# {"id": 1, "accountNumber": "999888777", "balance": 450.00}
```

### View Logs

```bash
# Spring Boot backend logs
docker-compose logs -f spring-backend

# FastAPI gateway logs
docker-compose logs -f fastapi-gateway

# MySQL database logs
docker-compose logs -f mysql-db
```

### Stop the System

```bash
# Stop all services but keep data
docker-compose stop

# Stop and remove everything (including data)
docker-compose down

# Stop and remove everything including volumes
docker-compose down -v
```

---

## 🛠️ Running Locally (Without Docker)

### Prerequisites
- Java 17+
- Python 3.11+
- MySQL 8.0+
- Maven 3.9+

### 1. Start MySQL

```powershell
# Option A: Docker container (easiest)
docker run -d -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=rootpassword `
  -e MYSQL_DATABASE=banking_db `
  -e MYSQL_USER=banking_user `
  -e MYSQL_PASSWORD=banking_password `
  mysql:8.0

# Option B: Local installation
# Download from https://dev.mysql.com/downloads/mysql/
# Create database and user as above
```

### 2. Run Spring Boot Backend

```powershell
# Terminal 1: Spring Backend
cd backend-spring
mvn clean install
mvn spring-boot:run

# Expected startup message:
# Started BankingApplication in 3.2 seconds
# Test account created with ID: 1 and Balance: $500.00
```

### 3. Run FastAPI Gateway

```powershell
# Terminal 2: FastAPI Gateway
cd gateway-fastapi
pip install -r requirements.txt
uvicorn main:app --reload --port 8000

# Expected startup message:
# Uvicorn running on http://127.0.0.1:8000
# Application startup complete
```

### 4. Test the Endpoints

```bash
# Same curl commands as Docker section above
curl http://localhost:8000/health
curl http://localhost:8000/api/accounts/1
curl -X POST http://localhost:8000/api/accounts/1/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.0}'
```

---

## 📊 Service URLs

| Service | URL | Port |
|---------|-----|------|
| FastAPI Gateway | http://localhost:8000 | 8000 |
| Spring Boot Backend | http://localhost:8080 | 8080 |
| MySQL Database | localhost | 3306 |

---

## 🌙 Testing the Nightly Batch Process

The batch process runs automatically at **23:59 (11:59 PM)** every day.

### Manual Test (Optional)

To test without waiting, you can:

1. Add a transaction and mark it as PENDING
2. Watch the logs at 23:59
3. Verify transaction status changed to COMPLETED

### Check Application Logs

```bash
# Spring Boot will print batch report:
# === NIGHTLY TRANSACTION BATCH REPORT ===
# Processing 2 pending transactions:
# Transaction ID: 1, Account: 1, Type: WITHDRAWAL, Amount: 50.00
# Transaction ID: 2, Account: 1, Type: WITHDRAWAL, Amount: 25.00
# All pending transactions have been processed and marked as COMPLETED.
# =====================================
```

---

## 📁 Project Structure

```
banking-system/
├── backend-spring/
│   ├── src/main/java/com/banking/
│   │   ├── domain/          # Pure business logic
│   │   ├── adapter/         # Spring integration layer
│   │   └── config/          # Dependency injection
│   ├── pom.xml              # Maven configuration
│   └── Dockerfile           # Container image
│
├── gateway-fastapi/
│   ├── main.py              # FastAPI application
│   ├── config.py            # Configuration
│   ├── requirements.txt      # Python dependencies
│   └── Dockerfile           # Container image
│
├── docker-compose.yml       # Service orchestration
├── ARCHITECTURE.md          # Architecture overview
├── CHANGES.md              # Detailed changes
├── DDD_HEXAGONAL_GUIDE.md  # Architecture deep dive
├── SENIOR_REVIEW.md        # For code review
└── QUICKSTART.md           # This file
```

---

## 🔧 Configuration

### Spring Boot Configuration

Default environment variables (Docker):
```
SPRING_DATASOURCE_URL=jdbc:mysql://mysql-db:3306/banking_db
SPRING_DATASOURCE_USERNAME=banking_user
SPRING_DATASOURCE_PASSWORD=banking_password
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

### FastAPI Configuration

Default environment variables (Docker):
```
SPRING_BACKEND_URL=http://spring-backend:8080
ENVIRONMENT=docker
```

### Modify Ports

Edit `docker-compose.yml` to change port mappings:

```yaml
services:
  spring-backend:
    ports:
      - "8888:8080"  # External:Internal (change 8888 to desired port)
      
  fastapi-gateway:
    ports:
      - "8888:8000"  # External:Internal (change 8888 to desired port)
```

---

## 🐛 Troubleshooting

### "Connection refused" when accessing gateway

```bash
# Check if services are running
docker-compose ps

# Check if gateway is healthy
docker-compose logs fastapi-gateway

# Restart services
docker-compose restart
```

### "MySQL connection failed"

```bash
# Check MySQL is healthy
docker-compose ps mysql-db

# Check MySQL logs
docker-compose logs mysql-db

# Restart MySQL
docker-compose restart mysql-db
```

### Port already in use

```bash
# Find process using port 8000 (Windows)
Get-Process -Id (Get-NetTCPConnection -LocalPort 8000 -ErrorAction SilentlyContinue).OwningProcess

# Kill process
Stop-Process -Id <PID> -Force

# Or change port in docker-compose.yml
```

### "Module not found" for Python

```bash
# Reinstall dependencies
pip install -r gateway-fastapi/requirements.txt

# Or in Docker, rebuild image
docker-compose build --no-cache fastapi-gateway
```

---

## 📝 Sample API Calls

### Create Initial Data

```powershell
# Get initial account (created automatically on startup)
$response = curl -s http://localhost:8000/api/accounts/1
Write-Output $response

# Response:
# {"id":1,"accountNumber":"999888777","balance":500.0}
```

### Withdrawal Sequence

```powershell
# Check initial balance
curl http://localhost:8000/api/accounts/1
# {"id":1,"accountNumber":"999888777","balance":500.0}

# Withdraw $50
curl -X POST http://localhost:8000/api/accounts/1/withdraw `
  -H "Content-Type: application/json" `
  -d '{"amount": 50.0}'
# {"message":"Withdrawal successful","account_id":1,"amount":50.0}

# Check balance again
curl http://localhost:8000/api/accounts/1
# {"id":1,"accountNumber":"999888777","balance":450.0}

# Try insufficient funds (will fail)
curl -X POST http://localhost:8000/api/accounts/1/withdraw `
  -H "Content-Type: application/json" `
  -d '{"amount": 500.0}'
# Status 400: {"detail":"Insufficient funds"}
```

---

## 📚 Documentation

- **ARCHITECTURE.md** - High-level architecture overview
- **DDD_HEXAGONAL_GUIDE.md** - Detailed architecture guide (for senior review)
- **SENIOR_REVIEW.md** - Code review reference
- **CHANGES.md** - Complete list of all changes made
- **QUICKSTART.md** - This file

---

## ✅ Verification Checklist

After starting the system, verify:

- [ ] `docker-compose ps` shows all services as "Up"
- [ ] `curl http://localhost:8000/health` returns status "up"
- [ ] `curl http://localhost:8000/api/accounts/1` returns account data
- [ ] Withdrawal endpoint accepts requests and returns success
- [ ] Spring logs show test account created
- [ ] FastAPI logs show forwarding of requests
- [ ] No errors in MySQL logs

---

## 🎯 Next Steps

1. **Read Architecture**: Start with `ARCHITECTURE.md`
2. **Understand DDD**: Read `DDD_HEXAGONAL_GUIDE.md`
3. **Review Changes**: Check `CHANGES.md` for what was updated
4. **Test Endpoints**: Use the sample curl commands above
5. **Prepare for Meeting**: Review `SENIOR_REVIEW.md`

---

## 📞 Need Help?

Check the Docker logs for detailed error messages:

```bash
# All services
docker-compose logs

# Specific service
docker-compose logs spring-backend
docker-compose logs fastapi-gateway
docker-compose logs mysql-db
```

---

**Version**: 1.0  
**Date**: March 20, 2026  
**Status**: Ready for Production

