@echo off
echo Starting Group IM Server MCP Test...
echo =====================================

echo Testing: Service Health Check
echo URL: http://localhost:8080/api/mcp/health
curl -s -w "HTTP Status: %%{http_code}\n" http://localhost:8080/api/mcp/health
echo.

echo Testing: Service Configuration
echo URL: http://localhost:8080/api/mcp/config
curl -s -w "HTTP Status: %%{http_code}\n" http://localhost:8080/api/mcp/config
echo.

echo Testing: API Endpoints
echo URL: http://localhost:8080/api/mcp/endpoints
curl -s -w "HTTP Status: %%{http_code}\n" http://localhost:8080/api/mcp/endpoints
echo.

echo =====================================
echo MCP Test Completed!
echo.
echo Checking Service Status...
curl -s http://localhost:8080/actuator/health
if %ERRORLEVEL% EQU 0 (
    echo Service is running OK
) else (
    echo Service is not running, please start it:
    echo    start.bat
)