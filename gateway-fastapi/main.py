from fastapi import FastAPI, HTTPException, Depends
from config import settings
import httpx
from typing import Optional, Any
from pydantic import BaseModel
import logging
from fastapi.security import OAuth2PasswordBearer

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="login")

class WithdrawRequest(BaseModel):
    amount: float

class LoginRequest(BaseModel):
    username: str

    password: str

@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {"status": "up", "environment": settings.environment}

@app.post("/login")
async def login(request: LoginRequest):
    """
    Forward login request to Spring backend for authentication.
    Returns the JWT token from the backend.
    """
    async with httpx.AsyncClient() as client:
        try:
            # Use the configurable backend URL from settings/environment variables
            url = f"{settings.spring_backend_url}/api/auth/login"
            headers = {"Content-Type": "application/json"}
            payload = {"username": request.username, "password": request.password}
            
            logger.info(f"Forwarding login request to: {url}")
            
            response = await client.post(url, json=payload, headers=headers, timeout=10.0)
            response.raise_for_status()
            
            logger.info("Login successful")
            return response.json()
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Backend error: {e.response.status_code} - {e.response.text}")
            raise HTTPException(
                status_code=e.response.status_code, 
                detail=f"Authentication failed: {e.response.text[:200]}"
            )
        except httpx.ConnectError:
            logger.error(f"Connection error to Spring backend at {settings.spring_backend_url}")
            raise HTTPException(
                status_code=503, 
                detail="Authentication service unavailable (connection error)"
            )
        except httpx.TimeoutException:
            logger.error("Timeout during login request")
            raise HTTPException(
                status_code=504, 
                detail="Authentication service unavailable (timeout)"
            )
        except Exception as e:
            logger.error(f"Unexpected error during login: {str(e)}")
            raise HTTPException(
                status_code=500, 
                detail=f"Internal server error: {str(e)}"
            )

@app.get("/api/accounts/{account_id}")
async def get_account(account_id: int, token: str = Depends(oauth2_scheme)):
    """
    Forward GET request to Spring backend for account retrieval.
    Returns the account details from the backend.
    """
    async with httpx.AsyncClient() as client:
        try:
            url = f"{settings.spring_backend_url}/api/accounts/{account_id}"
            headers = {"Authorization": f"Bearer {token}"}
            logger.info(f"Forwarding GET request to: {url}")
            
            response = await client.get(url, headers=headers, timeout=10.0)
            response.raise_for_status()
            
            logger.info(f"Successfully retrieved account {account_id}")
            return response.json()
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Backend error: {e.response.status_code} - {e.response.text}")
            raise HTTPException(
                status_code=e.response.status_code, 
                detail=f"Backend service error: {e.response.text[:200]}"
            )
        except httpx.ConnectError:
            logger.error("Connection error to Spring backend")
            raise HTTPException(
                status_code=503, 
                detail="Spring backend service unavailable (connection error)"
            )
        except httpx.TimeoutException:
            logger.error("Timeout connecting to Spring backend")
            raise HTTPException(
                status_code=504, 
                detail="Spring backend service unavailable (timeout)"
            )
        except Exception as e:
            logger.error(f"Unexpected error: {str(e)}")
            raise HTTPException(
                status_code=500, 
                detail=f"Internal server error: {str(e)}"
            )

@app.post("/api/accounts/{account_id}/withdraw")
async def withdraw_from_account(account_id: int, request: WithdrawRequest, token: str = Depends(oauth2_scheme)):
    """
    Forward POST request to Spring backend for withdrawal.
    Accepts a JSON body with amount field and forwards it to the backend.
    Returns the response from the backend.
    """
    # Validate amount
    if request.amount <= 0:
        raise HTTPException(status_code=400, detail="Withdrawal amount must be greater than zero")
    
    async with httpx.AsyncClient() as client:
        try:
            url = f"{settings.spring_backend_url}/api/accounts/{account_id}/withdraw"
            headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
            payload = {"amount": request.amount}
            
            logger.info(f"Forwarding POST request to: {url} with amount: {request.amount}")
            
            response = await client.post(url, json=payload, headers=headers, timeout=10.0)
            response.raise_for_status()
            
            logger.info(f"Withdrawal successful for account {account_id}, amount: {request.amount}")
            return {"message": "Withdrawal successful", "account_id": account_id, "amount": request.amount}
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Backend error: {e.response.status_code} - {e.response.text}")
            raise HTTPException(
                status_code=e.response.status_code, 
                detail=f"Backend service error: {e.response.text[:200]}"
            )
        except httpx.ConnectError:
            logger.error("Connection error to Spring backend during withdrawal")
            raise HTTPException(
                status_code=503, 
                detail="Spring backend service unavailable (connection error)"
            )
        except httpx.TimeoutException:
            logger.error("Timeout during withdrawal request")
            raise HTTPException(
                status_code=504, 
                detail="Spring backend service unavailable (timeout)"
            )
        except Exception as e:
            logger.error(f"Unexpected error during withdrawal: {str(e)}")
            raise HTTPException(
                status_code=500, 
                detail=f"Internal server error: {str(e)}"
            )

