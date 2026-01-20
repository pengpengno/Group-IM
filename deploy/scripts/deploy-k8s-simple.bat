@echo off
chcp 65001 > nul

echo ===========================================
echo     IM Group Server Kubernetes 简化部署
echo     (适用于 k3s 单节点环境)
echo ===========================================

REM 检查是否安装了必要的工具
kubectl version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 kubectl 命令，请先安装 kubectl
    pause
    exit /b 1
)

REM 检查 kubectl 是否能连接到集群
for /f "usebackq" %%i in (`kubectl cluster-info 2^>nul ^|^| echo failed`) do (
    if "%%i"=="failed" (
        echo 错误: 无法连接到 Kubernetes 集群，请确认集群正在运行
        pause
        exit /b 1
    )
)

echo 检查集群连接...
kubectl cluster-info

REM 部署所有服务
echo 部署所有服务...
kubectl apply -f ..\k8s-simple\simple-all-in-one.yaml

echo 等待服务启动...
timeout /t 30 /nobreak >nul

REM 检查所有服务状态
echo 检查所有服务状态...
kubectl get pods
kubectl get services

echo.
echo ===========================================
echo     部署完成
echo ===========================================
echo 服务已启动，可以通过以下地址访问：
echo   - 应用服务 (HTTP): http://^<YOUR_SERVER_IP^>:^<NODE_PORT^>
echo   - TCP 长连接服务: http://^<YOUR_SERVER_IP^>:^<NODE_PORT^>
echo   - LDAP管理: http://^<YOUR_SERVER_IP^>:^<NODE_PORT^>
echo.
echo 要查看日志，请运行:
echo   kubectl logs -f deployment/im-server
echo   kubectl logs -f deployment/nginx-proxy
echo.
echo 要删除部署，请运行:
echo   kubectl delete -f ..\k8s-simple\simple-all-in-one.yaml
echo.
pause