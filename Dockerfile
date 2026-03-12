FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# 复制父级 pom.xml
COPY pom.xml .

# 复制所有的关键子模块源码 (gui可以省略，因为服务端不需要且pom中已注释)
COPY common/ common/
COPY entity/ entity/
COPY server/ server/

# 编译并打包 server 模块（-am 表示同时也构建 server 依赖的其他本工程模块，如 common 和 entity）
# 这里去掉了 go-offline，因为多模块间强依赖时 go-offline 经常会报错找不到 common 包
RUN mvn clean package -pl server -am -DskipTests

# ==========================================
# 第二阶段：构建精简的运行时镜像
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从构建阶段提取打好的 jar 包
COPY --from=build /build/server/target/*.jar app.jar

# 创建非 root 用户运行，提升安全性
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring
USER 1001:1001

# 暴露供外部访问的端口
# 8080是Web API端口, 8088是Netty TCP/WebSocket长连接端口
EXPOSE 8080 8088

# 创建应用可能需要的外部挂载目录
RUN mkdir -p /app/logs /app/storage

# 启动 SpringBoot
ENTRYPOINT ["java", "-jar", "app.jar"]
