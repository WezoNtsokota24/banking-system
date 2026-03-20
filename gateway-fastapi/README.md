# Banking Gateway API

A FastAPI-based reverse proxy gateway for the Spring Boot banking backend.

## Features

- **Reverse Proxy**: Forwards requests to the Spring Boot backend
- **Async HTTP**: Uses httpx for efficient async HTTP client
- **Error Handling**: Proper error responses when backend is unavailable
- **Health Check**: Basic health endpoint for monitoring

## Endpoints

### Health Check
- **GET** `/health`
- Returns service status and environment

### Account Retrieval
- **GET** `/api/accounts/{account_id}`
- Forwards to Spring backend and returns account JSON

### Account Withdrawal
- **POST** `/api/accounts/{account_id}/withdraw`
- Accepts JSON: `{"amount": 50.00}`
- Forwards to Spring backend

## Configuration

Environment variables (via `.env` file):
- `SPRING_BACKEND_URL`: URL of the Spring Boot backend (default: http://localhost:8080)
- `APP_NAME`: Application name (default: Banking Gateway API)
- `ENVIRONMENT`: Environment name (default: development)

## Installation

1. Install dependencies:
```bash
pip install -r requirements.txt
```

## Running the Application

### Development Mode
```bash
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

### Production Mode
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
```

## Testing

### Health Check
```bash
curl http://localhost:8000/health
```

### Get Account
```bash
curl http://localhost:8000/api/accounts/1
```

### Withdraw Money
```bash
curl -X POST http://localhost:8000/api/accounts/1/withdraw \
  -H "Content-Type: application/json" \
  -d '{"amount": 50.00}'
```

## Error Handling

- **503 Service Unavailable**: When Spring backend is down or unreachable
- **4xx/5xx**: Forwarded from Spring backend with appropriate status codes

## Architecture

This gateway acts as a reverse proxy, providing:
- Request routing to the Spring backend
- Unified API interface
- Potential for future features like authentication, rate limiting, logging
- Isolation between external clients and internal services</content>
<parameter name="filePath">C:\Users\qfenama\Documents\banking-system\gateway-fastapi\README.md
