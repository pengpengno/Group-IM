# 跨平台检查与修复计划

## 当前状态分析

### 1. 目录结构问题
- **问题**: 构建配置使用 `wasmJsMain`，但源代码目录是 `jsMain`
- **影响**: 可能导致 Web 平台代码无法正确编译
- **解决方案**: 将 `jsMain` 目录重命名为 `wasmJsMain` 以匹配构建配置

### 2. 平台实现完整性检查

#### Android 平台 (androidMain)
- ✅ 24 个 actual 实现
- ✅ 实现较为完整

#### Desktop 平台 (desktopMain)  
- ✅ 24 个 actual 实现
- ✅ 实现较为完整

#### Web 平台 (jsMain/wasmJsMain)
- ⚠️ 仅 5 个 actual 实现
- ❌ 缺少大量实现：
  - `TryGetPermission` 相关函数
  - `FileStorageManager` 扩展函数
  - `MediaPickerScreen`
  - `CrossPlatformVideo` / `CrossPlatformImage`
  - `FunctionPanel`
  - `ProxyConfigStorage`
  - `VideoThumbnail`
  - `CameraPreviewView`
  - 等等

### 3. 需要检查的 expect 函数列表

从 commonMain 中发现的 expect 函数：

1. `getPlatform(): Platform` ✅ (已实现)
2. `getPlatformServices(): PlatformServices` ✅ (已实现)
3. `TryGetPermission()` ❌ (Web 未实现)
4. `TryGetMultiplePermissions()` ❌ (Web 未实现)
5. `TryGetVideoCallPermissions()` ❌ (Web 未实现)
6. `FileStorageManager.isFileExists()` ❌ (Web 未实现)
7. `FileStorageManager.getLocalFilePath()` ❌ (Web 未实现)
8. `FileStorageManager.getFile()` ❌ (Web 未实现)
9. `MediaPickerScreen()` ❌ (Web 未实现)
10. `VideoScreenView()` ❌ (Web 未实现)
11. `VideoThumbnail()` ❌ (Web 未实现)
12. `PlatformFilePickerPanel()` ❌ (Web 未实现)
13. `CameraPreviewView()` ❌ (Web 未实现)
14. `getPlatformFilePicker(): FilePicker` ❌ (Web 未实现)
15. `CrossPlatformVideo()` ❌ (Web 未实现)
16. `VideoPlayerManager` ❌ (Web 未实现)
17. `CrossPlatformImage()` ❌ (Web 未实现)
18. `ProxyConfigStorage` class ❌ (Web 未实现)

### 4. 数据库驱动问题
- Web 平台的 `createDatabaseDriverFactory()` 返回 `TODO()`
- 需要实现基于 IndexedDB 的数据库驱动或提供替代方案

## 执行计划

### 阶段 1: 修复目录结构
1. 将 `jsMain` 重命名为 `wasmJsMain`
2. 验证构建配置正确识别新目录

### 阶段 2: 检查并补充 Web 平台实现
1. 逐个检查缺失的 actual 实现
2. 为 Web 平台创建占位实现或完整实现
3. 确保所有 expect 函数都有对应的 actual 实现

### 阶段 3: 验证构建配置
1. 检查各平台的依赖配置
2. 确保 Web 平台依赖正确（compose.html.core, webrtc-adapter 等）
3. 验证 Android 和 Desktop 平台依赖完整

### 阶段 4: 测试与验证
1. 尝试编译各平台
2. 修复编译错误
3. 验证运行时行为

## 注意事项

1. **Web 平台限制**: 某些功能在 Web 平台可能无法实现（如文件系统访问、摄像头权限等），需要提供合理的占位实现或降级方案

2. **数据库驱动**: Web 平台需要使用 IndexedDB，可能需要额外的 SQLDelight 配置

3. **权限处理**: Web 平台的权限模型与移动端不同，需要适配

4. **文件系统**: Web 平台无法直接访问文件系统，需要使用浏览器 API（如 File API、IndexedDB 等）
