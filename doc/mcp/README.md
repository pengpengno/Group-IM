# Group IM Server MCP 服务

## 🎯 概述

Group IM Server MCP服务允许AI助手（如Lingma）直接获取即时通讯服务端的完整信息，包括API接口、配置详情、业务实体结构等。

## 🚀 快速开始

### 1. 启动服务端

```bash
# 使用一键启动脚本
./start.sh

# 或手动启动
cd server
../mvnw spring-boot:run
```

服务启动后，默认监听 `http://localhost:8080`

### 2. 验证MCP接口

```bash
# 获取服务摘要信息
curl http://localhost:8080/api/mcp/summary

# 获取API接口列表
curl http://localhost:8080/api/mcp/endpoints

# 获取服务配置
curl http://localhost:8080/api/mcp/config
```

## 🛠️ MCP工具列表

### 🔧 可用工具

| 工具名称 | 功能描述 | 使用场景 |
|---------|---------|---------|
| `get_service_summary` | 获取完整服务信息摘要 | 全面了解服务端结构 |
| `get_api_endpoints` | 获取所有REST API接口 | 了解可用的API端点 |
| `get_service_config` | 获取服务配置详情 | 获取数据库、Redis等配置 |
| `get_business_entities` | 获取业务实体结构 | 理解数据模型设计 |
| `get_service_health` | 获取服务健康状态 | 检查各组件运行状况 |

### 📦 资源类型

| 资源URI | 描述 | 内容类型 |
|---------|------|---------|
| `mcp://group-im/api-spec` | API接口规范 | JSON格式API文档 |
| `mcp://group-im/config-info` | 服务配置信息 | JSON格式配置详情 |
| `mcp://group-im/entity-models` | 业务实体模型 | JSON格式实体结构 |

## 💡 使用示例

### 1. Lingma集成配置

在Lingma的配置中添加MCP工具：

```json
{
  "mcp_tools": [
    {
      "name": "group-im-server",
      "type": "http",
      "config": {
        "base_url": "http://localhost:8080/api/mcp",
        "tools": [
          "get_service_summary",
          "get_api_endpoints", 
          "get_service_config",
          "get_business_entities",
          "get_service_health"
        ]
      }
    }
  ]
}
```

### 2. 典型使用场景

#### 场景1：生成API客户端代码
```
用户：帮我生成Group IM的Python客户端SDK
Lingma：正在查询服务端API接口... [调用get_api_endpoints]
Lingma：已获取到25个API端点，正在分析接口结构...
```

#### 场景2：数据库集成
```
用户：如何连接Group IM的数据库？
Lingma：正在获取数据库配置... [调用get_service_config]  
Lingma：服务端使用PostgreSQL数据库，连接信息：jdbc:postgresql://localhost:5432/group_im
```

#### 场景3：理解业务逻辑
```
用户：Group IM的消息是如何存储的？
Lingma：正在分析业务实体... [调用get_business_entities]
Lingma：消息实体包含：id, fromUserId, toUserId, content, type, timestamp, status等字段
```

## 🏗️ 技术架构

### 核心组件

```
MCP Info Controller (/api/mcp/*)
├── Service Summary Endpoint    # 综合信息接口
├── API Endpoints Endpoint      # 接口列表接口  
├── Config Endpoint             # 配置信息接口
├── Entities Endpoint           # 实体信息接口
└── Health Endpoint             # 健康检查接口
```

### 数据流向

```
Lingma MCP Client → HTTP Request → McpInfoController → Service Components → JSON Response
```

## 🔒 安全说明

- MCP接口默认无需认证（公开访问）
- 仅暴露配置信息和接口结构，不包含敏感数据
- 建议在生产环境中：
  - 添加IP白名单限制
  - 启用基本认证
  - 通过网关统一管理访问

## 📊 接口响应示例

### 获取服务摘要
```json
{
  "configuration": {
    "database": {
      "type": "PostgreSQL",
      "driver": "org.postgresql.Driver",
      "connection_example": "jdbc:postgresql://localhost:5432/group_im"
    },
    "redis": {
      "host": "localhost",
      "port": 6379
    }
  },
  "endpoints": {
    "total": 25,
    "endpoints": [...]
  },
  "entities": {
    "total": 3,
    "entities": [...]
  },
  "health": {
    "status": "UP",
    "components": {
      "database": "UP",
      "redis": "UP"
    }
  }
}
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进MCP服务：

1. Fork项目
2. 创建特性分支
3. 提交更改
4. 发起Pull Request

## 📄 许可证

MIT License - 详见[LICENSE](../../LICENSE)文件

---
*让AI更好地理解和使用你的服务端！*