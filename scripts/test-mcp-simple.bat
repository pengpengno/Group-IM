@echo off
echo Simple GroupIM MCP Test
echo ========================

echo 1. Testing service health...
curl -s http://localhost:8080/actuator/health
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Service not running! Please start with start.bat
    pause
    exit /b 1
)
echo [OK] Service is running
echo.

echo 2. Testing MCP endpoints...
echo Testing service summary...
curl -s -w "Status: %%{http_code}\n" http://localhost:8080/api/mcp/demo/service-summary
echo.

echo Testing API endpoints...
curl -s -w "Status: %%{http_code}\n" http://localhost:8080/api/mcp/demo/api-endpoints
echo.

echo Testing service config...
curl -s -w "Status: %%{http_code}\n" http://localhost:8080/api/mcp/demo/service-config
echo.

echo 3. Testing batch call...
curl -s -X POST http://localhost:8080/api/mcp/demo/batch-call ^
  -H "Content-Type: application/json" ^
  -d "{\"service_summary\":true,\"api_endpoints\":true}" ^
  -w "Status: %%{http_code}\n"
echo.

echo 4. Testing standard MCP call...
curl -s -X POST http://localhost:8080/api/mcp/demo/call-tool ^
  -H "Content-Type: application/json" ^
  -d "{\"tool_name\":\"get_service_summary\",\"arguments\":{}}" ^
  -w "Status: %%{http_code}\n"
echo.

echo ========================
echo Test completed!
echo Visit http://localhost:8080/mcp/mcp-demo.html for demo
pause