@echo off
echo Testing Group IM Server MCP Endpoints...
echo ========================================

echo Testing GET request to tools/call endpoint:
curl -X GET "http://localhost:8080/api/mcp/tools/call" -H "Accept: text/event-stream"
echo.

echo Testing POST request to tools/call endpoint:
curl -X POST "http://localhost:8080/api/mcp/tools/call" -H "Content-Type: application/json" -H "Accept: text/event-stream" -d "{\"name\":\"get_service_health\"}"
echo.

echo Testing tools list endpoint:
curl -X GET "http://localhost:8080/api/mcp/tools/list" -H "Accept: application/json"
echo.