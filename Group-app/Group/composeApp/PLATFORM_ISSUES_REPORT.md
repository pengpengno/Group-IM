# 跨平台问题检查报告

## 🔴 关键问题

### 1. 目录名称不匹配（必须修复）
- **问题**: 构建配置使用 `wasmJsMain`，但源代码目录是 `jsMain`
- **位置**: `composeApp/build.gradle.kts` 第 53 行引用 `wasmJsMain`
- **影响**: Web 平台代码可能无法正确编译
- **解决方案**: 
  - 选项 A: 将 `jsMain` 目录重命名为 `wasmJsMain`（推荐）
  - 选项 B: 修改构建配置，将 `wasmJsMain` 改为 `jsMain`（不推荐，不符合 Kotlin Multiplatform 规范）

### 2. Web 平台实现严重缺失
- **当前状态**: 仅 5 个 actual 实现
- **缺失数量**: 约 19 个 expect 函数缺少 actual 实现

## 📋 缺失实现详细列表

### Web 平台 (jsMain/wasmJsMain) 缺失的实现：

#### 1. 权限处理相关 (3个)
- ❌ `TryGetPermission()` - 音频录制权限
- ❌ `TryGetMultiplePermissions()` - 多权限处理
- ❌ `TryGetVideoCallPermissions()` - 视频通话权限

#### 2. 文件存储相关 (3个)
- ❌ `FileStorageManager.isFileExists()` - 检查文件是否存在
- ❌ `FileStorageManager.getLocalFilePath()` - 获取本地文件路径
- ❌ `FileStorageManager.getFile()` - 获取文件对象

#### 3. UI 组件相关 (5个)
- ❌ `MediaPickerScreen()` - 媒体选择屏幕
- ❌ `VideoScreenView()` - 视频屏幕视图
- ❌ `VideoThumbnail()` - 视频缩略图
- ❌ `PlatformFilePickerPanel()` - 平台文件选择面板
- ❌ `CameraPreviewView()` - 相机预览视图

#### 4. 文件选择相关 (1个)
- ❌ `getPlatformFilePicker(): FilePicker` - 获取平台文件选择器

#### 5. 视频处理相关 (3个)
- ❌ `CrossPlatformVideo()` - 跨平台视频组件
- ❌ `VideoPlayerManager` - 视频播放器管理器
- ❌ `CrossPlatformImage()` - 跨平台图片组件

#### 6. 配置相关 (1个)
- ❌ `ProxyConfigStorage` class - 代理配置存储

#### 7. 数据库相关 (1个)
- ❌ `createDatabaseDriverFactory(): DatabaseDriverFactory` - 数据库驱动工厂（当前返回 TODO）

## ✅ 已实现的 Web 平台功能

1. ✅ `getPlatform(): Platform` - 平台信息
2. ✅ `getPlatformServices(): PlatformServices` - 平台服务工厂
3. ✅ `WebRTC.js.kt` - WebRTC 管理器
4. ✅ `FilePicker.js.kt` - 文件选择器（部分）
5. ✅ `main.js.kt` - 应用入口点

## 📊 平台实现对比

| 功能模块 | Android | Desktop | Web | 状态 |
|---------|---------|---------|-----|------|
| 平台服务 | ✅ | ✅ | ✅ | 完整 |
| 权限处理 | ✅ | ✅ | ❌ | Web 缺失 |
| 文件存储 | ✅ | ✅ | ❌ | Web 缺失 |
| 媒体选择 | ✅ | ✅ | ❌ | Web 缺失 |
| 视频处理 | ✅ | ✅ | ❌ | Web 缺失 |
| 文件选择 | ✅ | ✅ | ⚠️ | Web 部分实现 |
| 数据库驱动 | ✅ | ✅ | ❌ | Web 缺失 |
| WebRTC | ✅ | ⚠️ | ✅ | Desktop 部分支持 |

## 🎯 执行优先级

### 高优先级（必须修复）
1. **目录重命名**: `jsMain` → `wasmJsMain`
2. **数据库驱动**: 实现或提供替代方案
3. **文件存储扩展函数**: 3个函数实现

### 中优先级（功能完整性）
4. **权限处理**: 3个函数实现（Web 平台权限模型不同）
5. **文件选择器**: 完善 `getPlatformFilePicker()` 实现
6. **媒体选择**: `MediaPickerScreen()` 实现

### 低优先级（可选功能）
7. **视频处理**: 视频相关组件实现
8. **代理配置**: `ProxyConfigStorage` 实现
9. **相机预览**: `CameraPreviewView()` 实现

## 💡 实施建议

### 阶段 1: 修复目录结构（立即执行）
```bash
# 重命名目录
mv composeApp/src/jsMain composeApp/src/wasmJsMain
```

### 阶段 2: 补充关键实现（按优先级）
1. 先实现文件存储相关函数（使用 IndexedDB 或 localStorage）
2. 实现数据库驱动或提供内存数据库替代方案
3. 实现权限处理（Web 平台使用浏览器权限 API）

### 阶段 3: 完善功能实现
1. 实现媒体选择相关组件
2. 完善文件选择器
3. 实现视频处理组件

### 阶段 4: 测试验证
1. 编译各平台验证无错误
2. 运行时测试关键功能
3. 修复发现的 bug

## ⚠️ 注意事项

1. **Web 平台限制**:
   - 无法直接访问文件系统，需要使用浏览器 API
   - 权限模型与移动端不同（基于用户交互）
   - 数据库需要使用 IndexedDB 或 Web SQL（已废弃）

2. **兼容性考虑**:
   - Web 平台某些功能可能需要降级实现
   - 某些功能在 Web 平台可能不可用（如原生相机）

3. **测试要求**:
   - 需要在真实浏览器环境中测试
   - 考虑不同浏览器的兼容性
