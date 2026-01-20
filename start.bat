@echo off
chcp 65001 > nul

echo ===========================================
echo     IM Group Server 一键启动脚本
echo ===========================================

echo.
echo 请选择部署模式：
echo 1) 开发模式 (Docker Compose)
echo 2) 生产模式 (Docker Compose)
echo 3) 生产模式 (GraalVM Native, Docker Compose)
echo 4) Kubernetes 简化模式 (k3s单节点)
echo 5) Kubernetes 完整模式
echo 6) 停止服务
echo.

set /p option="请输入选项 (1-6): "

if "%option%"=="1" (
    echo 启动开发模式...
    call deploy\scripts\deploy.bat
) else if "%option%"=="2" (
    echo 启动生产模式...
    call deploy\scripts\deploy.bat prod
) else if "%option%"=="3" (
    echo 启动生产模式 ^(GraalVM Native^)...
    call deploy\scripts\deploy.bat prod-native
) else if "%option%"=="4" (
    echo 启动 Kubernetes 简化模式 ^(k3s单节点^)...
    call deploy\scripts\deploy-k8s-simple.bat
) else if "%option%"=="5" (
    echo 启动 Kubernetes 完整模式...
    call deploy\scripts\deploy-k8s.bat
) else if "%option%"=="6" (
    echo 停止服务...
    cd deploy\docker && docker-compose down
) else (
    echo 无效选项！
    pause
    exit /b 1
)

pause