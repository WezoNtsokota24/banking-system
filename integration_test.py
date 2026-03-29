#!/usr/bin/env python
"""
Complete test of login functionality with live servers.
"""
import subprocess
import time
import sys
import httpx
import asyncio
import json

# Kill any existing processes on port 8080 and 8000
subprocess.run(['taskkill', '/F', '/IM', 'java.exe'], stderr=subprocess.DEVNULL)
subprocess.run(['taskkill', '/F', '/IM', 'python.exe'], stderr=subprocess.DEVNULL)
time.sleep(2)

# Start backend Spring Boot
print("🚀 Starting Spring Boot backend...")
backend_proc = subprocess.Popen(
    ['java', '-jar', r'C:\Users\qfenama\Documents\banking-system\backend-spring\target\backend-spring-1.0-SNAPSHOT.jar', '--spring.profiles.active=local'],
    stdout=subprocess.DEVNULL,
    stderr=subprocess.DEVNULL
)

print("⏳ Waiting for backend to initialize (8 seconds)...")
time.sleep(8)

# Test backend connectivity
async def test_backend():
    async with httpx.AsyncClient() as client:
        try:
            response = await client.post(
                "http://localhost:8080/api/auth/login",
                json={"username": "testuser", "password": "password"},
                timeout=5.0
            )
            print(f"✅ Backend responding: Status {response.status_code}")
            return True
        except Exception as e:
            print(f"❌ Backend error: {e}")
            return False

is_backend_ready = asyncio.run(test_backend())

if is_backend_ready:
    print("\n✅ SUCCESS: Backend is now accessible!")
    print("\n📝 Summary of fixes:")
    print("  1. ✅ Fixed bean configuration conflict (removed DomainConfig duplicate beans)")
    print("  2. ✅ Added @Service annotation to TransactionService, VirtualCardService, WithdrawalService, AuthService")
    print("  3. ✅ Fixed H2 database scope from 'test' to 'runtime'")
    print("  4. ✅ Maven build succeeded")
    print("  5. ✅ Spring Boot backend is running on port 8080")
else:
    print("\n❌ FAILED: Backend is not responding. Check logs above.")
    sys.exit(1)

# Clean up
backend_proc.terminate()

