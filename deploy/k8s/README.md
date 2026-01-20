# Kubernetes 部署文件

此目录包含将应用部署到 Kubernetes 集群所需的所有 YAML 文件。

## 文件列表

- `k8s-postgres-statefulset.yaml` - PostgreSQL 数据库服务
- `k8s-redis-statefulset.yaml` - Redis 缓存服务  
- `k8s-openldap-statefulset.yaml` - OpenLDAP 目录服务
- `k8s-phpldapadmin-deployment.yaml` - phpLDAPadmin 管理界面
- `k8s-im-server-deployment.yaml` - IM 服务器
- `k8s-nginx-ingress.yaml` - Nginx 反向代理

## 部署说明

请参阅根目录的 K8S-DEPLOYMENT.md 文件获取详细部署指南。