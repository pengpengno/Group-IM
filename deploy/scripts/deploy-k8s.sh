#!/bin/bash

# IM Group Server Kubernetes 部署脚本

set -e  # 遇到错误立即退出

echo "=== IM Group Server Kubernetes 部署脚本 ==="

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

# 应用配置
echo "部署 ConfigMaps..."
kubectl apply -f ../k8s/k8s-postgres-statefulset.yaml
kubectl apply -f ../k8s/k8s-redis-statefulset.yaml
kubectl apply -f ../k8s/k8s-openldap-statefulset.yaml
kubectl apply -f ../k8s/k8s-phpldapadmin-deployment.yaml

echo "等待基础服务启动..."
kubectl rollout status statefulset/postgres-db --timeout=120s
kubectl rollout status statefulset/redis-cache --timeout=120s
kubectl rollout status statefulset/openldap --timeout=120s

echo "部署应用服务..."
kubectl apply -f ../k8s/k8s-im-server-deployment.yaml
kubectl apply -f ../k8s/k8s-nginx-ingress.yaml

echo "等待应用服务启动..."
kubectl rollout status deployment/im-server --timeout=180s
kubectl rollout status deployment/nginx-proxy --timeout=120s

echo "检查所有服务状态..."
kubectl get pods
kubectl get services
kubectl get pvc

echo ""
echo "=== 部署完成 ==="
echo "服务已启动，可以通过以下地址访问："
echo "  - 应用服务 (HTTP): $(kubectl get svc nginx-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):80"
echo "  - 应用服务 (HTTPS): $(kubectl get svc nginx-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):443"
echo "  - TCP 长连接服务: $(kubectl get svc nginx-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):8088"
echo "  - LDAP管理: $(kubectl get svc phpldapadmin-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}'):8085"
echo ""
echo "要查看日志，请运行:"
echo "  kubectl logs -f deployment/im-server"
echo "  kubectl logs -f deployment/nginx-proxy"
echo ""
echo "要删除部署，请运行:"
echo "  kubectl delete -f ../k8s/k8s-postgres-statefulset.yaml"
echo "  kubectl delete -f ../k8s/k8s-redis-statefulset.yaml"
echo "  kubectl delete -f ../k8s/k8s-openldap-statefulset.yaml"
echo "  kubectl delete -f ../k8s/k8s-phpldapadmin-deployment.yaml"
echo "  kubectl delete -f ../k8s/k8s-im-server-deployment.yaml"
echo "  kubectl delete -f ../k8s/k8s-nginx-ingress.yaml"
echo ""