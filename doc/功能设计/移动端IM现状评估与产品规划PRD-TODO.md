# 移动端 IM 现状评估与产品规划 PRD + TODO

## 1. 文档说明

本文不是从 0 设计 IM 系统，而是基于当前项目中已经存在的移动端实现、服务端协议与数据链路，对现状进行产品化梳理，并给出后续可落地的规划建议。

分析基于以下现有代码与架构事实：

- 前端：Kotlin Multiplatform + Compose Multiplatform
- 后端：Spring WebFlux + Reactor TCP
- 通信协议：Protobuf
- 文件上传：HTTP
- 实时消息：TCP 长连接
- 存储：PostgreSQL + Redis

重点参考实现：

- `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/viewmodel/ChatRoomViewModel.kt`
- `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/viewmodel/ChatViewModel.kt`
- `Group-app/Group/composeApp/src/commonMain/kotlin/com/github/im/group/repository/MessageRepository.kt`
- `Group-app/Group/composeApp/src/androidMain/kotlin/com/github/im/group/connect/AndroidSocketClient.kt`
- `server/src/main/java/com/github/im/server/service/MessageService.java`
- `server/src/main/java/com/github/im/server/handler/impl/AckProcessServiceHandler.java`

---

## 2. 当前功能结构梳理

## 2.1 会话模块

### 已实现功能

- 会话列表展示
- 私聊会话创建或获取
- 群聊创建
- 本地会话缓存
- 会话最近消息摘要展示
- 会话未读数展示
- 本地优先、远程补全的离线兜底策略

### 实现方式

- `ChatViewModel` 负责会话首页状态管理。
- `getConversations()` 采用“两阶段加载”：
  1. 先从本地数据库读取会话与最近消息
  2. 再异步调用 `ConversationApi.getActiveConversationsByUserId()` 拉取远端数据并覆盖 UI
- `ConversationRepository` 负责会话信息和成员信息落库。
- `ConversationRepository.getLocalConversationByMembers()` 已支持通过成员查找私聊会话。
- `ChatUI` 已完成会话列表、摘要文本、未读红点、空态和离线提示的 UI 展示。

### 当前判断

会话模块已经不是空白状态，已具备基础可用能力，但仍偏“工程能力集合”，离成熟 IM 首页还有差距。

---

## 2.2 消息模块

### 已实现功能

- 单聊文本消息发送
- 消息本地落库
- 本地消息结构中包含 `clientMsgId`
- 消息列表展示
- 按会话加载消息
- 按 `sequenceId` 增量同步消息
- 发送中 / 已发送 / 已读 / 失败的基础状态枚举
- 本地消息与服务端消息合并更新
- 离线消息暂存表

### 实现方式

- `ChatRoomViewModel.performSend()` 在发送时先构造本地消息，并立即插入 UI 与本地数据库。
- `ChatMessageRepository` 使用 `clientMsgId` 与 `msgId` 作为本地更新和去重依据。
- `MessageSyncRepository.syncMessages()` 通过会话维度的最大 `sequenceId` 实现增量拉取。
- 服务端 `MessageService.handleMessage()` 会：
  1. 保存消息到数据库
  2. 分配 `msgId`
  3. 分配 `sequenceId`
  4. 推送给会话成员

### 当前判断

消息模块已经具备“能发、能收、能本地缓存、能增量同步”的骨架，但消息可靠性链路尚未真正闭环。

---

## 2.3 连接模块

### 已实现功能

- 登录后发起 TCP 连接
- 基础长连接收发
- 心跳 ping/pong
- 自动重连
- 消息按会话路由到当前聊天页

### 实现方式

- `AndroidSocketClient` 管理底层 `Socket/InputStream/OutputStream`。
- `startHeartbeat()` 定时发 ping，并在收到 pong 时刷新心跳时间。
- `startAutoReconnect()` 负责指数退避重连。
- `SenderSdk` 在登录时通过 `loginConnect()` 发起注册连接。
- `ConnectionLoginListener` 在登录后触发连接建立。
- `ChatSessionManager` 负责按 `conversationId` 将消息投递给对应处理器。

