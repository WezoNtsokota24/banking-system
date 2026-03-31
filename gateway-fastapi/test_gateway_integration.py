import pytest
from httpx import AsyncClient, ASGITransport
from main import app
from config import settings
import respx
import logging

@pytest.mark.asyncio
class TestFastAPIGatewayIntegration:
    """Integration tests for FastAPI gateway with mocked Spring Boot backend."""

    @pytest.fixture
    async def async_client(self):
        """Async client fixture for testing FastAPI app."""
        async with AsyncClient(
            transport=ASGITransport(app=app), base_url="http://testserver"
        ) as client:
            yield client

    @pytest.fixture
    def mock_spring_backend(self):
        """Fixture to mock Spring Boot backend responses."""
        with respx.mock:
            yield respx

    async def test_login_flow_success(self, async_client, mock_spring_backend):
        """Test login flow: POST to /login, mock successful JWT response from Spring Boot."""
        # Arrange
        username = "testuser"
        password = "testpass"
        mock_jwt_token = {"token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.mocktoken"}

        # Mock Spring Boot backend response
        mock_spring_backend.post(f"{settings.spring_backend_url}/api/auth/login").respond(
            status_code=200,
            json=mock_jwt_token
        )

        # Act: Send POST request to FastAPI /login
        response = await async_client.post(
            "/login",
            json={"username": username, "password": password}
        )

        # Assert: Verify FastAPI returns the token to the client
        assert response.status_code == 200
        assert response.json() == mock_jwt_token

    async def test_authentication_forwarding_balance(self, async_client, mock_spring_backend):
        """Test authentication forwarding: GET to /api/accounts/{account_id} with Bearer token."""
        # Arrange
        account_id = 123
        bearer_token = "test-jwt-token"
        mock_account_data = {
            "id": account_id,
            "accountNumber": "ACC123",
            "balance": 1000.00,
            "status": "ACTIVE"
        }

        # Mock Spring Boot backend response
        mock_spring_backend.get(f"{settings.spring_backend_url}/api/accounts/{account_id}").respond(
            status_code=200,
            json=mock_account_data
        )

        # Act: Send GET request to /api/accounts/{account_id} with Bearer token
        response = await async_client.get(
            f"/api/accounts/{account_id}",
            headers={"Authorization": f"Bearer {bearer_token}"}
        )

        # Assert: Verify request was successful and data returned
        assert response.status_code == 200
        assert response.json() == mock_account_data

        # Verify that the Authorization header was forwarded to Spring Boot
        # (respx captures the request, so we can inspect it)
        request = mock_spring_backend.calls.last.request
        assert request.headers["Authorization"] == f"Bearer {bearer_token}"

    async def test_error_handling_insufficient_funds(self, async_client, mock_spring_backend, caplog):
        """Test error handling: POST to /withdraw with amount > balance, mock 400 from Spring Boot."""
        # Arrange
        account_id = 123
        bearer_token = "test-jwt-token"
        withdraw_amount = 1500.00  # Amount greater than balance
        mock_error_response = {
            "status": 402,
            "message": "Insufficient funds for account 123. Requested: 1500, Available: 1000"
        }

        # Mock Spring Boot backend returning 400 Bad Request with insufficient funds
        mock_spring_backend.post(f"{settings.spring_backend_url}/api/accounts/{account_id}/withdraw").respond(
            status_code=402,
            json=mock_error_response
        )

        # Act: Send POST request to /api/accounts/{account_id}/withdraw
        with caplog.at_level(logging.ERROR):
            response = await async_client.post(
                f"/api/accounts/{account_id}/withdraw",
                json={"amount": withdraw_amount},
                headers={"Authorization": f"Bearer {bearer_token}"}
            )

        # Assert: Verify FastAPI catches the external error and returns appropriate status/message
        assert response.status_code == 402
        assert "Backend service error" in response.json()["detail"]
        assert "Insufficient funds" in response.json()["detail"]

        # Assert: Verify error was logged correctly
        assert any("Backend error: 402" in record.message for record in caplog.records)
        assert any("Insufficient funds" in record.message for record in caplog.records)
