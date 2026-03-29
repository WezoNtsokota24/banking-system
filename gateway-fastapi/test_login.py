import httpx
import asyncio
import json

async def test_login():
    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(
                "http://localhost:8000/login",
                json={"username": "testuser", "password": "password"},
                timeout=5.0
            )
            print(f"Status: {response.status_code}")
            print(f"Response: {json.dumps(response.json(), indent=2)}")
        except Exception as e:
            print(f"Error: {str(e)}")

asyncio.run(test_login())