### 当前判断

连接层已经具备基础可用能力，但存在职责重复和恢复链路不完整的问题。

---

## 2.4 文件模块

### 已实现功能

- 文件消息发送
- 文件 HTTP 上传
- 文件消息展示
- 文件元数据本地缓存
- 文件下载
- 语音、图片、视频、普通文件的消息类型区分

### 实现方式

- 当前文件发送链路是：
  1. 先通过 TCP 发送消息
  2. 再通过 HTTP 上传文件内容
- `FileUploadService.uploadFileData()` 调用 `FileApi.uploadFile()`
- `FilesRepository` 负责本地文件元数据维护
- `ChatRoomViewModel` 已整合文件上传、文件下载、媒体消息读取逻辑
- `ChatBubble` / `UnifiedFileMessage` / `VoiceMessage` 已承载基础渲染能力

### 当前判断

文件模块已经有完整骨架，但“消息发送”和“文件可用”之间还没有严格同步，体验不稳定。

---

## 2.5 UI / 交互模块

### 已实现功能

- 会话列表 UI
- 聊天室 UI
- 消息气泡
- 下拉刷新
- 上滑加载历史
- 输入框、发送按钮
- 语音录制入口
- 语音播放 UI
- 文件消息展示
- 视频通话入口
- 搜索联系人入口

### 实现方式

- Compose 页面主要集中在：
  - `ChatUI.kt`
  - `ChatRoomScreen.kt`
  - `ChatInputArea.kt`
  - `ChatBubble.kt`
- 状态主要通过 ViewModel + StateFlow 驱动。
- `ChatRoomScreen` 已展示消息状态图标：
  - `SENDING`
  - `SENT`
  - `READ`
  - `FAILED`

### 当前判断

UI 已经超过原型阶段，但产品行为与状态反馈还不够稳定，尤其是在弱网和文件消息场景。

---

## 3. 现存问题分析

## 3.1 消息可靠性问题

### 现状

- 客户端有 `clientMsgId`
- 本地有 `OfflineMessage` 表
- 服务端有 `AckProcessServiceHandler`
- 服务端消息保存后会生成 `msgId` 与 `sequenceId`

### 核心问题

当前 ACK 并没有形成真正闭环。

- `ChatSessionManager.routeMessage()` 已识别 `pkg.ack`
- 但只是记录日志，没有真正更新本地消息状态
- `ChatRoomViewModel.handleMessageAck()` 存在，但没有形成稳定调用路径
- 结果是客户端更多依赖“服务端再次推送自己的消息”来间接更新状态

### 风险

- 弱网下消息可能已经入库，但客户端仍显示发送中
- 离线重发时容易出现重复消息
- 用户无法判断消息是否真的成功送达

### 结论

`clientMsgId` 已经设计对了，但使用深度不够，需立即补齐 ACK 消费链路。

---

## 3.2 一致性问题

### 本地消息 vs 服务端消息

当前已有本地消息和服务端消息的合并逻辑，但还存在问题：

- 本地消息先插入，服务端消息后到达
- 文件消息是先发消息、后上传文件
- 文件元数据可能晚于消息本体到达

这会导致：

- 聊天页短时间出现占位消息
- 文件点击时元数据不完整
- 本地状态和服务端真实状态有时间差

### 顺序问题

- 当前消息排序主要依赖 `sequenceId`
- 但本地新插入消息在获得服务端 `sequenceId` 前排序并不稳定
- 发送中消息位置可能波动

### 已读一致性问题

- 本地未读数已能统计
- 服务端 `markConversationAsRead()` 已实现按 `sequenceId` 批量已读
- 但移动端没有稳定发送 READ ACK 的机制

### 结论

当前不是“没有一致性设计”，而是“设计存在，但链路未打通”。

---

## 3.3 性能问题

### ViewModel 过重

`ChatRoomViewModel` 当前承担了过多职责：

- 聊天室初始化
- 消息发送
- 消息同步
- 文件上传
- 文件下载
- 离线消息处理
- 会话创建
- 媒体消息管理
- 滚动状态控制

`ChatViewModel` 也同时承担：

