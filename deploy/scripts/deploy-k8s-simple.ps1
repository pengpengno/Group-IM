# IM Group Server Kubernetes 简化部署脚本 (PowerShell)
# 适用于 k3s 单节点环境

Write-Host "=== IM Group Server Kubernetes 简化部署 (k3s 单节点) ===" -ForegroundColor Green

# 检查是否安装了必要的工具
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 未找到 kubectl 命令，请先安装 kubectl" -ForegroundColor Red
    exit 1
}

# 检查 kubectl 是否能连接到集群
try {
    kubectl cluster-info *> $null
} catch {
    Write-Host "错误: 无法连接到 Kubernetes 集群，请确认集群正在运行" -ForegroundColor Red
    exit 1
}

Write-Host "检查集群连接..." -ForegroundColor Yellow
kubectl cluster-info

# 部署所有服务
Write-Host "部署所有服务..." -ForegroundColor Yellow
kubectl apply -f ..\k8s-simple\simple-all-in-one.yaml

Write-Host "等待服务启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 30

# 检查所有服务状态
Write-Host "检查所有服务状态..." -ForegroundColor Yellow
kubectl get pods
kubectl get services

Write-Host ""
Write-Host "=== 部署完成 ===" -ForegroundColor Green
Write-Host "服务已启动，可以通过以下地址访问：" -ForegroundColor Cyan
Write-Host "  - 应用服务 (HTTP): http://<YOUR_SERVER_IP>:<NODE_PORT>" -ForegroundColor Cyan
Write-Host "  - TCP 长连接服务: http://<YOUR_SERVER_IP>:<NODE_PORT>" -ForegroundColor Cyan
Write-Host "  - LDAP管理: http://<YOUR_SERVER_IP>:<NODE_PORT>" -ForegroundColor Cyan

Write-Host ""
Write-Host "要查看日志，请运行:" -ForegroundColor Yellow
Write-Host "  kubectl logs -f deployment/im-server" -ForegroundColor White
Write-Host "  kubectl logs -f deployment/nginx-proxy" -ForegroundColor White

Write-Host ""
Write-Host "要删除部署，请运行:" -ForegroundColor Yellow
Write-Host "  kubectl delete -f ..\k8s-simple\simple-all-in-one.yaml" -ForegroundColor White
Write-Host ""