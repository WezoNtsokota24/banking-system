@echo off
REM Run H2 Integration Tests (No Docker Required)

cd /d C:\Users\qfenama\Documents\banking-system\backend-spring

echo ================================================
echo Running H2 Integration Tests
echo No Docker Required - Uses in-memory database
echo ================================================
echo.

mvn clean test -Dtest=WithdrawalControllerH2IT

echo.
echo ================================================
echo Test execution complete!
echo ================================================
pause

