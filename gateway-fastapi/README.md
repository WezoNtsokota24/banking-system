# Python API Gateway (FastAPI)

High-performance API Gateway for the Banking System. It handles public traffic, basic authentication routing, and orchestrates requests to the Java Spring Boot backend.

## 🚀 How to Run (Development)

### Prerequisites
- [Python 3.12+](https://www.python.org/downloads/)
- Virtual Environment (`venv`)

### Manual Start
1.  **Initialize Virtual Environment**:
    ```powershell
    python -m venv venv
    .\venv\Scripts\activate
    ```
2.  **Install Dependencies**:
    ```powershell
    pip install -r requirements.txt
    ```
3.  **Environment Setup**: Ensure `.env` points to the Spring Backend:
    `SPRING_BACKEND_URL=http://localhost:8080`
4.  **Run Server**:
    ```powershell
    uvicorn main:app --reload --port 8000
    ```
The gateway will start on `http://localhost:8000`.

---

## 🧪 How to Test

This project uses **Pytest** for unit and integration testing.

### Run All Tests
```powershell
.\venv\Scripts\python -m pytest
```

---

## 🏗️ Technical Stack

-   **FastAPI**: Modern, high-performance web framework for Python.
-   **Pydantic**: Data validation and settings management.
-   **Uvicorn**: Lightning-fast ASGI server implementation.
-   **Respx**: Utility for mocking HTTP requests in tests.

---

## 🌍 Installation in Production

### Using Docker (Recommended)
Build and run the containerized gateway:
```powershell
docker build -t banking-gateway:latest .
docker run -p 8000:8000 banking-gateway:latest
```
*Note: Ensure `SPRING_BACKEND_URL` points to your production backend.*

### Manual Production Setup
1.  **Install Production Requirements**:
    ```powershell
    pip install -r requirements.txt
    ```
2.  **Run with Production Server (Uvicorn)**:
    ```powershell
    uvicorn main:app --host 0.0.0.0 --port 8000 --workers 4
    ```

---

## 🧹 Cleanup
To clean the project environment:
```powershell
rm -r .pytest_cache
# (Optional) Remove venv if you want to start fresh
rm -r venv
```
