# IM Group Server 部署指南

## 目录结构

```
/opt/app/                        # 部署根目录
├── docker-compose.yml           # Docker Compose 配置文件
├── nginx.conf                   # Nginx HTTP 配置
├── nginx-tcp.conf              # Nginx TCP 配置
├── .env                        # 环境变量配置
├── logs/                       # 日志目录
│   └── app/
├── storage/                    # 文件存储目录
├── ssl/                        # SSL 证书目录
└── scripts/                    # 脚本目录
    └── create_company_schema_function.sql  # 数据库初始化脚本
```

## 快速开始

### 1. 生产环境部署（CI/CD 方式 - 推荐）

使用预构建的 Docker 镜像，无需本地编译：

```bash
# 一键部署脚本
curl -sL https://raw.githubusercontent.com/pengpengno/Group-IM/master/deploy/scripts/deploy.sh | bash
```

或手动执行：

```bash
cd /opt/app
docker-compose -f docker-compose.cicd.yml up -d
```

### 2. 开发环境部署（本地构建）

在项目根目录执行：

```bash
./start.sh
```

选择选项 `1) 本地开发部署`

或使用 Docker Compose：

```bash
cd deploy/docker
docker-compose -f docker-compose.yml up -d --build
```

## GitHub Actions 自动部署

### 配置 Secrets

在 GitHub 仓库设置中添加以下 Secrets：

| Secret Name | Description | Example |
|------------|-------------|---------|
| `DOCKER_USERNAME` | Docker Hub 用户名 | `pengpeng163` |
| `DOCKER_PASSWORD` | Docker Hub 密码/token | `your-token` |
| `SERVER_HOST` | 生产服务器 IP | `192.168.1.100` |
| `SERVER_USER` | 服务器用户 | `root` |
| `SERVER_SSH_KEY` | SSH 私钥 | `-----BEGIN RSA PRIVATE KEY-----...` |

### 触发条件

- 推送到 `main` 分支：自动构建并部署
- 创建 Tag：自动构建并部署对应版本
- PR：只构建，不部署

## 本地开发部署

在项目根目录执行：

```bash
./start.sh
```

选择选项 `1) 本地开发部署`

## 配置文件说明

### docker-compose.cicd.yml（生产环境）

使用远程 Docker Hub 镜像，适用于生产环境快速部署：

**包含服务：**
- **nginx**: Nginx 反向代理（HTTP/HTTPS/TCP）
- **postgres**: PostgreSQL 数据库
- **redis**: Redis 缓存
- **server**: Spring Boot 应用服务（JVM 版本）

**特点：**
- 无需本地构建镜像
- 使用 `pengpeng163/groupim:master` 镜像
- 配置了完整的健康检查
- 支持 TCP 长连接（端口 8088）

### docker-compose.yml（开发环境）

本地构建 Docker 镜像，适用于开发测试：

**包含服务：**
- **postgres**: PostgreSQL 数据库
- **redis**: Redis 缓存
- **openldap**: LDAP 目录服务（仅开发环境）
- **phpldapadmin**: LDAP 管理界面（仅开发环境）
- **server**: Spring Boot 应用服务（本地构建）
- **nginx**: Nginx 反向代理

**特点：**
- 从源代码构建镜像
- 支持热更新
- 包含完整的开发环境组件

### nginx.conf

**功能：**
- 端口 80: HTTP 反向代理到应用服务
- 端口 443: HTTPS（需配置 SSL 证书）
- 端口 8088: TCP 长连接代理（用于 IM 消息推送）
- 健康检查端点：`/actuator/health/liveness`

### nginx-tcp.conf

**功能：**
- TCP 长连接代理配置
- 将 8088 端口的流量转发到后端服务
- 支持 WebSocket 协议升级

### create_company_schema_function.sql

**功能：**
- PostgreSQL 数据库初始化脚本
- 创建公司 schema 管理函数
- 支持多租户架构

### 环境变量 (.env)

关键配置项：

```bash
# 数据库
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/group
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# Redis
SPRING_DATA_REDIS_HOST=redis
SPRING_DATA_REDIS_PORT=6379

# 系统初始化
SYSTEM_INITIALIZER_ENABLED=true
ADMIN_PASSWORD=your_admin_password
```

## 健康检查

### Docker Compose 健康检查

