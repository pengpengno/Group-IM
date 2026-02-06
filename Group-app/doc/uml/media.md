

# 媒体资源管理设计文档

## 1. 项目概述

这是一个基于Kotlin Multiplatform (KMP) 的即时通讯应用，其中的媒体资源管理模块负责处理视频、图片等多媒体内容的展示、播放和缓存等功能。

## 2. 核心架构

### 2.1 KMP Expect/Actual 模式

#### 2.1.1 跨平台接口定义 (CommonMain)

在 `commonMain` 中定义了平台无关的接口和数据结构：

**CrossPlatformVideo.kt**
```kotlin
@Composable
expect fun CrossPlatformVideo(
    file: File,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    onClose: (() -> Unit)? = null
)

expect object VideoPlayerManager {
    fun play(file: File)
    fun release()
    @Composable fun Render()
}
```


**MediaFileView.kt**
```kotlin
@Composable
expect fun VideoThumbnail(
    file: File,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
)
```


**FilePicker.kt**
```kotlin
@Composable
expect fun CameraPreviewView(): Unit

expect fun getPlatformFilePicker(): FilePicker
```


#### 2.1.2 平台特定实现 (AndroidMain)

在 `androidMain` 中提供了具体的平台实现：

**CrossPlatformVideo.android.kt**
```kotlin
@Composable
@OptIn(UnstableApi::class)
actual fun CrossPlatformVideo(
    file: File,
    modifier: Modifier,
    size: Dp,
    onClose: (() -> Unit)?
) {
    VideoThumbnail(
        file = file,
        modifier = modifier.size(size),
        onClick = {
            VideoPlayerManager.play(file)
        }
    )
}

actual object VideoPlayerManager {
    // 具体的ExoPlayer实现
}
```


### 2.2 文件数据模型

**FileData Sealed Class**
```kotlin
sealed class FileData {
    data class Bytes(val data: ByteArray) : FileData()
    data class Path(val path: String) : FileData()
    object None : FileData()
}
```


**File Data Class**
```kotlin
data class File(
    val name: String,
    val path: String,
    val mimeType: String?,
    val size: Long,
    val data : FileData = FileData.None
)
```


## 3. 媒体资源管理功能

### 3.1 视频处理

#### 3.1.1 视频缩略图生成
- 使用 `MediaMetadataRetriever` 提取视频首帧
- 实现了缓存机制，避免重复提取
- 支持多种数据源类型（文件路径、Content URI、字节数组）

#### 3.1.2 视频播放器管理
- 单例模式，确保同时只有一个视频播放器实例
- 使用 ExoPlayer 作为底层播放引擎
- 支持全屏播放和基本控制功能

### 3.2 图片处理

#### 3.1.1 跨平台图片组件
- [CrossPlatformImage](file://D:\ideaproject\pengpeng\Group\Group-app\Group\composeApp\src\desktopMain\kotlin\com\github\im\group\sdk\CrossPlatformImage.desktop.kt#L19-L74) 用于显示图片
- 支持多种图片格式和数据源

### 3.3 文件选择器

#### 3.3.1 平台特定实现
- 支持选择图片、视频和普通文件
- 支持拍照功能
- 支持读取文件内容为字节数组

## 4. 缓存策略

### 4.1 视频缩略图缓存
- 使用 [VideoCache](file://D:\ideaproject\pengpeng\Group\Group-app\Group\composeApp\src\androidMain\kotlin\com\github\im\group\config\VideoCache.kt#L17-L106) 类管理视频缩略图缓存
- 基于文件路径的缓存键值
- 自动清理过期缓存

### 4.2 播放器资源管理
- 自动释放播放器资源
- 防止内存泄漏
- 单例播放器模式

## 5. 数据类型支持

### 5.1 文件类型检测
- [isVideo()](file://D:\ideaproject\pengpeng\Group\Group-app\Group\composeApp\src\commonMain\kotlin\com\github\im\group\sdk\MediaFileView.kt#L66-L75): 检测视频文件（mp4, mov, mkv, avi等）
- [isImage()](file://D:\ideaproject\pengpeng\Group\Group-app\Group\composeApp\src\commonMain\kotlin\com\github\im\group\sdk\MediaFileView.kt#L80-L89): 检测图片文件（jpg, png, gif等）

### 5.2 数据源类型
- `FileData.Bytes`: 字节数组形式的数据
- `FileData.Path`: 文件路径形式的数据
- `FileData.None`: 无数据

## 6. UI 组件

### 6.1 MediaFileView
统一的媒体文件查看组件，自动判断文件类型并选择合适的展示方式：
- 视频：显示缩略图 + 播放按钮
- 图片：显示图片预览
- 其他：显示文件图标

### 6.2 视频播放界面
- 全屏播放界面
- 播放/暂停控制
- 进度条和时间显示
- 自动隐藏控制栏

## 7. 最佳实践

### 7.1 KMP 设计原则
- **接口与实现分离**：`expect` 声明在 `commonMain`，`actual` 实现在平台模块
- **状态管理**：避免在 `commonMain` 中直接使用状态管理
- **平台API隔离**：平台特定的API只能在相应的 `actual` 实现中使用

### 7.2 内存管理
- 使用流式处理避免内存溢出
- 及时释放播放器资源
- 实现缓存机制减少重复操作

### 7.3 异常处理
- 区分协程取消异常和其他异常
- 适当的资源清理
- 用户友好的错误提示

## 8. 扩展性考虑

### 8.1 支持更多媒体类型
通过扩展 [FileData](file://D:\ideaproject\pengpeng\Group\Group-app\Group\composeApp\src\commonMain\kotlin\com\github\im\group\sdk\FilePicker.kt#L14-L18) sealed class 和添加相应的处理逻辑，可以轻松支持更多媒体类型。

### 8.2 平台扩展
通过添加新的 `actual` 实现，可以支持 iOS、Desktop 等其他平台。

这份设计文档涵盖了当前项目的媒体资源管理架构，特别是展示了 KMP 中 expect/actual 模式的最佳实践，以及如何有效地管理多媒体内容的展示和播放。