@echo off
chcp 65001 > nul

echo === IM Group Server 部署脚本 ===

REM 检查 Docker 是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到 docker 命令，请先安装 Docker
    pause
    exit /b 1
)

REM 检查 Docker Compose 是否安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo 检查 Docker Compose V2...
    docker compose version >nul 2>&1
    if errorlevel 1 (
        echo 错误: 未找到 docker compose 命令，请先安装 Docker Compose
        pause
        exit /b 1
    ) else (
        set COMPOSE_CMD=docker compose
    )
) else (
    set COMPOSE_CMD=docker-compose
)

echo 创建必要目录...
if not exist "..\..\logs\app" mkdir ..\..\logs\app
if not exist "..\..\postgres_data" mkdir ..\..\postgres_data
if not exist "..\..\redis_data" mkdir ..\..\redis_data
if not exist "..\..\ldap_data" mkdir ..\..\ldap_data
if not exist "..\..\ldap_config" mkdir ..\..\ldap_config
if not exist "..\..\storage" mkdir ..\..\storage
if not exist "..\..\ssl" mkdir ..\..\ssl

REM 构建并启动服务
echo 启动 IM Group Server...

if "%1"=="prod-native" (
    echo 使用生产模式 ^(GraalVM Native^) 启动...
    %COMPOSE_CMD% -f ..\..\docker-compose.prod.yml up -d --build
) else if "%1"=="prod" (
    echo 使用生产模式启动...
    %COMPOSE_CMD% -f ..\..\docker-compose.prod.yml up -d --build
) else if "%1"=="dev" (
    echo 使用开发模式启动...
    %COMPOSE_CMD% -f ..\..\docker-compose.yml up -d --build
) else (
    echo 使用默认模式启动...
    %COMPOSE_CMD% -f ..\..\docker-compose.yml up -d --build
)

REM 等待服务启动
echo 等待服务启动...
timeout /t 10 /nobreak >nul

REM 检查服务状态
echo 检查服务状态...
%COMPOSE_CMD% -f ..\..\docker-compose.yml ps

echo.
echo === 部署完成 ===
echo 服务已启动，可以通过以下地址访问：
echo   - 应用服务: http://localhost:8080
echo   - TCP 服务: localhost:8088
echo   - HTTPS 服务: https://localhost:443 ^(Nginx SSL代理^)
echo   - LDAP管理: http://localhost:8085 ^^(phpLDAPadmin^^^)
echo   - PostgreSQL: localhost:5432 ^^(数据库^^^)
echo   - Redis: localhost:6379 ^^(缓存^^^)
echo.
echo 要查看日志，请运行: %COMPOSE_CMD% -f ..\..\docker-compose.yml logs -f
echo 要停止服务，请运行: %COMPOSE_CMD% -f ..\..\docker-compose.yml down
echo.
pause