# IM Group Server Kubernetes 部署指南

本指南将帮助您将 IM Group Server 部署到 Kubernetes 集群。

## 环境要求

- Kubernetes 1.20+
- kubectl 命令行工具
- k3s 或其他兼容的 Kubernetes 发行版
- 至少 8GB 可用内存
- 至少 15GB 可用磁盘空间

## 集群准备

### 1. 验证集群连接

```bash
kubectl cluster-info
```

### 2. 验证节点状态

```bash
kubectl get nodes
```

## 部署架构

部署包含以下组件：

- **PostgreSQL**: StatefulSet (数据持久化)
- **Redis**: StatefulSet (数据持久化)  
- **OpenLDAP**: StatefulSet (数据持久化)
- **phpLDAPadmin**: Deployment (管理界面)
- **IM Server**: Deployment (主应用服务)
- **Nginx**: Deployment (反向代理)

## 部署步骤

### 1. 使用部署脚本（推荐）

**Linux/macOS:**
```bash
chmod +x scripts/deploy-k8s.sh
./scripts/deploy-k8s.sh
```

**Windows:**
```powershell
.\scripts\deploy-k8s.ps1
```

### 2. 手动部署

依次应用各个配置文件：

```bash
# 1. 部署数据库和缓存服务
kubectl apply -f k8s/k8s-postgres-statefulset.yaml
kubectl apply -f k8s/k8s-redis-statefulset.yaml
kubectl apply -f k8s/k8s-openldap-statefulset.yaml

# 2. 等待基础服务启动
kubectl rollout status statefulset/postgres-db --timeout=120s
kubectl rollout status statefulset/redis-cache --timeout=120s
kubectl rollout status statefulset/openldap --timeout=120s

# 3. 部署应用服务
kubectl apply -f k8s/k8s-phpldapadmin-deployment.yaml
kubectl apply -f k8s/k8s-im-server-deployment.yaml
kubectl apply -f k8s/k8s-nginx-ingress.yaml

# 4. 等待应用服务启动
kubectl rollout status deployment/im-server --timeout=180s
kubectl rollout status deployment/nginx-proxy --timeout=120s
```

## 服务组件

### 有状态服务 (StatefulSet)

1. **PostgreSQL**
   - Service: `postgres-service:5432`
   - 持久卷: `postgres-data`
   - 初始化SQL: 通过ConfigMap挂载

2. **Redis**
   - Service: `redis-service:6379`
   - 持久卷: `redis-data`
   - AOF持久化: 已启用

3. **OpenLDAP**
   - Service: `openldap-service:389,636`
   - 持久卷: `ldap-data`, `ldap-config`
   - 初始化LDIF: 通过ConfigMap挂载

### 无状态服务 (Deployment)

1. **phpLDAPadmin**
   - Service: `phpldapadmin-service:8085` (NodePort)

2. **IM Server**
   - Service: `im-server-service:8080,8088`
   - 环境变量: 通过Deployment配置
   - 持久卷: 日志和存储卷

3. **Nginx**
   - Service: `nginx-service:80,443,8088` (LoadBalancer)
   - HTTP/HTTPS代理: 80/443端口
   - TCP代理: 8088端口

## 访问服务

部署完成后，可以通过以下地址访问服务：

```bash
# 获取服务IP
kubectl get svc nginx-service
kubectl get svc phpldapadmin-service
```

- **应用服务 (HTTP)**: `LOAD_BALANCER_IP:80`
- **应用服务 (HTTPS)**: `LOAD_BALANCER_IP:443`
- **TCP 长连接服务**: `LOAD_BALANCER_IP:8088`
- **LDAP管理**: `NODEPORT_IP:8085`

## 健康检查

所有服务都配置了健康检查探针：

- **PostgreSQL**: 使用 `pg_isready` 命令
- **Redis**: 使用 `redis-cli ping` 命令
- **OpenLDAP**: 使用 `ldapsearch` 命令
- **IM Server**: HTTP `/actuator/health` 端点
- **Nginx**: HTTP `/` 端点

## 存储管理

### 持久卷声明 (PVC)

- `postgres-data`: PostgreSQL 数据存储
- `redis-data`: Redis 数据存储
- `im-server-logs-pvc`: 应用日志存储
- `im-server-storage-pvc`: 文件存储

### 配置管理

- ConfigMap 存储初始化脚本和配置文件
- 环境变量通过 Deployment 配置

## 监控和日志

### 查看Pod状态

```bash
kubectl get pods
```

### 查看服务状态

```bash
kubectl get services
```

### 查看日志

```bash
# IM Server日志
kubectl logs -f deployment/im-server

# Nginx日志
kubectl logs -f deployment/nginx-proxy

# PostgreSQL日志
kubectl logs -f statefulset/postgres-db

# Redis日志
kubectl logs -f statefulset/redis-cache
```

## 扩容和缩容

### 扩容IM Server

```bash
kubectl scale deployment/im-server --replicas=3
```

### 缩容服务

```bash
kubectl scale deployment/im-server --replicas=1
```

## 故障排除

### 检查Pod状态

```bash
kubectl describe pod <pod-name>
```

### 检查事件

```bash
kubectl get events --sort-by=.metadata.creationTimestamp
```

### 进入容器调试

```bash
kubectl exec -it <pod-name> -- bash
```

## 清理部署

### 删除所有资源

```bash
kubectl delete -f k8s/k8s-postgres-statefulset.yaml
kubectl delete -f k8s/k8s-redis-statefulset.yaml
kubectl delete -f k8s/k8s-openldap-statefulset.yaml
kubectl delete -f k8s/k8s-phpldapadmin-deployment.yaml
kubectl delete -f k8s/k8s-im-server-deployment.yaml
kubectl delete -f k8s/k8s-nginx-ingress.yaml
```

### 删除持久卷（谨慎操作）

```bash
kubectl delete pvc postgres-data-im-server-0
kubectl delete pvc redis-data-im-server-0
kubectl delete pvc im-server-logs-pvc
kubectl delete pvc im-server-storage-pvc
```

## 生产环境建议

1. **安全配置**
   - 使用Secret管理敏感信息
   - 配置RBAC权限
   - 启用网络策略

2. **监控配置**
   - 集成Prometheus监控
   - 配置日志收集系统
   - 设置告警规则

3. **备份策略**
   - 定期备份PostgreSQL数据
   - 备份Redis RDB/AOF文件
   - 配置自动备份计划

4. **性能优化**
   - 配置资源限制和请求
   - 调整副本数量
   - 优化存储性能

## 联系支持

如有问题，请联系开发团队或参考项目文档。