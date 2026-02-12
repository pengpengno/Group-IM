#!/bin/bash

# Group IM Server MCP 功能测试脚本

echo "🚀 开始测试 Group IM Server MCP 功能..."
echo "=========================================="

# 服务基础URL
BASE_URL="http://localhost:8080"
MCP_BASE="$BASE_URL/api/mcp"

# 测试函数
test_endpoint() {
    local endpoint=$1
    local description=$2
    
    echo "🔍 测试: $description"
    echo "   接口: $MCP_BASE/$endpoint"
    
    response=$(curl -s -w "%{http_code}" -X GET "$MCP_BASE/$endpoint" -H "Accept: application/json")
    http_code="${response: -3}"
    body="${response%???}"
    
    if [ "$http_code" = "200" ]; then
        echo "   ✅ 成功 (HTTP $http_code)"
        echo "   响应长度: ${#body} 字符"
    else
        echo "   ❌ 失败 (HTTP $http_code)"
        echo "   错误响应: $body"
    fi
    echo ""
}

# 执行测试
test_endpoint "health" "服务健康检查"
test_endpoint "config" "服务配置信息"  
test_endpoint "endpoints" "API接口列表"
test_endpoint "entities" "业务实体信息"
test_endpoint "summary" "完整服务摘要"

echo "=========================================="
echo "🎉 MCP功能测试完成!"

# 验证服务是否运行
echo ""
echo "🌐 验证服务状态..."
if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
    echo "✅ 服务端运行正常"
else
    echo "❌ 服务端未运行，请先启动服务:"
    echo "   ./start.sh"
fi