@app.post("/api/accounts/{account_id}/cards")
async def generate_virtual_card(account_id: int, token: str = Depends(oauth2_scheme)):
    """
    Forward POST request to Spring backend for virtual card generation.
    Creates a new virtual card for the account.
    """
    async with httpx.AsyncClient() as client:
        try:
            url = f"{settings.spring_backend_url}/api/accounts/{account_id}/cards"
            headers = {"Authorization": f"Bearer {token}"}
            logger.info(f"Forwarding POST request to: {url} for card generation")
            
            response = await client.post(url, headers=headers, timeout=10.0)
            response.raise_for_status()
            
            logger.info(f"Virtual card generated successfully for account {account_id}")
            return response.json()
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Backend error: {e.response.status_code} - {e.response.text}")
            if e.response.status_code == 401:
                raise HTTPException(
                    status_code=401, 
                    detail="Unauthorized: Invalid or expired token"
                )
            elif e.response.status_code == 404:
                raise HTTPException(
                    status_code=404, 
                    detail="Account not found"
                )
            else:
                raise HTTPException(
                    status_code=e.response.status_code, 
                    detail=f"Backend service error: {e.response.text[:200]}"
                )
        except httpx.ConnectError:
            logger.error("Connection error to Spring backend during card generation")
            raise HTTPException(
                status_code=503, 
                detail="Spring backend service unavailable (connection error)"
            )
        except httpx.TimeoutException:
            logger.error("Timeout during card generation request")
            raise HTTPException(
                status_code=504, 
                detail="Spring backend service unavailable (timeout)"
            )
        except Exception as e:
            logger.error(f"Unexpected error during card generation: {str(e)}")
            raise HTTPException(
                status_code=500, 
                detail=f"Internal server error: {str(e)}"
            )

@app.get("/api/accounts/{account_id}/cards")
async def get_virtual_cards(account_id: int, token: str = Depends(oauth2_scheme)):
    """
    Forward GET request to Spring backend for virtual card retrieval.
    Returns the list of virtual cards for the account.
    """
    async with httpx.AsyncClient() as client:
        try:
            url = f"{settings.spring_backend_url}/api/accounts/{account_id}/cards"
            headers = {"Authorization": f"Bearer {token}"}
            logger.info(f"Forwarding GET request to: {url} for card retrieval")
            
            response = await client.get(url, headers=headers, timeout=10.0)
            response.raise_for_status()
            
            logger.info(f"Virtual cards retrieved successfully for account {account_id}")
            return response.json()
            
        except httpx.HTTPStatusError as e:
            logger.error(f"Backend error: {e.response.status_code} - {e.response.text}")
            if e.response.status_code == 401:
                raise HTTPException(
                    status_code=401, 
                    detail="Unauthorized: Invalid or expired token"
                )
            elif e.response.status_code == 404:
                raise HTTPException(
                    status_code=404, 
                    detail="Account not found or no cards available"
                )
            else:
                raise HTTPException(
                    status_code=e.response.status_code, 
                    detail=f"Backend service error: {e.response.text[:200]}"
                )
        except httpx.ConnectError:
            logger.error("Connection error to Spring backend during card retrieval")
            raise HTTPException(
                status_code=503, 
                detail="Spring backend service unavailable (connection error)"
            )
        except httpx.TimeoutException:
            logger.error("Timeout during card retrieval request")
            raise HTTPException(
                status_code=504, 
                detail="Spring backend service unavailable (timeout)"
            )
        except Exception as e:
            logger.error(f"Unexpected error during card retrieval: {str(e)}")
            raise HTTPException(
                status_code=500, 
                detail=f"Internal server error: {str(e)}"
            )
