# ==========================================
# 第一阶段：构建 Java 项目
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# 复制父级 pom.xml
COPY pom.xml .

# 复制关键模块源码
COPY common/ common/
COPY entity/ entity/
COPY server/ server/

# 构建 server 模块（同时构建依赖模块）
RUN mvn -B clean package -pl server -am -DskipTests


# ==========================================
# 第二阶段：运行时镜像
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从构建阶段复制 jar
COPY --from=build /build/server/target/*.jar /app/app.jar

# 创建运行用户
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

# 创建应用目录并授权
RUN mkdir -p /app/logs /app/storage && \
    chown -R spring:spring /app

# 切换为非 root 用户
USER spring

# 暴露端口
# 8080 -> HTTP API
# 8088 -> Netty TCP/WebSocket
EXPOSE 8080 8088

# 启动 SpringBoot
ENTRYPOINT ["java","-jar","/app/app.jar"]