```yaml
healthcheck:
  test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health/liveness"]
  interval: 30s
  timeout: 10s
  retries: 3
  start_period: 60s
```

### 检查命令

```bash
# 查看容器健康状态
docker inspect --format='{{.State.Health.Status}}' im-server-cicd

# 查看详细日志
docker inspect --format='{{json .State.Health.Log}}' im-server-cicd | jq
```

## 常见问题

### 1. 下载配置文件失败

**问题：** 使用 `deploy.sh` 脚本时提示下载失败

**解决：**
```bash
# 检查网络连接
curl -I https://raw.githubusercontent.com

# 手动下载配置文件
cd /opt/app
wget https://raw.githubusercontent.com/pengpengno/Group-IM/master/deploy/docker/docker-compose.cicd.yml -O docker-compose.yml
```

### 2. 端口冲突

如果端口被占用，修改对应配置文件：

```yaml
# docker-compose.cicd.yml
ports:
  - "8081:8080"  # 改为其他端口
```

### 3. 磁盘空间不足

清理旧镜像：

```bash
docker image prune -a
```

### 4. 服务启动失败

查看日志：

```bash
# 生产环境
docker-compose -f docker-compose.cicd.yml logs -f server

# 开发环境
docker-compose -f docker-compose.yml logs -f server
```

### 5. 数据库初始化失败

确保 SQL 脚本已正确挂载：

```bash
ls -la /opt/app/create_company_schema_function.sql
```

### 6. 健康检查一直不通过

等待应用启动完成（默认 60s 宽限期）：

```bash
# 查看容器状态
docker ps

# 查看详细健康信息
docker inspect --format='{{json .State.Health}}' im-server-cicd | jq
```

## 备份与恢复

### 数据备份

```bash
# 备份 PostgreSQL 数据
docker exec postgres-db-prod pg_dump -U postgres group > backup_$(date +%Y%m%d).sql

# 备份 Redis 数据
docker exec redis-cache-prod redis-cli BGSAVE
cp /opt/app/redis_data/dump.rdb backup_$(date +%Y%m%d).rdb
```

### 数据恢复

```bash
# 恢复 PostgreSQL
cat backup_20240101.sql | docker exec -i postgres-db-prod psql -U postgres group

# 恢复 Redis
cp backup_20240101.rdb /opt/app/redis_data/dump.rdb
docker restart redis-cache-prod
```

## 监控建议

### 1. 资源监控

```bash
# 查看容器资源使用
docker stats
```

### 2. 日志轮转

配置 `/etc/docker/daemon.json`:

```json
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

### 3. 健康告警

添加监控脚本 `/opt/app/monitor.sh`:

```bash
#!/bin/bash
HEALTH=$(docker inspect --format='{{.State.Health.Status}}' im-server-cicd)
if [ "$HEALTH" != "healthy" ]; then
    echo "警告：服务不健康！" | mail -s "IM Server Alert" admin@example.com
fi
```

添加到 crontab:

```bash
*/5 * * * * /opt/app/monitor.sh
```

## 安全建议

1. **修改默认密码**：特别是数据库、Redis、管理员密码
2. **启用 HTTPS**：配置 SSL 证书
3. **防火墙配置**：只开放必要端口
4. **定期更新**：及时应用安全补丁
5. **限制访问**：使用白名单限制管理端点访问

## 性能优化

### 1. JVM参数调整

在 `docker-compose.cicd.yml` 中修改：

```yaml
environment:
  - JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### 2. 数据库连接池

```yaml
environment:
  - SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=30
  - SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
```

### 3. Redis 优化

```yaml
command: redis-server --appendonly yes --maxmemory 2gb --maxmemory-policy allkeys-lru
```

## 升级指南

### 版本升级

```bash
# 1. 备份数据
bash deploy-production.sh backup

# 2. 停止服务
bash deploy-production.sh stop

# 3. 更新配置
git pull origin main

# 4. 拉取新镜像
docker pull pengpeng163/groupim:v2.0.0

# 5. 启动服务
bash deploy-production.sh start
```

### 回滚

```bash
# 1. 停止当前版本
docker compose down

# 2. 使用旧版本镜像
docker pull pengpeng163/groupim:v1.0.0

# 3. 启动旧版本
docker compose up -d
```

## 联系支持

如有问题，请提交 Issue 或联系运维团队。
