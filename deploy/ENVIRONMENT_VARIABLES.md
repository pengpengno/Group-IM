# 环境变量配置说明

本文档用于说明通过环境变量配置应用的所有参数，适用于 Docker、Docker Compose、Kubernetes 等容器化部署场景。

## 快速开始

### Docker 运行示例

```bash
docker run -d \
  --name group-server \
  -p 8080:8080 \
  -p 8088:8088 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/group \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=postgres \
  -e SPRING_DATA_REDIS_HOST=redis \
  -e REDIS_HOST=redis \
  -e SYSTEM_INITIALIZER_ENABLED=true \
  -e ADMIN_PASSWORD=12345 \
  -v group-logs:/app/logs \
  -v group-storage:/app/storage \
  group-im:latest
```

### Docker Compose 示例

```yaml
version: '3.8'

services:
  server:
    image: group-im:latest
    ports:
      - "8080:8080"
      - "8088:8088"
    environment:
      # 数据库配置
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/group
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      
      # Redis 配置
      - SPRING_DATA_REDIS_HOST=redis
      - REDIS_HOST=redis
      
      # LDAP 配置
      - SPRING_LDAP_URLS=ldap://openldap:389
      - SPRING_LDAP_USERNAME=cn=admin,dc=mycompany,dc=com
      - SPRING_LDAP_PASSWORD=admin
      
      # 系统初始化配置
      - SYSTEM_INITIALIZER_ENABLED=true
      - DEFAULT_COMPANY_NAME=public
      - DEFAULT_COMPANY_SCHEMA=public
      - ADMIN_USERNAME=admin
      - ADMIN_EMAIL=admin@example.com
      - ADMIN_PASSWORD=12345
      
      # AI 配置（可选）
      - SPRING_AI_ENABLED=false
      - GROQ_API_KEY=your-api-key
      - OPENAI_API_KEY=your-api-key
    volumes:
      - ./logs:/app/logs
      - ./storage:/app/storage
    depends_on:
      - postgres
      - redis
      - openldap

  postgres:
    image: postgres:14
    environment:
      - POSTGRES_DB=group
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres

  redis:
    image: redis:7-alpine

  openldap:
    image: osixia/openldap:1.5.0
    environment:
      - LDAP_ORGANISATION=MyCompany
      - LDAP_DOMAIN=mycompany.com
      - LDAP_ADMIN_PASSWORD=admin
```

## 完整环境变量列表

### 1. 数据库配置 (Spring DataSource)

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `SPRING_DATASOURCE_URL` | JDBC 连接 URL | `jdbc:postgresql://localhost:5432/group` | `jdbc:postgresql://postgres:5432/group` |
| `SPRING_DATASOURCE_USERNAME` | 数据库用户名 | `postgres` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | 数据库密码 | `postgres` | `your_password` |

### 2. Redis 配置

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `SPRING_DATA_REDIS_HOST` | Redis 主机地址 | `localhost` | `redis` |
| `SPRING_DATA_REDIS_PORT` | Redis 端口 | `6379` | `6379` |
| `REDIS_HOST` | Redis 主机地址（简化配置） | `localhost` | `redis` |
| `REDIS_PORT` | Redis 端口（简化配置） | `6379` | `6379` |

### 3. LDAP 配置

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `SPRING_LDAP_URLS` | LDAP 服务器 URL | `ldap://localhost:389` | `ldap://openldap:389` |
| `SPRING_LDAP_USERNAME` | LDAP 管理员 DN | `cn=admin,dc=mycompany,dc=com` | `cn=admin,dc=mycompany,dc=com` |
| `SPRING_LDAP_PASSWORD` | LDAP 管理员密码 | `admin` | `admin` |
| `SPRING_LDAP_BASE` | LDAP 基础 DN | `dc=mycompany,dc=com` | `dc=mycompany,dc=com` |

