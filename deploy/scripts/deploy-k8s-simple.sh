#!/bin/bash

# IM Group Server Kubernetes 简化部署脚本
# 适用于 k3s 单节点环境

set -e  # 遇到错误立即退出

echo "=== IM Group Server Kubernetes 简化部署 (k3s 单节点) ==="

# 检查是否安装了必要的工具
if ! command -v kubectl >/dev/null 2>&1; then
    echo "错误: 未找到 kubectl 命令，请先安装 kubectl"
    exit 1
fi

# 检查 kubectl 是否能连接到集群
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo "错误: 无法连接到 Kubernetes 集群，请确认集群正在运行"
    exit 1
fi

echo "检查集群连接..."
kubectl cluster-info

# 部署所有服务
echo "部署所有服务..."
kubectl apply -f ../k8s-simple/simple-all-in-one.yaml

echo "等待服务启动..."
sleep 30

# 检查所有服务状态
echo "检查所有服务状态..."
kubectl get pods
kubectl get services

# 获取服务地址
NGINX_SVC=$(kubectl get svc nginx-service -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "未就绪")
IM_SERVER_SVC=$(kubectl get svc im-server-service -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "未就绪")
LDAP_SVC=$(kubectl get svc phpldapadmin-service -o jsonpath='{.spec.ports[0].nodePort}' 2>/dev/null || echo "未就绪")

echo ""
echo "=== 部署完成 ==="
echo "服务已启动，可以通过以下地址访问："
echo "  - 应用服务 (HTTP): http://$(hostname -I | awk '{print $1}'):${NGINX_SVC}"
echo "  - TCP 长连接服务: http://$(hostname -I | awk '{print $1}'):${IM_SERVER_SVC}"
echo "  - LDAP管理: http://$(hostname -I | awk '{print $1}'):${LDAP_SVC}"
echo ""
echo "要查看日志，请运行:"
echo "  kubectl logs -f deployment/im-server"
echo "  kubectl logs -f deployment/nginx-proxy"
echo ""
echo "要删除部署，请运行:"
echo "  kubectl delete -f ../k8s-simple/simple-all-in-one.yaml"
echo ""