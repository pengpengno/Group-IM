FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /build

# 复制整个项目结构
COPY pom.xml .
COPY common/ common/
COPY entity/ entity/
COPY server/ server/
COPY gui/ gui/

# 预下载依赖
RUN mvn dependency:go-offline -q -pl server

# 构建server模块
RUN mvn clean package -DskipTests -q -pl server -am

# 第二阶段：运行时镜像
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 从构建阶段复制JAR文件
COPY --from=build /build/server/target/*.jar app.jar

# 创建应用用户
RUN addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring
USER 1001:1001

# 暴露端口
EXPOSE 8080 8088

# 创建应用目录
RUN mkdir -p /app/logs /app/storage

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]