### 4. 系统初始化配置 (Group System Initializer)

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `SYSTEM_INITIALIZER_ENABLED` | 是否启用系统初始化 | `true` | `true` |
| `DEFAULT_COMPANY_NAME` | 默认公司名称 | `public` | `public` |
| `DEFAULT_COMPANY_SCHEMA` | 默认公司 Schema 名称 | `public` | `public` |
| `DEFAULT_COMPANY_ACTIVE` | 默认公司是否激活 | `true` | `true` |
| `ADMIN_USERNAME` | 管理员用户名 | `admin` | `admin` |
| `ADMIN_EMAIL` | 管理员邮箱 | `admin@example.com` | `admin@example.com` |
| `ADMIN_PHONE_NUMBER` | 管理员电话号码 | `1234567890` | `1234567890` |
| `ADMIN_PASSWORD` | 管理员密码 | `12345` | `your_secure_password` |

### 5. AI 配置 (Group AI)

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `SPRING_AI_ENABLED` | 是否启用 AI 功能 | `false` | `false` |
| `GROQ_API_KEY` | Groq API 密钥 | - | `gsk_xxxxx` |
| `GROQ_BASE_URL` | Groq API 基础 URL | `https://api.groq.com/openai/v1` | `https://api.groq.com/openai/v1` |
| `OPENAI_API_KEY` | OpenAI API 密钥 | - | `sk-xxxxx` |
| `OPENAI_BASE_URL` | OpenAI API 基础 URL | `https://api.openai.com/v1` | `https://api.openai.com/v1` |

### 6. 文件存储配置 (Group File Upload)

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `GROUP_STORAGE_PATH` | 文件存储路径 | `/app/storage` | `/app/storage` |
| `GROUP_FILE_UPLOAD_CHUNK_TEMP_PATH` | 分片上传临时路径 | `/tmp/chunk-storage` | `/tmp/chunk-storage` |
| `GROUP_FILE_UPLOAD_BASE_PATH` | 文件上传基础路径 | `uploads` | `uploads` |

### 7. WebRTC 配置

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `WEBRTC_SESSION_TIMEOUT` | WebRTC 会话超时时间 (ms) | `300000` | `300000` |

### 8. 服务器配置

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `SERVER_PORT` | HTTP 服务端口 | `8080` | `8080` |
| `TCP_PORT` | TCP 服务端口 | `8088` | `8088` |

### 9. 日志配置

| 环境变量名 | 说明 | 默认值 | 示例 |
|-----------|------|--------|------|
| `LOGGING_LEVEL_ROOT` | 根日志级别 | `INFO` | `INFO` |
| `LOGGING_LEVEL_COM_GITHUB_IM` | 应用日志级别 | `DEBUG` | `DEBUG` |
| `LOGGING_FILE_NAME` | 日志文件路径 | `/app/logs/application.log` | `/app/logs/application.log` |

## Kubernetes Deployment 示例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: group-server
  labels:
    app: group-server
spec:
  replicas: 2
  selector:
    matchLabels:
      app: group-server
  template:
    metadata:
      labels:
        app: group-server
    spec:
      containers:
      - name: group-server
        image: group-im:latest
        ports:
        - containerPort: 8080
          name: http
        - containerPort: 8088
          name: tcp
        env:
        # 数据库配置
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service:5432/group"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        
        # Redis 配置
        - name: SPRING_DATA_REDIS_HOST
          value: "redis-service"
        - name: REDIS_HOST
          value: "redis-service"
        
        # LDAP 配置
        - name: SPRING_LDAP_URLS
          value: "ldap://openldap-service:389"
        - name: SPRING_LDAP_USERNAME
          value: "cn=admin,dc=mycompany,dc=com"
        - name: SPRING_LDAP_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ldap-secret
              key: password
        
        # 系统初始化配置
        - name: SYSTEM_INITIALIZER_ENABLED
          value: "true"
        - name: DEFAULT_COMPANY_NAME
          value: "public"
        - name: ADMIN_USERNAME
          value: "admin"
        - name: ADMIN_PASSWORD
          valueFrom:
            secretKeyRef:
              name: admin-secret
              key: password
        
        # AI 配置（使用 Secret）
        - name: GROQ_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secret
              key: groq-key
        - name: OPENAI_API_KEY
          valueFrom:
            secretKeyRef:
              name: ai-secret
              key: openai-key
        
        # 资源限制
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "2Gi"
            cpu: "1000m"
        
        # 健康检查
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        
        # 挂载卷
        volumeMounts:
        - name: logs-volume
          mountPath: /app/logs
        - name: storage-volume
          mountPath: /app/storage
      
      volumes:
      - name: logs-volume
        persistentVolumeClaim:
          claimName: logs-pvc
      - name: storage-volume
        persistentVolumeClaim:
          claimName: storage-pvc
