# IM Group Server 部署指南

## 快速启动

如果您想快速启动服务，可以直接使用一键部署脚本：

**Docker Compose 方式 (推荐):**
```bash
# Linux/macOS - 生产环境（CI/CD 方式，使用远程镜像）
./deploy/scripts/deploy.sh

# Linux/macOS - 开发环境（本地构建镜像）
./start.sh

# Windows - 生产环境
deploy\scripts\deploy.bat

# Windows - 开发环境
start.bat
```

**Kubernetes 方式 (适用于 k3s 单节点环境):**
```bash
# Linux/macOS
./deploy/scripts/deploy-k8s-simple.sh

# Windows
./deploy/scripts/deploy-k8s-simple.ps1
```

**Kubernetes 方式 (适用于多节点生产环境):**
```bash
# Linux/macOS
./deploy/scripts/deploy-k8s.sh

# Windows
./deploy/scripts/deploy-k8s.ps1
```

详细的一键启动指南请参见 [QUICKSTART.md](QUICKSTART.md) 文件。

本指南将帮助您部署 IM Group Server 到生产环境。

## 目录结构

部署相关的文件已整理到 `deploy/` 目录下：

```
deploy/
├── k8s/                    # Kubernetes 部署文件
│   ├── k8s-postgres-statefulset.yaml
│   ├── k8s-redis-statefulset.yaml
│   ├── k8s-openldap-statefulset.yaml
│   ├── k8s-phpldapadmin-deployment.yaml
│   ├── k8s-im-server-deployment.yaml
│   ├── k8s-nginx-ingress.yaml
│   └── README.md
├── docker/                 # Docker Compose 部署文件
│   ├── docker-compose.yml           # 开发环境（本地构建）
│   ├── docker-compose.cicd.yml      # 生产环境（远程镜像）
│   ├── nginx.conf                   # Nginx HTTP/HTTPS配置
│   ├── nginx-tcp.conf               # Nginx TCP长连接配置
│   ├── create_company_schema_function.sql  # 数据库初始化脚本
│   └── README.md
├── dockerfiles/            # Docker 镜像构建文件
│   ├── Dockerfile          # JVM 版本构建文件
│   └── Dockerfile.native   # GraalVM Native 版本构建文件
├── scripts/                # 部署脚本
│   ├── deploy-k8s.sh
│   ├── deploy-k8s.ps1
│   ├── deploy.sh          # 生产环境一键部署脚本
│   └── deploy.bat
├── K8S-DEPLOYMENT.md      # Kubernetes 部署文档
└── README.md
```

## 环境要求

- Docker 20.10+
- Docker Compose V2
- 至少 4GB 可用内存
- 至少 5GB 可用磁盘空间

## 快速部署

### 1. 使用部署脚本（推荐）

**生产环境部署（CI/CD 方式 - 使用远程镜像）：**

Linux/macOS:
```bash
./deploy/scripts/deploy.sh
```

Windows:
```cmd
deploy\scripts\deploy.bat
```

**开发环境部署（本地构建镜像）：**

在项目根目录执行：

Linux/macOS:
```bash
./start.sh
```

Windows:
```cmd
start.bat
```

然后选择选项 `1) 本地开发部署`

## GraalVM Native Image 支持

项目支持使用 GraalVM 构建原生镜像，以获得更快的启动速度和更低的内存占用。

### 构建原生镜像

在构建原生镜像之前，您需要安装 GraalVM 和 native-image 工具：

```bash
# 安装 native-image 工具
gu install native-image

# 构建原生镜像
./mvnw -Pnative native:compile
```

### 生产环境部署 (GraalVM Native)

使用以下命令启动使用原生镜像的服务：

```bash
# Linux/macOS
./deploy/scripts/deploy.sh prod-native

# Windows
deploy\scripts\deploy.bat prod-native
```

### 优势

- **启动速度**：原生镜像启动时间显著减少（通常在1秒内）
- **内存占用**：内存使用量减少约30-50%
- **CPU效率**：更高的CPU效率和响应速度

### 注意事项

- 构建原生镜像需要较长的时间（首次构建可能需要10-20分钟）
- 需要 GraalVM 22.3+ 和相应的构建工具
- 某些动态特性（如反射）需要预先配置

## 环境变量配置

您可以修改 `docker-compose.yml` 文件中的环境变量来自定义部署：

### 数据库配置
```yaml
- POSTGRES_DB=group
- POSTGRES_USER=postgres
- POSTGRES_PASSWORD=postgres
```

### 系统初始化配置
```yaml
- SYSTEM_INITIALIZER_ENABLED=true
- ADMIN_USERNAME=admin
- ADMIN_PASSWORD=12345
```

### LDAP 配置
```yaml
- LDAP_ORGANISATION="MyCompany"
- LDAP_DOMAIN="mycompany.com"
- LDAP_ADMIN_PASSWORD="admin"
```

## 服务组件

部署后，以下服务将在容器中运行：

### 生产环境（docker-compose.cicd.yml）
- **nginx**: Nginx 反向代理服务 (端口 80, 443, 8088)
- **postgres**: PostgreSQL 数据库服务 (端口 5432)
- **redis**: Redis 缓存服务 (端口 6379)
- **server**: Spring Boot 应用服务 (端口 8080 HTTP, 8088 TCP)

