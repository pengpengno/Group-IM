# Group IM

一个跨平台的即时通讯应用，支持文字、语音、视频通话等多种通信方式。

## 🚀 特性

- 💬 实时消息传递
- 📱 跨平台支持 (Android, Desktop, Web)
- 🎵 语音消息
- 📹 视频通话
- 📁 文件共享
- 👥 群聊功能

## 🛠 技术栈

### 后端
- Spring Boot
- WebSocket 長连接
- JPA/Hibernate
- PostgreSQL

### 前端 (Kotlin Multiplatform)
- Kotlin Multiplatform Mobile (KMM)
- Jetpack Compose Multiplatform
- 共享业务逻辑和UI组件

#### 平台实现
- Android (Kotlin + Compose)
- Desktop (Kotlin + Compose Multiplatform)
- iOS (未来支持) 
- Web (未来支持)

### 音视频
- WebRTC
- STUN/TURN 服务器支持

## 🏗 项目结构

```
├── server/          # 后端服务
├── common/          # 公共代码
├── entity/          # 实体定义
├── Group-app/       # KMP 客户端应用
├── gui/            # JavaFX 桌面客户端
└── doc/            # 文档
```

## 🚀 快速开始

### 后端启动

```bash
# 启动数据库
docker-compose up -d

# 启动后端服务
./mvnw spring-boot:run -pl server
```

### 客户端启动

```bash
# Android
./gradlew :Group-app:composeApp:installDebug

# Desktop
./gradlew :Group-app:composeApp:run

# iOS (未来支持)
./gradlew :Group-app:composeApp:iosSimulatorArm64DebugTest

# Web (未来支持)
./gradlew :Group-app:composeApp:jsBrowserDevelopmentRun
```

## 📱 平台支持

| 平台 | 状态 | 备注 |
|------|------|------|
| Android | ✅ | 已支持 |
| iOS | 🚧 | 计划中 |
| Windows | ✅ | JavaFX |
| macOS | ✅ | JavaFX |
| Linux | ✅ | JavaFX |
| Web | 🚧 | 计划中 |

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License