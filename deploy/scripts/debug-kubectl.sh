#!/bin/bash

echo "=== kubectl 调试信息 ==="

echo "检查 kubectl 命令是否存在..."
if command -v kubectl > /dev/null 2>&1; then
    echo "✓ kubectl 命令存在"
    echo "kubectl 路径: $(which kubectl)"
    echo "kubectl 版本信息:"
    kubectl version --client
else
    echo "✗ kubectl 命令不存在"
    exit 1
fi

echo ""
echo "检查是否能连接到 Kubernetes 集群..."
if kubectl cluster-info > /dev/null 2>&1; then
    echo "✓ 成功连接到 Kubernetes 集群"
    kubectl cluster-info
else
    echo "✗ 无法连接到 Kubernetes 集群"
    echo "请确保 k3s 或其他 Kubernetes 集群正在运行"
    exit 1
fi

echo ""
echo "检查 kubectl 是否在 PATH 中..."
if [[ ":$PATH:" == *":$(dirname $(which kubectl)):"* ]]; then
    echo "✓ kubectl 在 PATH 中"
else
    echo "✗ kubectl 不在 PATH 中"
fi

echo ""
echo "所有检查完成"