### 开发环境（docker-compose.yml）
- **postgres**: PostgreSQL 数据库服务 (端口 5432)
- **redis**: Redis 缓存服务 (端口 6379)
- **openldap**: LDAP 目录服务 (端口 389/636)
- **phpldapadmin**: LDAP 管理界面 (端口 8085)
- **server**: Spring Boot 应用服务 (端口 8080, 8088)
- **nginx**: Nginx 反向代理服务 (端口 80, 443, 8088)

## 访问服务

### 生产环境（CI/CD）
- **应用服务 (HTTP)**: http://服务器 IP:8080 (经 Nginx 代理)
- **TCP 长连接服务**: 服务器 IP:8088 (经 Nginx TCP 代理)
- **HTTPS 服务**: https://服务器 IP:443 (Nginx SSL 代理)

### 开发环境
- **应用服务 (HTTP)**: http://localhost:8080 (经 Nginx 代理)
- **TCP 长连接服务**: localhost:8088 (经 Nginx TCP 代理)
- **HTTPS 服务**: https://localhost:443 (Nginx SSL 代理)
- **LDAP 管理**: http://localhost:8085
  - 用户名：`cn=admin,dc=mycompany,dc=com`
  - 密码：`admin`

## Nginx 配置

Nginx 配置包含两个部分：

1. **HTTP/HTTPS 代理**: 用于处理 Web 请求和 WebSocket 连接
2. **TCP 代理**: 专门用于处理 IM 系统的 TCP 长连接

配置文件：
- `nginx.conf`: 主配置文件，包含 HTTP/HTTPS 设置
- `nginx-tcp.conf`: TCP 代理配置，用于 8088 端口的长连接
- `create_company_schema_function.sql`: PostgreSQL 数据库初始化脚本

## 生产环境建议

### 1. 安全配置

- 修改所有默认密码
- 配置 SSL/TLS 加密
- 限制网络访问权限
- 启用防火墙规则

### 2. 数据持久化

数据卷已配置为持久化存储：
- PostgreSQL 数据: `./postgres_data`
- Redis 数据: `./redis_data`
- LDAP 数据: `./ldap_data`
- 应用日志: `./logs/app`
- 文件存储: `./storage`

### 3. 监控和日志

查看服务日志：
```bash
docker-compose logs -f
```

查看特定服务日志：
```bash
docker-compose logs -f server
```

### 4. 备份策略

定期备份重要数据：
```bash
# 备份 PostgreSQL 数据库
docker-compose exec postgres pg_dump -U postgres group > backup_$(date +%Y%m%d_%H%M%S).sql

# 备份 Redis 数据
docker-compose exec redis redis-cli BGSAVE
```

## 常见问题

### 服务启动失败

检查日志输出：
```bash
docker-compose logs server
```

### 端口冲突

修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "8081:8080"  # 修改为其他端口
  - "8089:8088"
```

### 内存不足

调整 JVM 参数：
```yaml
JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC
```

## 管理命令

### 生产环境（CI/CD）

启动服务：
```bash
docker-compose -f docker-compose.cicd.yml up -d
```

停止服务：
```bash
docker-compose -f docker-compose.cicd.yml down
```

重启服务：
```bash
docker-compose -f docker-compose.cicd.yml restart server
```

查看服务状态：
```bash
docker-compose -f docker-compose.cicd.yml ps
```

查看日志：
```bash
docker-compose -f docker-compose.cicd.yml logs -f server
```

### 开发环境

启动服务：
```bash
docker-compose -f docker-compose.yml up -d
```

停止服务：
```bash
docker-compose -f docker-compose.yml down
```

重启服务：
```bash
docker-compose -f docker-compose.yml restart server
```

查看服务状态：
```bash
docker-compose -f docker-compose.yml ps
```

构建并重启服务：
```bash
docker-compose -f docker-compose.yml up -d --build
```

## 扩容和负载均衡

如需支持更多用户，可考虑：
- 增加服务实例数量
- 使用外部负载均衡器
- 优化数据库连接池配置
- 添加 Redis 集群

## 故障恢复

### 服务自动重启

配置了 `restart: unless-stopped` 策略，容器会在异常退出后自动重启。

### 数据恢复

从备份恢复 PostgreSQL：
```bash
cat backup.sql | docker-compose exec -T postgres psql -U postgres group
```

## 版本升级

### 生产环境（CI/CD）

```bash
# 1. 拉取最新镜像
docker pull pengpeng163/groupim:master

# 2. 停止当前服务
docker-compose -f docker-compose.cicd.yml down

# 3. 启动新版本
docker-compose -f docker-compose.cicd.yml up -d

# 4. 查看日志确认启动成功
docker-compose -f docker-compose.cicd.yml logs -f server
```

### 开发环境

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 重建并启动服务
docker-compose -f docker-compose.yml down
docker-compose -f docker-compose.yml up -d --build
```

## 联系支持

如有问题，请联系开发团队或参考项目文档。