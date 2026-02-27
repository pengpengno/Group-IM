---
trigger: manual
---
# 注释编写规则（Comment Rules）

本规则用于约束在本项目中添加、修改代码时的注释方式，目标是：

- 提高可维护性
- 让跨端（Web / Electron Desktop）代码更容易理解
- 避免无价值或重复说明代码本身的注释

---

## 一、基本原则

### 1. 注释说明「为什么」，而不是「做了什么」

禁止仅描述代码表面行为，例如：

```ts
// 设置变量为 true
enabled = true
应该说明业务或设计原因：

// 启用自动重连，用于解决 Electron 渲染进程休眠后连接丢失问题
enabled = true
2. 只有在以下场景必须添加注释
存在平台差异（Web / Electron）

逻辑存在非直观设计

有历史兼容或临时方案

有安全 / 性能 / 稳定性考虑

依赖外部系统或约定（如 IPC、后端接口、协议）

3. 禁止为显而易见的代码添加注释
例如：

// 遍历数组
list.forEach(...)
二、平台相关代码注释规则（强制）
凡是涉及以下内容，必须添加平台说明注释：

Electron API

preload / ipc 通信

桌面能力封装（platform 层）

示例：

// Desktop only：通过 Electron IPC 打开系统文件选择窗口
async openFile(): Promise<PlatformFile> {
  return window.electron.invoke('open-file')
}
Web 侧实现必须注明限制：

// Web 环境无法访问本地文件系统，仅返回空实现
async openFile(): Promise<PlatformFile> {
  throw new Error('Not supported in browser')
}
三、跨端抽象接口注释规则
所有平台抽象接口必须说明：

能力语义

平台差异

可能失败的场景

示例：

export interface PlatformApi {

  /**
   * 打开本地文件并返回文件信息。
   *
   * Desktop：通过 Electron dialog 实现
   * Web：部分浏览器可能不支持或行为受限
   */
  openFile(): Promise<PlatformFile>
}
四、业务逻辑注释规则
业务代码中的注释应集中在以下内容：

状态流转原因

特殊边界场景

与后端 / 协议的约定

示例：

// 文件上传完成后必须先发送 file 消息，再发送业务消息。
// 后端会以 file 消息建立文件索引。
await sendFileMessage(fileId)
await sendChatMessage(message)
五、临时方案与兼容代码必须标注
凡是：

workaround

兼容旧版本

临时绕过问题

必须标注原因和可移除条件。

示例：

// 临时兼容旧客户端（<=1.2.0）未携带 checksum 字段的问题
// 当所有客户端升级后可移除此逻辑
if (!payload.checksum) {
  payload.checksum = ''
}
六、禁止在注释中重复函数名或参数名
禁止如下形式：

// handleSendMessage 处理发送消息
function handleSendMessage(...) {}
应说明该函数的业务角色：

// 统一入口：负责发送所有类型的聊天消息（文本 / 文件 / 语音）
function handleSendMessage(...) {}
七、函数级注释规范
只有在以下情况下添加函数注释：

该函数为公共接口

该函数跨模块调用

该函数封装复杂流程

推荐格式：

/**
 * 发送文件消息的完整流程封装。
 *
 * 包含：
 * 1. 文件上传
 * 2. 获取 fileId
 * 3. 发送文件消息到 TCP 通道
 */
async function sendFileMessage(file: File): Promise<void> {}
八、禁止在 core / domain 层写平台相关注释
以下目录中：

core

domain

usecases

禁止出现：

Electron

浏览器 API

平台差异说明

这些内容必须放在 platform 层。

九、IPC 与 preload 相关注释规则（强制）
preload 中的每一个暴露接口，必须说明：

对应的主进程能力

安全边界

示例：

// 仅暴露文件选择能力，不允许传入任意路径参数，防止滥用文件系统权限
contextBridge.exposeInMainWorld('electron', {
  openFile: () => ipcRenderer.invoke('open-file')
})
十、AI / 自动重构添加注释的规则
当通过 AI 或自动工具生成或重构代码时：

必须补充关键设计注释

不允许生成无意义模板注释

必须优先为以下代码添加注释：

新增的抽象接口

新增的平台实现

修改过的核心业务流程

十一、推荐语言风格
使用简体中文

表述客观、简洁

避免口语化描述

不在注释中写与代码无关的信息

十二、最终目标
通过本规则保证：

新成员可以快速理解 Web / Desktop 差异

Electron 相关能力集中、清晰、可审计

业务代码保持平台无关性