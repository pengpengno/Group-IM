# 跨平台架构设计文档

## 1. 项目概述

本项目采用 Kotlin Multiplatform Mobile (KMM) 技术栈，实现跨平台移动应用开发。项目支持 Android、iOS、Desktop (JVM) 和 Web (WASM/JS) 平台。

## 2. 目录结构

```
composeApp/
├── src/
│   ├── commonMain/                 # 共享代码
│   │   └── kotlin/
│   │       └── com/github/im/group/sdk/
│   │           ├── WebRTC.kt          # WebRTC 接口定义
│   │           ├── VoiceRecorder.kt   # 录音接口定义
│   │           ├── FilePicker.kt      # 文件选择接口定义
│   │           ├── NetworkManager.kt  # 网络管理接口定义
│   │           ├── DataStorage.kt     # 数据存储接口定义
│   │           ├── UserManager.kt     # 用户管理接口定义
│   │           └── PlatformServices.kt # 平台服务工厂
│   ├── androidMain/               # Android 平台特定实现
│   │   └── kotlin/
│   │       └── com/github/im/group/sdk/
│   ├── iosMain/                   # iOS 平台特定实现
│   │   └── kotlin/
│   │       └── com/github/im/group/sdk/
│   ├── desktopMain/               # Desktop 平台特定实现
│   │   └── kotlin/
│   │       └── com/github/im/group/sdk/
│   └── jsMain/                    # Web 平台特定实现
│       └── kotlin/
│           └── com/github/im/group/sdk/
```

## 3. 跨平台模块划分

### 3.1 音视频模块 (WebRTC)
- **Common**: 定义 [WebRTCManager](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/WebRTC.kt#L52-L147) 接口和相关数据类
- **Android**: 使用 `webrtc-kmp` 库实现
- **iOS**: 使用原生 WebRTC 框架实现
- **Desktop**: 提供占位实现（WebRTC 不支持桌面）
- **Web**: 使用浏览器 WebRTC API 实现

### 3.2 录音模块 (Voice Recording)
- **Common**: 定义 [VoiceRecorder](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/VoiceRecorder.kt#L3-L24) 接口
- **Android**: 使用 Android MediaRecorder 实现
- **iOS**: 使用 AVAudioRecorder 实现
- **Desktop**: 使用 Java Sound API 实现
- **Web**: 使用 Web Audio API 实现

### 3.3 网络通信模块
- **Common**: 定义 [NetworkManager](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/DataStorage.kt#L145-L170) 和 [WebSocketManager](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/DataStorage.kt#L173-L185) 接口
- **Android**: 使用 OkHttp 实现
- **iOS**: 使用 NSURLSession 实现
- **Desktop**: 使用 Java 11+ HTTP Client 实现
- **Web**: 使用 Fetch API 和 WebSocket 实现

### 3.4 数据存储模块
- **Common**: 定义 [DataStorage](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/DataStorage.kt#L15-L87), [DatabaseManager](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/DataStorage.kt#L90-L107), [FileStorage](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/DataStorage.kt#L153-L170) 接口
- **Android**: 使用 SharedPreferences, SQLite, 文件系统实现
- **iOS**: 使用 UserDefaults, Core Data, 文件系统实现
- **Desktop**: 使用 Preferences, SQLite, 文件系统实现
- **Web**: 使用 localStorage, IndexedDB, FileSystem Access API 实现

### 3.5 用户信息管理模块
- **Common**: 定义 [UserManager](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/UserManager.kt#L15-L51) 接口和相关数据类
- **各平台**: 实现用户认证、偏好设置等功能

## 4. 跨平台服务访问模式

通过 [PlatformServices](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/PlatformServices.kt#L7-L17) 接口提供统一的访问入口：

```kotlin
// 在共享代码中使用
val services = getPlatformServices()
val webRTCManager = services.getWebRTCManager()
val userManager = services.getUserManager()
```

## 5. 实现状态

### 已实现
- ✅ 接口定义 (Common)
- ✅ Android 平台实现
- ✅ iOS 平台实现
- ✅ Desktop 平台实现
- ✅ Web 平台实现

### 待完善
- 🔄 具体功能实现（当前主要是架子代码）
- 🔄 错误处理和异常管理
- 🔄 性能优化
- 🔄 测试覆盖

## 6. 平台特定注意事项

### Android
- 需要处理权限请求
- 使用 AndroidX 库
- 适配不同的 Android 版本

### iOS
- 需要配置 Info.plist 权限
- 使用 Kotlin Native 互操作
- 遵循苹果开发者指南

### Desktop
- 使用 JVM 标准库
- 考虑不同操作系统的差异
- 处理桌面应用的生命周期

### Web
- 使用 Kotlin/JS 编译
- 处理浏览器兼容性
- 考虑安全性限制

## 7. 开发指南

### 添加新功能
1. 在 `commonMain` 中定义接口和数据类
2. 在各平台的 `*Main` 目录中实现具体功能
3. 通过 [getPlatformServices()](file:///D:/ideaproject/pengpeng/Group/Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/sdk/PlatformServices.kt#L20-L21) 访问平台特定实现

### 测试策略
- 共享逻辑单元测试在 `commonTest`
- 平台特定功能测试在各平台的 `*Test` 目录
- 集成测试覆盖跨平台交互

## 8. 依赖管理

### Android
- OkHttp
- ExoPlayer
- Room
- webrtc-kmp

### iOS
- 通过 CocoaPods 集成原生库

### Desktop
- SQLite JDBC
- Java 11+ HTTP Client

### Web
- Kotlin/JS 标准库
- Web APIs (fetch, WebSocket, etc.)

## 9. 未来扩展

- 支持更多平台（如 watchOS, tvOS）
- 引入更高级的架构模式
- 增强安全性实现
- 优化性能和用户体验