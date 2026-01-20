# 简化版 Kubernetes 部署文件

此目录包含适用于 k3s 单节点环境的简化版 Kubernetes 部署文件。

## 设计理念

此部署方案专为个人/小团队的 k3s 单节点环境设计，特点：

- 使用 Deployment 替代 StatefulSet（单节点无需状态集复杂性）
- 使用 emptyDir 存储（适用于开发/测试环境）
- 使用 NodePort 服务（适用于单节点环境）
- 单个 YAML 文件部署（减少复杂性）
- 类似 docker-compose 的简单体验

## 文件列表

- `simple-all-in-one.yaml` - 包含所有服务的单一部署文件

## 适用场景

- k3s 单节点部署
- 个人/小团队开发测试
- 资源受限环境
- 快速原型验证

## 不适用场景

- 多节点生产环境
- 需要高可用性的场景
- 需要持久化存储的场景
- 需要复杂网络策略的场景

## 部署说明

使用以下命令部署：

```bash
# 应用部署
kubectl apply -f simple-all-in-one.yaml

# 检查状态
kubectl get pods
kubectl get services

# 删除部署
kubectl delete -f simple-all-in-one.yaml
```

如需更高级的功能（持久化存储、多节点部署、高可用等），请使用 `../k8s/` 目录中的完整版部署文件。