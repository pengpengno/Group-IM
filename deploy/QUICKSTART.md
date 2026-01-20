# IM Group Server 快速启动指南

## 一键部署（Docker Compose）


###  Installation
下载k3s
```
curl -sfL https://get.k3s.io | sh -
```
### 开发环境快速启动
```bash
# Linux/macOS
./deploy/scripts/deploy.sh

# Windows
deploy\scripts\deploy.bat
```

### 生产环境快速启动
```bash
# Linux/macOS (JVM版本)
./deploy/scripts/deploy.sh prod

# Linux/macOS (GraalVM Native版本)
./deploy/scripts/deploy.sh prod-native

# Windows (JVM版本)
deploy\scripts\deploy.bat prod

# Windows (GraalVM Native版本)
deploy\scripts\deploy.bat prod-native
```

## 一键部署（Kubernetes - 简化版，推荐用于k3s单节点）

### Linux/macOS
```bash
chmod +x deploy/scripts/deploy-k8s-simple.sh
./deploy/scripts/deploy-k8s-simple.sh
```

### Windows
```powershell
.\deploy\scripts\deploy-k8s-simple.ps1
```

## 一键部署（Kubernetes - 企业版，适用于多节点集群）

### Linux/macOS
```bash
chmod +x deploy/scripts/deploy-k8s.sh
./deploy/scripts/deploy-k8s.sh
```

### Windows
```powershell
.\deploy\scripts\deploy-k8s.ps1
```

## 推荐部署方式

对于 **k3s 单节点环境**（个人/小团队）：
- 使用简化版 Kubernetes 部署：`deploy-k8s-simple.sh`
- 或使用 Docker Compose：`deploy.sh`

对于 **多节点生产环境**：
- 使用完整版 Kubernetes 部署：`deploy-k8s.sh`

## 访问服务

部署完成后，服务将在以下地址可用：

- **应用服务 (HTTP)**: http://localhost:8080
- **TCP 长连接服务**: localhost:8088
- **HTTPS 服务**: https://localhost:443
- **LDAP管理**: http://localhost:8085

## 停止服务

### Docker Compose
```bash
# Linux/macOS
cd deploy/docker && docker-compose down

# Windows
cd deploy\docker && docker-compose down
```

### Kubernetes (简化版)
```bash
# 删除所有部署
kubectl delete -f deploy/k8s-simple/simple-all-in-one.yaml
```

### Kubernetes (完整版)
```bash
# 使用清理脚本
kubectl delete -f deploy/k8s/
```

## 查看日志

### Docker Compose
```bash
# 查看所有服务日志
cd deploy/docker && docker-compose logs -f

# 查看特定服务日志
cd deploy/docker && docker-compose logs -f server
```

### Kubernetes
```bash
# 查看特定部署的日志
kubectl logs -f deployment/im-server
kubectl logs -f deployment/nginx-proxy
```

## 常见问题

### 1. 端口被占用
- 确保端口 8080, 8088, 80, 443, 5432, 6379, 389, 8085 未被其他服务占用

### 2. 内存不足
- 确保系统有至少 4GB 可用内存

### 3. 权限问题
- Linux/macOS: 确保脚本有执行权限 (`chmod +x deploy/scripts/*.sh`)