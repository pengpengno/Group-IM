#!/bin/bash

# IM Group Server 一键启动脚本

echo "==========================================="
echo "    IM Group Server 一键启动脚本"
echo "==========================================="

echo ""
echo "请选择部署模式："
echo "1) 开发模式 (Docker Compose)"
echo "2) 生产模式 (Docker Compose)"
echo "3) 生产模式 (GraalVM Native, Docker Compose)"
echo "4) Kubernetes 模式"
echo "5) 停止服务"
echo ""

read -p "请输入选项 (1-5): " option

case $option in
    1)
        echo "启动开发模式..."
        ./deploy/scripts/deploy.sh
        ;;
    2)
        echo "启动生产模式..."
        ./deploy/scripts/deploy.sh prod
        ;;
    3)
        echo "启动生产模式 (GraalVM Native)..."
        ./deploy/scripts/deploy.sh prod-native
        ;;
    4)
        echo "启动 Kubernetes 简化模式 (k3s单节点)..."
        chmod +x ./deploy/scripts/deploy-k8s-simple.sh
        ./deploy/scripts/deploy-k8s-simple.sh
        ;;
    5)
        echo "启动 Kubernetes 完整模式..."
        chmod +x ./deploy/scripts/deploy-k8s.sh
        ./deploy/scripts/deploy-k8s.sh
        ;;
    5)
        echo "停止服务..."
        cd deploy/docker && docker-compose down
        ;;
    *)
        echo "无效选项！"
        exit 1
        ;;
esac