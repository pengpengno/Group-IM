# IM Group Server Kubernetes 部署脚本 (PowerShell)

Write-Host "=== IM Group Server Kubernetes 部署脚本 ===" -ForegroundColor Green

# 检查是否安装了必要的工具
if (!(Get-Command kubectl -ErrorAction SilentlyContinue)) {
    Write-Host "错误: 未找到 kubectl 命令，请先安装 kubectl" -ForegroundColor Red
    exit 1
}

try {
    kubectl cluster-info *> $null
} catch {
    Write-Host "错误: 无法连接到 Kubernetes 集群，请确认集群正在运行" -ForegroundColor Red
    exit 1
}

Write-Host "检查集群连接..." -ForegroundColor Yellow
kubectl cluster-info

# 应用配置
Write-Host "部署 ConfigMaps..." -ForegroundColor Yellow
kubectl apply -f ..\k8s\k8s-postgres-statefulset.yaml
kubectl apply -f ..\k8s\k8s-redis-statefulset.yaml
kubectl apply -f ..\k8s\k8s-openldap-statefulset.yaml
kubectl apply -f ..\k8s\k8s-phpldapadmin-deployment.yaml

Write-Host "等待基础服务启动..." -ForegroundColor Yellow
kubectl rollout status statefulset/postgres-db --timeout=120s
kubectl rollout status statefulset/redis-cache --timeout=120s
kubectl rollout status statefulset/openldap --timeout=120s

Write-Host "部署应用服务..." -ForegroundColor Yellow
kubectl apply -f ..\k8s\k8s-im-server-deployment.yaml
kubectl apply -f ..\k8s\k8s-nginx-ingress.yaml

Write-Host "等待应用服务启动..." -ForegroundColor Yellow
kubectl rollout status deployment/im-server --timeout=180s
kubectl rollout status deployment/nginx-proxy --timeout=120s

Write-Host "检查所有服务状态..." -ForegroundColor Yellow
kubectl get pods
kubectl get services
kubectl get pvc

Write-Host ""
Write-Host "=== 部署完成 ===" -ForegroundColor Green
Write-Host "服务已启动，可以通过以下地址访问：" -ForegroundColor Cyan

$nginxIP = $(kubectl get svc nginx-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
$phpldapIP = $(kubectl get svc phpldapadmin-service -o jsonpath='{.status.loadBalancer.ingress[0].ip}')

Write-Host "  - 应用服务 (HTTP): $nginxIP`:80" -ForegroundColor Cyan
Write-Host "  - 应用服务 (HTTPS): $nginxIP`:443" -ForegroundColor Cyan
Write-Host "  - TCP 长连接服务: $nginxIP`:8088" -ForegroundColor Cyan
Write-Host "  - LDAP管理: $phpldapIP`:8085" -ForegroundColor Cyan

Write-Host ""
Write-Host "要查看日志，请运行:" -ForegroundColor Yellow
Write-Host "  kubectl logs -f deployment/im-server" -ForegroundColor White
Write-Host "  kubectl logs -f deployment/nginx-proxy" -ForegroundColor White

Write-Host ""
Write-Host "要删除部署，请运行:" -ForegroundColor Yellow
Write-Host "  kubectl delete -f ..\k8s\k8s-postgres-statefulset.yaml" -ForegroundColor White
Write-Host "  kubectl delete -f ..\k8s\k8s-redis-statefulset.yaml" -ForegroundColor White
Write-Host "  kubectl delete -f ..\k8s\k8s-openldap-statefulset.yaml" -ForegroundColor White
Write-Host "  kubectl delete -f ..\k8s\k8s-phpldapadmin-deployment.yaml" -ForegroundColor White
Write-Host "  kubectl delete -f ..\k8s\k8s-im-server-deployment.yaml" -ForegroundColor White
Write-Host "  kubectl delete -f ..\k8s\k8s-nginx-ingress.yaml" -ForegroundColor White
Write-Host ""