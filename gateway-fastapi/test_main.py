import pytest
from fastapi.testclient import TestClient
from main import app
import respx
import httpx

client = TestClient(app)

def test_login_success():
    """Test successful POST /login"""
    username = "testuser"
    password = "testpass"
    mock_response_data = {"token": "jwt-token-here"}

    # Mock the backend response
    with respx.mock:
        respx.post("http://localhost:8080/api/auth/login").respond(
            status_code=200,
            json=mock_response_data
        )

        # Make request to our FastAPI app
        response = client.post(
            "/login",
            json={"username": username, "password": password}
        )

        # Assert
        assert response.status_code == 200
        assert response.json() == mock_response_data

def test_login_failure():
    """Test POST /login with invalid credentials"""
    username = "testuser"
    password = "wrongpass"

    with respx.mock:
        respx.post("http://localhost:8080/api/auth/login").respond(
            status_code=401,
            json={"error": "Invalid credentials"}
        )

        response = client.post(
            "/login",
            json={"username": username, "password": password}
        )

        # Assert
        assert response.status_code == 401
        assert "Authentication failed" in response.json()["detail"]

def test_get_account_success():
    """Test successful GET /api/accounts/{account_id}"""
    account_id = 123
    token = "dummy-jwt-token"
    mock_response_data = {
        "id": account_id,
        "accountNumber": "ACC123",
        "balance": 1000.00,
        "status": "ACTIVE"
    }

    # Mock the backend response
    with respx.mock:
        respx.get(f"http://localhost:8080/api/accounts/{account_id}").respond(
            status_code=200,
            json=mock_response_data
        )

        # Make request to our FastAPI app with Authorization header
        response = client.get(
            f"/api/accounts/{account_id}",
            headers={"Authorization": f"Bearer {token}"}
        )

        # Assert
        assert response.status_code == 200
        assert response.json() == mock_response_data

def test_withdraw_success():
    """Test successful POST /api/accounts/{account_id}/withdraw"""
    account_id = 123
    token = "dummy-jwt-token"
    withdraw_amount = 50.00

    # Mock the backend response (assuming it returns 200 on success)
    with respx.mock:
        respx.post(f"http://localhost:8080/api/accounts/{account_id}/withdraw").respond(
            status_code=200,
            json={"message": "Withdrawal processed"}
        )

        # Make request to our FastAPI app with Authorization header
        response = client.post(
            f"/api/accounts/{account_id}/withdraw",
            json={"amount": withdraw_amount},
            headers={"Authorization": f"Bearer {token}"}
        )

        # Assert
        assert response.status_code == 200
        assert response.json() == {
            "message": "Withdrawal successful",
            "account_id": account_id,
            "amount": withdraw_amount
        }

def test_withdraw_invalid_amount():
    """Test POST /api/accounts/{account_id}/withdraw with invalid amount"""
    account_id = 123
    token = "dummy-jwt-token"
    invalid_amount = -10.00

    # Make request with invalid amount (should fail before reaching backend)
    response = client.post(
        f"/api/accounts/{account_id}/withdraw",
        json={"amount": invalid_amount},
        headers={"Authorization": f"Bearer {token}"}
    )

    # Assert
    assert response.status_code == 400
    assert "Withdrawal amount must be greater than zero" in response.json()["detail"]

def test_get_account_backend_error():
    """Test GET /api/accounts/{account_id} when backend returns error"""
    account_id = 123
    token = "dummy-jwt-token"

    with respx.mock:
        respx.get(f"http://localhost:8080/api/accounts/{account_id}").respond(
            status_code=404,
            json={"error": "Account not found"}
        )

        response = client.get(
            f"/api/accounts/{account_id}",
            headers={"Authorization": f"Bearer {token}"}
        )

        # Assert that the error is forwarded
        assert response.status_code == 404
        assert "Backend service error" in response.json()["detail"]

def test_withdraw_backend_unavailable():
    """Test POST /api/accounts/{account_id}/withdraw when backend is down"""
    account_id = 123
    token = "dummy-jwt-token"
    withdraw_amount = 50.00

    with respx.mock:
        respx.post(f"http://localhost:8080/api/accounts/{account_id}/withdraw").mock(
            side_effect=httpx.ConnectError("Connection failed")
        )

        response = client.post(
            f"/api/accounts/{account_id}/withdraw",
            json={"amount": withdraw_amount},
            headers={"Authorization": f"Bearer {token}"}
        )

        # Assert
        assert response.status_code == 503
        assert "Spring backend service unavailable" in response.json()["detail"]

def test_get_account_no_token():
    """Test GET /api/accounts/{account_id} without token"""
    account_id = 123

    response = client.get(f"/api/accounts/{account_id}")

    # Should return 401 Unauthorized
    assert response.status_code == 401
