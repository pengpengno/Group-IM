# ==========================================
# 第一阶段：构建 Java 项目
# ==========================================
FROM maven:3.9.14-sapmachine-21 AS build

WORKDIR /build

# 复制父级 pom.xml
COPY pom.xml .
COPY alisettings.xml .

# 复制关键模块源码
COPY common/ common/
COPY entity/ entity/
COPY server/ server/

# 构建 server 模块（同时构建依赖模块）
# RUN mvn -B clean package  -DskipTests -s alisettings.xml
RUN mvn -B clean package  -DskipTests


# ==========================================
# 第二阶段：运行时镜像
# ==========================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 从构建阶段复制 jar
COPY --from=build /build/server/target/*.jar /app/server.jar

# 创建运行用户
RUN groupadd -g 1001 spring && \
    useradd -u 1001 -g spring -m -s /bin/bash spring

# 创建应用目录并授权

RUN mkdir -p /app/logs/server && \
    mkdir -p /app/logs/server/app && \
    mkdir -p /app/storage && \
    chown -R spring:spring /app && \
    chmod -R 755 /app

# 切换为非 root 用户
USER spring

# 暴露端口
# 8080 -> HTTP API
# 8088 -> Netty TCP/WebSocket
EXPOSE 8080 8088

# 设置环境变量，默认使用 host.docker.internal 访问宿主机服务
ENV DB_IP=host.docker.internal
ENV REDIS_HOST=host.docker.internal

# 健康检查配置
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health/liveness || exit 1

# 启动 SpringBoot
ENTRYPOINT ["java","-jar","/app/server.jar"]