# Dockerfiles 目录

此目录包含项目的 Docker 配置文件。

## 文件列表

- `Dockerfile` - 用于构建 JVM 版本的 Docker 镜像
- `Dockerfile.native` - 用于构建 GraalVM Native 版本的 Docker 镜像

## 用途

这些 Dockerfile 用于构建 IM Group Server 的容器镜像，支持两种部署模式：

1. **JVM 版本** - 传统的基于 JVM 的部署方式
2. **Native 版本** - 使用 GraalVM 构建的原生镜像，启动更快，内存占用更少

## 构建说明

### 构建 JVM 版本
```bash
# 在项目根目录执行
docker build -f deploy/dockerfiles/Dockerfile -t im-server:jvm .
```

### 构建 Native 版本
```bash
# 在项目根目录执行
docker build -f deploy/dockerfiles/Dockerfile.native -t im-server:native .
```