```

## 安全建议

### 1. 敏感信息使用 Secret

**不要硬编码密码！** 使用 Docker Secret 或 Kubernetes Secret 管理敏感信息：

```bash
# Docker Swarm
echo "mysecretpassword" | docker secret create db_password -

# Kubernetes
kubectl create secret generic db-secret \
  --from-literal=username=postgres \
  --from-literal=password=mysecretpassword
```

### 2. 环境变量文件 (.env)

开发环境可以使用 `.env` 文件：

```bash
# .env 文件示例
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/group
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
SPRING_DATA_REDIS_HOST=localhost
SYSTEM_INITIALIZER_ENABLED=true
ADMIN_USERNAME=admin
ADMIN_PASSWORD=12345
GROQ_API_KEY=gsk_xxxxx
OPENAI_API_KEY=sk-xxxxx
```

在 Docker Compose 中使用：

```yaml
version: '3.8'

services:
  server:
    image: group-im:latest
    env_file:
      - .env
    ports:
      - "8080:8080"
```

### 3. 生产环境推荐配置

```yaml
# production.env - 生产环境示例
# 数据库配置
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db:5432/group
SPRING_DATASOURCE_USERNAME=group_app_user
SPRING_DATASOURCE_PASSWORD=Str0ng_P@ssw0rd!

# Redis 配置
SPRING_DATA_REDIS_HOST=prod-redis
REDIS_HOST=prod-redis

# LDAP 配置
SPRING_LDAP_URLS=ldap://prod-ldap:389
SPRING_LDAP_PASSWORD=Str0ng_LDAP_P@ss!

# 系统初始化（生产环境通常关闭自动初始化）
SYSTEM_INITIALIZER_ENABLED=false

# AI 配置
SPRING_AI_ENABLED=true
GROQ_API_KEY=gsk_production_key_xxxxx
OPENAI_API_KEY=sk-prod-xxxxx

# 日志配置
LOGGING_LEVEL_ROOT=WARN
LOGGING_LEVEL_COM_GITHUB_IM=INFO
```

## 故障排查

### 查看容器环境变量

```bash
# 查看运行中容器的环境变量
docker exec <container-id> printenv

# 或者
docker inspect <container-id> | grep -A 20 Env
```

### 测试环境变量注入

```bash
# 启动容器并测试配置
docker run --rm -it \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://test:5432/test \
  group-im:latest \
  java -jar /app/server.jar --debug
```

### 常见问题

1. **环境变量不生效**
   - 检查环境变量名是否正确
   - 确认 Spring Boot 版本支持环境变量覆盖
   - 查看 application.yml 中的占位符格式

2. **特殊字符处理**
   - 密码中包含特殊字符时需要转义
   - 建议使用引号包裹复杂值

3. **配置优先级**
   - 命令行参数 > 环境变量 > application.yml > 默认值

## 配置检查清单

部署前请确认：

- [ ] 数据库连接配置正确
- [ ] Redis 连接配置正确
- [ ] LDAP 连接配置正确（如使用）
- [ ] 管理员密码已修改为强密码
- [ ] 文件存储路径有足够空间
- [ ] 日志目录有写权限
- [ ] AI API Key 已配置（如使用）
- [ ] 系统初始化开关状态正确
- [ ] 端口映射配置正确
- [ ] 资源限制已设置

## 相关文档

- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Docker 环境变量](https://docs.docker.com/engine/reference/builder/#env)
- [Kubernetes ConfigMap](https://kubernetes.io/docs/concepts/configuration/configmap/)
- [Kubernetes Secret](https://kubernetes.io/docs/concepts/configuration/secret/)