- 会话列表加载
- 最近消息组装
- 未读统计
- 会话创建入口
- 时间文案格式化

### 数据访问颗粒度偏粗

- 会话列表中每个会话都临时查询最近消息和未读数
- `saveConversation()` 采用“删除全部成员再重插”的方式
- 群成员变多后会放大本地事务成本

### 结论

当前性能问题还没到“不可用”，但后续功能一增加就会快速恶化。

---

## 3.4 架构问题

### TCP 与 HTTP 职责边界不够清晰

当前设计是：

- 文本消息：TCP
- 文件内容：HTTP
- 文件消息通知：TCP

这个方向本身是合理的，但当前实现顺序不理想：

- 先发消息
- 再上传文件

这会导致消息已进入会话，但文件还未真正可用。

### 重连逻辑重复

当前两处都在管重连：

- `AndroidSocketClient`
- `SenderSdk`

这容易造成：

- 重连竞争
- 状态不一致
- 调试复杂

### Redis 使用现状

当前 Redis 更像：

- 在线节点定位
- 跨节点消息路由

并没有真正作为“客户端消息状态中心”。

这是合理的，不建议把 Redis Stream 直接承担客户端 ACK 真值存储；客户端状态的最终真值仍应以 PostgreSQL 中的消息记录为主。

---

## 4. IM 必备能力缺失清单

## 4.1 基础能力缺失

### 1. 消息 ACK 闭环

- 现状：有协议痕迹，有服务端 ACK handler，有客户端 ack 分支
- 问题：客户端未真正消费 ACK 更新状态
- 影响：消息成功状态不可信

### 2. 未读回执闭环

- 现状：有未读数 UI，有服务端按 sequence 批量标记已读方法
- 问题：客户端未稳定上报 READ ACK
- 影响：未读数和已读状态失真

### 3. 离线重发

- 现状：有 `OfflineMessage` 表
- 问题：`processOfflineMessages()` 尚未真正实现重发逻辑
- 影响：离线消息只是缓存，不是可靠发送

### 4. 连接恢复后的补偿流程

- 现状：有重连
- 问题：没有重连后的补注册、补拉消息、补发待确认消息
- 影响：断网恢复后容易出现消息断层

---

## 4.2 进阶能力缺失

### 1. 撤回消息

- 现状：本地状态枚举已有 `REVOKE`
- 问题：缺少完整 API、协议和 UI

### 2. 消息编辑

- 现状：未落地
- 问题：产品能力缺失

### 3. 多端同步

- 现状：有按 `sequenceId` 拉消息基础
- 问题：没有统一的已读游标、多设备状态、会话态同步设计

### 4. 会话产品能力

- 现状：有基础会话列表
- 缺失：置顶、静音、草稿、删除、归档等能力

---

## 4.3 实时能力缺失

### 已有

- 心跳
- 重连
- 增量拉消息

### 缺失

- 统一连接状态机
- 连接状态 UI
- 重连成功后的自动恢复流程
- 发送失败后的可见重试入口

---

## 4.4 多媒体能力缺失

### 图片 / 视频

- 已有类型定义和基础展示
- 缺失上传进度、失败重试、发送前预览闭环、缩略图稳定展示

### 语音消息

- 已有录制、播放、时长基础
- 缺失正式发送确认链路、失败重试、已听状态

### 文件消息

- 已有上传与下载
- 缺失“消息可见时文件必可用”的稳定保障

---

## 5. 优先级规划

## 5.1 P0（必须立即做）

### P0-1 实现消息 ACK 机制

#### 为什么优先

这是消息状态流转的根基，决定发送成功、失败、离线重发、去重是否可信。

#### 不做风险

- 消息长期卡在发送中
- 弱网下重复发送
- 客户端和服务端状态不一致

#### 改造成本

- 中

#### 技术建议

- 服务端消息入库后，向发送者返回 ACK 或返回带 `clientMsgId + msgId + sequenceId` 的确认消息
- 客户端按 `clientMsgId` 更新本地状态为 `SENT`

---

### P0-2 补齐离线消息重发

#### 为什么优先

