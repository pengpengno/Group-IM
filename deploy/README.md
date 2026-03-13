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

### 1. 首次部署（生产环境）

在目标服务器上以 `deploy` 用户执行：

```bash
# 下载并执行部署脚本
curl -sL https://raw.githubusercontent.com/your-repo/main/deploy/scripts/deploy-production.sh | bash -s -- install
```

或手动执行：

```bash
cd /opt/app
bash deploy-production.sh install
```

### 2. 更新部署

```bash
bash deploy-production.sh update
```

### 3. 服务管理

```bash
# 查看状态
bash deploy-production.sh status

# 查看日志
bash deploy-production.sh logs

# 重启服务
bash deploy-production.sh restart

# 停止服务
bash deploy-production.sh stop
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

### docker-compose.cicd.yml

包含以下服务：
- **postgres**: PostgreSQL 数据库
- **redis**: Redis 缓存
- **server**: Spring Boot 应用服务
- **nginx**: Nginx 反向代理

### nginx.conf

- 端口 80: HTTP 反向代理
- 端口 443: HTTPS (需配置 SSL 证书)
- 健康检查端点：`/actuator/health/liveness`

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

### 1. 端口冲突

如果端口被占用，修改 `.env` 文件：

```bash
SERVER_PORT=8081  # 改为其他端口
```

### 2. 磁盘空间不足

清理旧镜像：

```bash
docker image prune -a
```

### 3. 服务启动失败

查看日志：

```bash
docker compose logs -f server
```

### 4. 数据库初始化失败

确保 SQL 脚本已正确挂载：

```bash
ls -la /opt/groupim/scripts/create_company_schema_function.sql
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
