# Docker Compose 部署文件

此目录包含使用 Docker Compose 部署应用所需的文件。

## 文件列表

- `docker-compose.yml` - 主要的 Docker Compose 配置（开发环境）
- `docker-compose.prod.yml` - 生产环境的 Docker Compose 配置
- `nginx.conf` - Nginx HTTP/HTTPS 代理配置
- `nginx-tcp.conf` - Nginx TCP 代理配置
- `../dockerfiles/Dockerfile` - JVM 版本的 Docker 镜像构建文件
- `../dockerfiles/Dockerfile.native` - GraalVM Native 版本的 Docker 镜像构建文件

## 部署说明

### 开发环境部署
```bash
# 启动开发环境
docker-compose -f docker-compose.yml up -d

# 构建并启动
docker-compose -f docker-compose.yml up -d --build
```

### 生产环境部署
```bash
# 启动生产环境（JVM版本）
docker-compose -f docker-compose.prod.yml up -d --build

# 启动生产环境（GraalVM Native版本）
# 注意：需要先构建Native镜像
docker-compose -f docker-compose.prod.yml up -d --build
```

### 常用命令
```bash
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
- **OpenLDAP**: 目录服务 (端口 389/636)
- **phpLDAPadmin**: LDAP 管理界面 (端口 8085)
- **IM Server**: 主应用服务 (端口 8080, 8088)
- **Nginx**: 反向代理服务 (端口 80, 443, 8088)

## 访问服务

- **应用服务**: http://localhost:8080
- **TCP 长连接服务**: localhost:8088
- **LDAP管理**: http://localhost:8085
- **数据库**: localhost:5432
- **缓存**: localhost:6379

## 部署要求

- Docker 20.10+
- Docker Compose V2
- 至少 4GB 可用内存
- 至少 5GB 可用磁盘空间

## 目录结构

部署时会自动创建以下目录用于数据持久化：
- `postgres_data/` - PostgreSQL 数据存储
- `redis_data/` - Redis 数据存储
- `ldap_data/` - LDAP 数据存储
- `ldap_config/` - LDAP 配置存储
- `logs/app/` - 应用日志存储
- `storage/` - 文件存储
- `ssl/` - SSL证书存储

## 详细部署说明

请参阅根目录的 DEPLOYMENT.md 文件获取详细部署指南。