既然本地已经有离线消息表，这部分必须形成闭环，否则现有设计价值没有兑现。

#### 不做风险

- 离线消息无法自动恢复发送
- 重连后消息仍停留在本地

#### 改造成本

- 低

#### 技术建议

- 在 `processOfflineMessages()` 中遍历 `SENDING/FAILED` 消息
- 根据 `retry_count` 重试发送
- 成功后删除离线消息记录
- 失败后更新重试次数，超过阈值标为 `FAILED`

---

### P0-3 打通已读回执

#### 为什么优先

未读数、已读图标、会话排序等都依赖该能力。

#### 不做风险

- 未读数不准
- 已读状态无意义
- 多端后续无法扩展

#### 改造成本

- 中

#### 技术建议

- 进入会话并滚动到底部后，发送 `READ ACK(conversationId, lastSequenceId)`
- 服务端批量标记 <= `lastSequenceId` 的消息为已读
- 服务端再把 READ 状态推回发送者

---

### P0-4 统一重连恢复流程

#### 为什么优先

现在有“重连”，没有“恢复”，属于 IM 基础体验缺口。

#### 不做风险

- 重连后错过消息
- 消息状态无法恢复
- 用户误认为系统不稳定

#### 改造成本

- 中

#### 技术建议

- 统一只保留一套连接状态机
- 重连成功后执行：
  1. 注册身份
  2. 增量补拉消息
  3. 重发待确认消息

---

## 5.2 P1（核心体验）

### P1-1 调整文件消息链路

#### 为什么优先

当前文件消息是最容易出现一致性问题的一类消息。

#### 不做风险

- 接收方先看到消息，后看到文件
- 文件消息无法打开

#### 改造成本

- 中

#### 技术建议

- 低成本方案：先上传文件，成功后再发 TCP 消息
- 中期方案：先发占位消息，但只有上传成功后才切换状态为 `SENT`

---

### P1-2 会话列表产品化

#### 为什么优先

当前有会话列表，但更像“数据展示页”，不够像成熟 IM 首页。

#### 不做风险

- 首页缺乏运营能力
- 用户难以管理会话

#### 改造成本

- 低

#### 技术建议

- 增加会话排序规则
- 增加清未读、静音、删除等轻量能力
- 逐步补 last message cache

---

### P1-3 连接状态可视化

#### 为什么优先

消息系统必须让用户感知当前在线状态。

#### 不做风险

- 用户误以为消息已发出
- 问题排查困难

#### 改造成本

- 低

#### 技术建议

- 在首页和聊天页增加：
  - 连接中
  - 已连接
  - 重连中
  - 离线

---

### P1-4 拆分 ViewModel

#### 为什么优先

当前复杂度已经进入快速膨胀阶段。

#### 不做风险

- 功能越做越难维护
- 测试和回归成本快速增加

#### 改造成本

- 中

#### 技术建议

- 将消息发送、消息同步、文件传输、连接状态从 `ChatRoomViewModel` 中拆出

---

## 5.3 P2（增强体验）

### P2-1 撤回消息

- 为什么优先：IM 标配，增强体验
- 不做风险：产品成熟度不足
- 改造成本：中

### P2-2 消息编辑

- 为什么优先：增强型能力
- 不做风险：影响不大，但弱于主流 IM
- 改造成本：中

### P2-3 多端同步增强

- 为什么优先：适合移动端与桌面端协同推进
- 不做风险：跨端体验割裂
- 改造成本：高

### P2-4 多媒体体验完善

- 为什么优先：已有基础，适合第二阶段打磨
- 不做风险：多媒体功能可演示但不稳定
- 改造成本：中到高

---

## 6. 技术落地建议

## 6.1 消息状态设计

建议统一消息状态机，避免现在“枚举已有，但流转不完整”的问题。

### 建议状态

```kotlin
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
```

### 推荐流转

1. 客户端本地创建消息：`SENDING`
2. 服务端成功入库：`SENT`
3. 对端在线收到并确认：`DELIVERED`（可第二阶段实现）
4. 对端进入会话并已读：`READ`
5. 超时或重试失败：`FAILED`

