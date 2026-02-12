# Skill: Electron 项目重构为 Web / Desktop 可共用结构

## 描述

用于将 Group-app Electron 桌面端项目重构为：

- 业务逻辑可同时运行在 Web 与 Desktop
- Electron 仅作为桌面能力壳存在  
- 通过 platform 层隔离所有平台差异
- 支持 Kotlin Multiplatform 共享业务逻辑

适用于希望后续支持 Web 版本或统一前端业务层的 Electron 项目。

---

## 触发示例

当用户输入以下或相近语义时触发本技能：

- 重构这个 Electron 项目，让 Web 和 Desktop 共用
- 拆分 Electron 项目为跨平台结构
- 把 Electron 业务抽出来支持 Web
- 重构为 web / desktop 通用架构
- 将 Group-app 重构为支持 Web 端的架构

---

## 执行规范（必须按顺序执行）

### 1. 扫描工程

识别并列出：

- main 进程入口文件 (`main/index.ts`)
- preload 脚本文件 (`preload/index.ts`)
- renderer 入口 (`renderer/index.tsx`)
- 所有直接引用以下模块的文件：

```text
electron
ipcRenderer
ipcMain
dialog
BrowserWindow
fs
path
os
crypto
```

输出：

Electron 强依赖文件清单

### 2. 分类现有代码
将现有文件分为三类：

A. 可跨平台业务代码
- 业务服务 (`services/`)
- 状态管理 (`store/`, `features/*/slice.ts`)
- 网络请求 (`api/`)
- 领域模型 (`types/`)
- KMP 共享模块 (`composeApp/`)

B. UI 层代码
- 页面组件 (`features/*/`)
- 视图逻辑
- 路由 (`App.tsx`)
- 样式文件

C. 桌面能力代码
- 文件系统操作
- 窗口控制
- 原生能力
- 系统通知
- 本地存储

### 3. 生成目标目录结构
生成并说明如下结构：

```
src/
  core/
    domain/           # 领域模型
    services/         # 业务服务
    usecases/         # 业务用例
    
  ui/
    components/       # 通用组件
    features/         # 功能模块
      auth/
      chat/
      contacts/
      profile/
      video-call/
    
  platform/
    web/             # Web 平台实现
    desktop/         # 桌面平台实现
      electron/
        main/        # 主进程
        preload/     # 预加载脚本
        services/    # 桌面服务
        
  shared/            # KMP 共享模块
    composeApp/      # Kotlin 多平台业务逻辑
```

并说明每个目录的职责。

### 4. 设计统一平台接口
必须生成统一平台接口，例如：

```typescript
export interface PlatformApi {
  // 文件操作
  selectFile(options: SelectFileOptions): Promise<SelectFileResult>
  saveFile(data: Uint8Array, filename: string): Promise<boolean>
  
  // 网络状态
  getNetworkStatus(): NetworkStatus
  
  // 通知
  showNotification(title: string, options: NotificationOptions): void
  
  // 存储
  localStorage: {
    getItem(key: string): Promise<string | null>
    setItem(key: string, value: string): Promise<void>
    removeItem(key: string): Promise<void>
  }
  
  // 音视频
  getMediaDevices(): Promise<MediaDeviceInfo[]>
}
```

该接口只能描述能力，不允许包含 Electron 类型。

### 5. Desktop 实现
在以下目录中实现基于 Electron 的平台能力封装：

`src/platform/desktop`

示例代码：

```typescript
export class DesktopPlatformApi implements PlatformApi {
  async selectFile(options: SelectFileOptions): Promise<SelectFileResult> {
    return window.electron.invoke('select-file', options)
  }
  
  async saveFile(data: Uint8Array, filename: string): Promise<boolean> {
    return window.electron.invoke('save-file', { data, filename })
  }
  
  getNetworkStatus(): NetworkStatus {
    // Electron 网络状态检测
    return { online: navigator.onLine }
  }
  
  showNotification(title: string, options: NotificationOptions): void {
    window.electron.invoke('show-notification', { title, ...options })
  }
}
```

### 6. Web 实现
在以下目录中实现浏览器版本的平台能力封装：

