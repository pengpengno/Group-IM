#!/bin/bash

# IM Group Server 一键部署脚本
# 用法：curl ... | bash

set -e

DEPLOY_DIR="/opt/app"

echo "=========================================="
echo "  IM Group Server 一键部署"
echo "=========================================="

# 创建目录
echo "创建部署目录..."
#mkdir -p "$DEPLOY_DIR"/{logs/app,storage,ssl,scripts}
cd "$DEPLOY_DIR"

# 设置权限
#if id "deploy" &>/dev/null; then
#    chown -R deploy:deploy "$DEPLOY_DIR"
#    chmod -R 755 "$DEPLOY_DIR"
#fi

# 下载配置文件（覆盖旧版本）
echo "下载配置文件..."
GITHUB_RAW_BASE="https://raw.githubusercontent.com/Group-IM/master"

curl -sL "${GITHUB_RAW_BASE}/deploy/docker/docker-compose.cicd.yml" -o docker-compose.yml || exit 1
curl -sL "${GITHUB_RAW_BASE}/deploy/docker/nginx.conf" -o nginx.conf || exit 1
curl -sL "${GITHUB_RAW_BASE}/deploy/docker/nginx-tcp.conf" -o nginx-tcp.conf || exit 1
curl -sL "${GITHUB_RAW_BASE}/scripts/create_company_schema_function.sql" -o scripts/create_company_schema_function.sql || exit 1

echo "配置文件下载完成"

# 启动服务
echo "启动 Docker 服务..."
docker-compose up -d

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  - 应用：http://localhost:8080"
echo "  - TCP: localhost:8088"
echo ""
echo "管理命令："
echo "  - 查看状态：docker-compose ps"
echo "  - 查看日志：docker-compose logs -f"
echo "  - 停止：docker-compose down"
echo "  - 重启：docker-compose restart"
echo ""