### 现有代码贴合点

- 本地已有 `MessageStatus`
- UI 已有图标渲染
- 本地表中已有状态字段
- 服务端已有 `markConversationAsRead()`

### 改造成本

- 中

---

## 6.2 ACK 机制如何实现

### 推荐方案

不要把 Redis Stream 作为客户端 ACK 真值存储层。

建议职责：

- PostgreSQL：消息真值、已读真值
- Redis：跨节点路由、在线节点定位
- TCP：客户端实时确认回传

### 建议链路

1. 客户端发送消息，带 `clientMsgId`
2. 服务端保存消息后，生成 `msgId + sequenceId`
3. 服务端向发送者返回 ACK
4. 客户端按 `clientMsgId` 更新本地消息状态

### 示例思路

```java
Message saved = saveMessage(chatMessage);

AckMessage ack = AckMessage.newBuilder()
    .setConversationId(saved.getConversation().getConversationId())
    .setClientMsgId(saved.getClientMsgId())
    .setServerMsgId(saved.getMsgId())
    .setSequenceId(saved.getSequenceId())
    .setStatus(Chat.MessagesStatus.SENT)
    .build();
```

```kotlin
fun onAckReceived(ack: AckMessage) {
    chatMessageRepository.updateMessageByClientMsgId(
        clientMsgId = ack.clientMsgId,
        status = MessageStatus.SENT,
        msgId = ack.serverMsgId,
        sequenceId = ack.sequenceId
    )
}
```

### 当前代码问题点

- 客户端 ACK 分支已存在，但未真正执行业务更新
- 服务端 ACK 更偏已读回执，缺少发送成功确认回包

### 改造成本

- 中

---

## 6.3 ViewModel 拆分建议

### 当前问题

`ChatRoomViewModel` 已经集成过多职责，继续追加功能风险很高。

### 推荐拆分结构

- `ChatRoomViewModel`
  - 只负责页面状态聚合
- `MessageSendUseCase`
  - 负责文本、语音、文件发送
- `MessageSyncUseCase`
  - 负责增量同步、历史分页
- `FileTransferUseCase`
  - 负责上传、下载、失败重试
- `ConnectionStateStore`
  - 负责连接状态和恢复流程

### 示例思路

```kotlin
class ChatRoomViewModel(
    private val messageSendUseCase: MessageSendUseCase,
    private val messageSyncUseCase: MessageSyncUseCase
) : ViewModel() {

    fun sendText(content: String) = viewModelScope.launch {
        messageSendUseCase.sendText(currentConversationId(), content)
    }

    fun refresh() = viewModelScope.launch {
        val messages = messageSyncUseCase.refresh(currentConversationId())
        _uiState.update { it.copy(messages = messages) }
    }
}
```

### 改造成本

- 中

### 收益

- 降低回归风险
- 提高测试覆盖可能性
- 后续功能接入更清晰

---

## 6.4 TCP 重连策略

### 当前问题

- `AndroidSocketClient` 有自动重连
- `SenderSdk` 也有自动重连
- 两处都在控制连接状态

### 建议方案

统一成“一套连接状态机”。

### 推荐状态

- `DISCONNECTED`
- `CONNECTING`
- `CONNECTED`
- `RECONNECTING`
- `FAILED`

### 推荐流程

1. 登录后建立 TCP
2. 发送身份注册包
3. 心跳保活
4. 断线进入 `RECONNECTING`
5. 重连成功后执行恢复流程：
   - 重新注册身份
   - 同步最新消息
   - 重发离线消息

### 示例思路

```kotlin
suspend fun reconnectAndRecover() {
    state.value = ConnectionState.RECONNECTING
    tcpClient.connect(host, port)
    registerUser()
    syncLatestMessages()
    resendPendingMessages()
    state.value = ConnectionState.CONNECTED
}
```

### 改造成本

- 中

---

## 6.5 文件消息链路优化建议

### 当前问题

当前顺序是：

1. 发送 TCP 消息
2. 上传 HTTP 文件

这会导致接收方可能先收到消息，再发现文件不可用。

