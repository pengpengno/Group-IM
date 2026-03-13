# Docker Compose 部署文件

此目录包含使用 Docker Compose 部署应用所需的文件。

## 文件列表

- `docker-compose.yml` - 主要的 Docker Compose 配置（开发环境，本地构建镜像）
- `docker-compose.cicd.yml` - CI/CD 生产环境配置（使用远程镜像）
- `nginx.conf` - Nginx HTTP/HTTPS 代理配置
- `nginx-tcp.conf` - Nginx TCP 长连接代理配置
- `create_company_schema_function.sql` - PostgreSQL 数据库初始化脚本（多公司 schema 支持）
- `../dockerfiles/Dockerfile` - JVM 版本的 Docker 镜像构建文件
- `../dockerfiles/Dockerfile.native` - GraalVM Native 版本的 Docker 镜像构建文件

## 部署说明

## 部署说明

### 开发环境部署
```bash
# 启动开发环境（本地构建镜像）
docker-compose -f docker-compose.yml up -d

# 重新构建并启动
docker-compose -f docker-compose.yml up -d --build
```

### 生产环境部署（CI/CD）
```bash
# 启动生产环境（使用远程镜像，无需构建）
docker-compose -f docker-compose.cicd.yml up -d

# 查看服务状态
docker-compose -f docker-compose.cicd.yml ps

# 查看日志
docker-compose -f docker-compose.cicd.yml logs -f
```

### 常用命令
```bash
# 生产环境
# 查看服务状态
docker-compose -f docker-compose.cicd.yml ps

# 查看日志
docker-compose -f docker-compose.cicd.yml logs -f

# 停止服务
docker-compose -f docker-compose.cicd.yml down

# 开发环境
# 查看服务状态
docker-compose -f docker-compose.yml ps

# 查看日志
docker-compose -f docker-compose.yml logs -f

# 停止服务
docker-compose -f docker-compose.yml down

# 重新构建并启动
docker-compose -f docker-compose.yml up -d --build
```

## 服务组件

- **PostgreSQL**: 数据库服务 (端口 5432)
- **Redis**: 缓存服务 (端口 6379)
- **IM Server**: 主应用服务 (端口 8080 HTTP, 8088 TCP 长连接)
- **Nginx**: 反向代理服务 (端口 80 HTTP, 443 HTTPS, 8088 TCP)

### 可选组件（开发环境）
- **OpenLDAP**: 目录服务 (端口 389/636) - 仅开发环境使用
- **phpLDAPadmin**: LDAP 管理界面 (端口 8085) - 仅开发环境使用

## 访问服务

### 开发环境
- **应用服务**: http://localhost:8080
- **TCP 长连接服务**: localhost:8088
- **LDAP 管理**: http://localhost:8085
- **数据库**: localhost:5432
- **缓存**: localhost:6379

### 生产环境（CI/CD）
- **应用服务**: http://服务器 IP:8080
- **TCP 长连接服务**: 服务器 IP:8088

## 部署要求

- Docker 20.10+
- Docker Compose V2
- 至少 4GB 可用内存
- 至少 5GB 可用磁盘空间

## 目录结构

部署时会自动创建以下目录用于数据持久化：
- `postgres_data/` - PostgreSQL 数据存储
- `redis_data/` - Redis 缓存数据
- `logs/app/` - 应用日志存储
- `storage/` - 文件存储
- `ssl/` - SSL 证书存储

### 开发环境特有目录
- `ldap_data/` - LDAP 数据存储（仅开发环境）
- `ldap_config/` - LDAP 配置存储（仅开发环境）

## 详细部署说明

请参阅根目录的 DEPLOYMENT.md 文件获取详细部署指南。