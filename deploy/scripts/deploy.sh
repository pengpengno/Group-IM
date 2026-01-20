#!/bin/bash

# IM Group Server 部署脚本

set -e  # 遇到错误立即退出

echo "=== IM Group Server 部署脚本 ==="

# 检查是否安装了必要的工具
if ! command -v docker &> /dev/null; then
    echo "错误: 未找到 docker 命令，请先安装 Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "警告: 未找到 docker-compose 命令，尝试使用 docker compose (Docker Desktop v2.0.0.0+)"
    if ! docker compose version &> /dev/null; then
        echo "错误: 未找到 docker compose 命令，请先安装 Docker Compose"
        exit 1
    fi
    COMPOSE_CMD="docker compose"
else
    COMPOSE_CMD="docker-compose"
fi

# 创建必要的目录
echo "创建必要目录..."
mkdir -p ../../logs/app
mkdir -p ../../postgres_data
mkdir -p ../../redis_data
mkdir -p ../../ldap_data
mkdir -p ../../ldap_config
mkdir -p ../../storage
mkdir -p ../../ssl

# 构建并启动服务
echo "启动 IM Group Server..."

if [ "$1" = "prod-native" ]; then
    echo "使用生产模式 (GraalVM Native) 启动..."
    $COMPOSE_CMD -f ../../docker-compose.prod.yml up -d --build
elif [ "$1" = "prod" ]; then
    echo "使用生产模式启动..."
    $COMPOSE_CMD -f ../../docker-compose.prod.yml up -d --build
elif [ "$1" = "dev" ]; then
    echo "使用开发模式启动..."
    $COMPOSE_CMD -f ../../docker-compose.yml up -d --build
else
    echo "使用默认模式启动..."
    $COMPOSE_CMD -f ../../docker-compose.yml up -d --build
fi

# 等待服务启动
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
$COMPOSE_CMD -f ../../docker-compose.yml ps

echo ""
echo "=== 部署完成 ==="
echo "服务已启动，可以通过以下地址访问："
echo "  - 应用服务: http://localhost:8080"
echo "  - TCP 服务: localhost:8088"
echo "  - HTTPS 服务: https://localhost:443 (Nginx SSL代理)"
echo "  - LDAP 管理: http://localhost:8085 (phpLDAPadmin)"
echo "  - PostgreSQL: localhost:5432 (数据库)"
echo "  - Redis: localhost:6379 (缓存)"
echo ""
echo "要查看日志，请运行: $COMPOSE_CMD -f ../../docker-compose.yml logs -f"
echo "要停止服务，请运行: $COMPOSE_CMD -f ../../docker-compose.yml down"
echo ""