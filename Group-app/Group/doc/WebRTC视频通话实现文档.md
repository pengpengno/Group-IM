# WebRTC 视频通话实现文档

## 1. 概述

本文档详细描述了 Android 平台上 WebRTC 视频通话的实现流程，包括初始化、连接建立、媒体协商和资源清理等关键步骤。

## 2. WebRTC 时序图

请参考 [WebRTC时序图.uml](./uml/WebRTC时序图.uml) 查看详细的时序图。

## 3. 核心组件说明

### 3.1 WebRTCManager 接口

WebRTCManager 定义了视频通话的核心功能接口：

- `initialize()` - 初始化 WebRTC 环境
- `createLocalMediaStream()` - 创建本地媒体流
- `connectToSignalingServer()` - 连接信令服务器
- `initiateCall()` - 发起呼叫
- `acceptCall()` - 接受呼叫
- `endCall()` - 结束通话
- `sendIceCandidate()` - 发送 ICE 候选

### 3.2 AndroidWebRTCManager 实现

AndroidWebRTCManager 是 WebRTCManager 在 Android 平台的具体实现：

- 使用 Google WebRTC 库
- 通过 WebSocket 进行信令传输
- 支持音视频通话功能

## 4. 关键流程详解

### 4.1 初始化流程

1. 初始化 PeerConnectionFactory
2. 创建本地媒体流（音频+视频轨道）
3. 配置 ICE 服务器（STUN）
4. 设置 SDP 语义（PLAN_B）

### 4.2 信令连接流程

1. 根据 ProxyConfig.host 构建 WebSocket URL
2. 建立 WebSocket 连接
3. 准备接收和发送信令消息

### 4.3 呼叫建立流程

#### 发起呼叫：
1. 创建 PeerConnection（如果尚未存在）
2. 发送 `call/request` 消息
3. 等待对方接受

#### 接受呼叫：
1. 创建 PeerConnection（如果尚未存在）
2. 发送 `call/accept` 消息
3. 开始 SDP 协商

### 4.4 SDP 协商流程

1. 发起方创建 Offer 并设置为本地描述
2. 发起方通过信令服务器发送 Offer 给接收方
3. 接收方设置 Offer 为远程描述
4. 接收方创建 Answer 并设置为本地描述
5. 接收方通过信令服务器发送 Answer 给发起方
6. 发起方设置 Answer 为远程描述

### 4.5 ICE 候选交换流程

1. 双方通过 onIceCandidate 回调收集 ICE 候选
2. 将候选信息通过信令服务器发送给对方
3. 对方通过 addIceCandidate 添加收到的候选

### 4.6 资源清理流程

1. 关闭 PeerConnection
2. 停止并释放视频捕获器
3. 释放 SurfaceTextureHelper
4. 清理媒体流资源
5. 关闭 WebSocket 连接

## 5. 信令消息格式

所有信令消息都通过 WebSocket 以 JSON 格式传输：

### 5.1 呼叫相关消息

```json
{
  "type": "call/request|call/accept|call/end",
  "fromUser": "发送方用户ID",
  "toUser": "接收方用户ID"
}
```

### 5.2 SDP 协商消息

```json
{
  "type": "offer|answer",
  "fromUser": "发送方用户ID",
  "toUser": "接收方用户ID",
  "sdp": "SDP描述内容",
  "sdpType": "offer|answer"
}
```

### 5.3 ICE 候选消息

```json
{
  "type": "candidate",
  "fromUser": "发送方用户ID",
  "toUser": "接收方用户ID",
  "candidate": {
    "candidate": "候选描述",
    "sdpMid": "SDP中段标识",
    "sdpMLineIndex": "SDP中媒体行索引"
  }
}
```

## 6. 设计原则与注意事项

### 6.1 PeerConnection 管理
- PeerConnection 应在通话初始化时创建一次
- 后续通过判空检查复用，避免重复创建
- 通话结束时通过 cleanup 方法正确释放

### 6.2 资源释放
- 视频捕获器需要显式调用 stopCapture() 和 dispose()
- 所有资源释放操作都应包含异常处理
- 按照依赖关系顺序释放资源

### 6.3 错误处理
- 所有异步操作都应包含错误回调处理
- 网络异常时应有重连机制
- 信令处理中应包含异常捕获

### 6.4 安全性
- 通过应用服务器代理信令通信，而非直接连接
- 根据协议类型（http/https）选择 ws/wss 连接
- 用户身份验证应在应用层完成

## 7. 常见问题及解决方案

### 7.1 SDP 协商失败
- 确保媒体流正确创建并添加到 PeerConnection
- 检查编解码器兼容性
- 验证网络连接和防火墙设置

### 7.2 ICE 连接失败
- 确认 STUN 服务器配置正确
- 检查网络连接和 NAT 类型
- 验证防火墙和端口开放情况

### 7.3 摄像头资源释放异常
- 在 cleanup 方法中为每个资源释放操作添加异常处理
- 确保按正确顺序释放依赖资源
- 快速切换时增加适当的延迟