`src/platform/web`

示例代码：

```typescript
export class WebPlatformApi implements PlatformApi {
  async selectFile(options: SelectFileOptions): Promise<SelectFileResult> {
    // 使用浏览器文件 API
    const input = document.createElement('input')
    input.type = 'file'
    // ... 实现文件选择逻辑
    return { canceled: false, filePath: 'web-file-path' }
  }
  
  async saveFile(data: Uint8Array, filename: string): Promise<boolean> {
    // 使用浏览器下载 API
    const blob = new Blob([data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
    return true
  }
  
  getNetworkStatus(): NetworkStatus {
    return { online: navigator.onLine }
  }
  
  showNotification(title: string, options: NotificationOptions): void {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, options)
    }
  }
}
```

### 7. 统一平台注入入口
必须生成文件：

`src/platform/index.ts`

示例：

```typescript
import { DesktopPlatformApi } from './desktop'
import { WebPlatformApi } from './web'

export const isElectron = () => {
  return typeof window !== 'undefined' && 
         typeof window.process !== 'undefined' && 
         window.process.type === 'renderer'
}

export const platformApi = isElectron() 
  ? new DesktopPlatformApi() 
  : new WebPlatformApi()

// 类型定义
export type { PlatformApi } from './types'
```

### 8. 重构业务代码
将所有业务代码中直接调用 Electron 的方式替换为统一平台接口调用。

例如将：

```typescript
import { ipcRenderer } from 'electron'

const result = await ipcRenderer.invoke('select-file', options)
```

替换为：

```typescript
import { platformApi } from '@/platform'

const result = await platformApi.selectFile(options)
```

并禁止在以下目录中直接引用 Electron：

- `core/`
- `domain/`  
- `services/`
- `usecases/`
- `features/`

### 9. 重构 preload
preload 层必须满足：

- 仅负责暴露最小 IPC API
- 不包含任何业务逻辑
- 通过 contextBridge 暴露接口

示例：

```typescript
import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('electron', {
  invoke: (channel: string, ...args: any[]) => 
    ipcRenderer.invoke(channel, ...args),
    
  on: (channel: string, listener: (...args: any[]) => void) => 
    ipcRenderer.on(channel, (_, ...args) => listener(...args)),
    
  removeListener: (channel: string, listener: (...args: any[]) => void) => 
    ipcRenderer.removeListener(channel, listener)
})
```

### 10. 集成 KMP 共享模块
由于项目使用 Kotlin Multiplatform：

1. 在 `shared/composeApp/` 目录维护共享业务逻辑
2. 通过 JS 编译输出供 Web/Desktop 使用
3. 确保平台接口与 KMP 模块的兼容性
4. 在业务服务层调用 KMP 共享功能

### 11. 输出重构结果
必须输出：

**新增文件列表**
- `src/platform/` 目录下所有文件
- `src/core/` 业务核心代码
- 平台接口定义文件

**修改文件列表**
- 所有直接调用 Electron API 的业务文件
- `preload/index.ts` 预加载脚本
- `main/index.ts` 主进程文件

**删除或废弃文件列表**
- 直接在业务代码中的 Electron 引用
- 平台相关的条件编译代码

**当前仍无法跨平台的能力说明**
- 某些原生系统功能
- 特定的桌面环境集成
- 硬件访问权限限制

## 强制约束

- `core` 层禁止出现：
  - `electron`
  - `fs`
  - `path` 
  - `ipcRenderer`
  - `BrowserWindow`

- 所有平台差异必须集中在 `platform` 层

- UI 层禁止直接访问 Electron API

- 必须保持与 KMP 共享模块的兼容性

## 输出格式要求

最终输出必须包含：

1. **新目录结构树**
2. **平台接口定义代码**
3. **Desktop 实现代码**
4. **Web 实现代码**
5. **一个业务模块改造前 / 改造后的代码对比示例**

## 项目特定注意事项

- 项目使用 React 18 + TypeScript + Redux Toolkit
- 构建工具为 Webpack
- 包含音视频通话功能（WebRTC）
- 需要考虑移动端 UI 设计的响应式适配
- 存在多个 types 文件需要合并统一