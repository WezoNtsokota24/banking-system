from fastapi import FastAPI, HTTPException
from config import settings
import httpx
from typing import Optional, Any
from pydantic import BaseModel
import logging

# Set up logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.app_name)

class WithdrawRequest(BaseModel):
    amount: float

@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {"status": "up", "environment": settings.environment}

@app.get("/api/accounts/{account_id}")
async def get_account(account_id: int):
    """
    Forward GET request to Spring backend for account retrieval.
    Returns the account details from the backend.
    """
    async with httpx.AsyncClient() as client:
        try:
            url = f"{settings.spring_backend_url}/api/accounts/{account_id}"
            logger.info(f"Forwarding GET request to: {url}")
            
            response = await client.get(url, timeout=10.0)
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
async def withdraw_from_account(account_id: int, request: WithdrawRequest):
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
            headers = {"Content-Type": "application/json"}
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