### 推荐方案

#### 方案 A：先上传，后发消息

- 优点：实现简单，体验稳定
- 缺点：发送感知略慢
- 改造成本：低到中

#### 方案 B：先发占位消息，上传成功后切换为可用状态

- 优点：即时感更强
- 缺点：实现复杂，需要更多状态管理
- 改造成本：中

### 当前阶段建议

优先采用方案 A。

原因：

- 更贴近当前现有代码
- 风险更低
- 更容易快速稳定上线

---

## 7. 产品规划结论

当前移动端 IM 并不是从 0 开始，而是已经具备：

- 会话列表
- 单聊消息
- 本地消息缓存
- 增量同步
- 文件上传下载骨架
- 语音消息骨架
- TCP 长连接与心跳重连

真正的问题不在“功能完全没有”，而在于：

- 链路没有闭环
- 状态没有统一
- 连接恢复不完整
- 文件消息一致性不稳定
- ViewModel 和职责边界开始失控

因此下一阶段目标不应是盲目堆功能，而应先补齐 P0 可靠性，再做 P1 产品化增强。

---

## 8. 最终 TODO List

- [P0] 实现消息 ACK 机制
  - 客户端：消费 `pkg.ack`，按 `clientMsgId` 更新本地消息状态为 `SENT`
  - 服务端：消息入库后给发送者返回 ACK，带 `msgId + sequenceId + clientMsgId`
  - 风险：若继续依赖消息回推替代 ACK，状态不稳定

- [P0] 实现离线消息重发
  - 客户端：补齐 `processOfflineMessages()`，按重试次数重发 `OfflineMessage`
  - 服务端：保证 `clientMsgId` 幂等，避免重复入库
  - 风险：断网恢复后消息悬挂或重复

- [P0] 打通已读回执
  - 客户端：进入会话后按 `conversationId + lastSequenceId` 发送 READ ACK
  - 服务端：批量更新消息为 READ，并推回发送方
  - 风险：未读数和已读状态失真

- [P0] 统一重连恢复流程
  - 客户端：只保留一套连接状态机，重连后执行注册、补拉、补发
  - 服务端：确保重连注册幂等
  - 风险：重连后消息断层

- [P1] 调整文件消息发送链路
  - 客户端：优先改为先上传后发消息
  - 服务端：确保 fileId 在消息可见时已可查询
  - 风险：文件消息无法打开或元数据不完整

- [P1] 增加连接状态 UI
  - 客户端：首页和聊天页展示连接中、重连中、离线
  - 服务端：无强依赖
  - 风险：用户无法判断消息是否真正发出

- [P1] 拆分 ChatRoomViewModel
  - 客户端：拆分发送、同步、文件、连接职责
  - 服务端：无改动
  - 风险：后续功能越做越难维护

- [P1] 强化会话列表产品能力
  - 客户端：增加排序规则、清未读、静音、删除等能力
  - 服务端：后续可补会话态接口
  - 风险：首页体验不足

- [P2] 实现撤回消息
  - 客户端：撤回入口与撤回态展示
  - 服务端：撤回接口与广播事件
  - 风险：产品成熟度不足

- [P2] 实现消息编辑
  - 客户端：编辑态和已编辑提示
  - 服务端：消息编辑接口与事件推送
  - 风险：弱于主流 IM 产品

- [P2] 增强多端同步
  - 客户端：同步已读游标、会话状态
  - 服务端：按账号/设备维护同步游标
  - 风险：移动端与桌面端体验割裂

- [P2] 完善多媒体体验
  - 客户端：上传进度、失败重试、预览发送、缩略图缓存
  - 服务端：补充媒体 meta 和稳定访问能力
  - 风险：多媒体功能停留在演示阶段

---

## 9. 建议的实施顺序

建议按以下顺序推进：

1. ACK 闭环
2. 离线重发
3. 已读回执
4. 重连恢复
5. 文件消息链路改造
6. ViewModel 拆分
7. 会话列表增强
8. 撤回 / 编辑 / 多端同步

这样可以先把“能不能稳定用”解决，再去做“像不像成熟 IM”。
