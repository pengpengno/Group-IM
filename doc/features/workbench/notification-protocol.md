# Workbench Structured Card / ClientEvent / Push Protocol

> 状态：CURRENT DESIGN  
> Related Issue：#14  
> Architecture：ADR-0005

本文定义 Workbench 业务事件在 IM、应用内实时事件和 Push 三个通道中的协议边界。

---

## 1. 设计目标

协议需要同时满足：

- Task / Approval / Announcement / Schedule 等 OA domain 可扩展；
- 不复用 `BOT_CARD` 机器人语义；
- 不为每个业务动作无限增加顶层 MessageType / ClientEventType；
- 多租户 resourceId 不歧义；
- Deep Link 不成为权限凭证；
- Push 不泄露敏感详情；
- 旧/新客户端可以安全 rollout；
- IM 历史消息不成为业务当前状态第二份真相。

---

## 2. 三层协议，不共享一个大 DTO

虽然三种通道有共同目标字段，但不定义一个所有场景通用的超大 DTO。

```text
Domain Event
   ↓
WorkbenchTarget
   ├── IM WorkbenchCardEnvelope
   ├── ClientEvent WorkbenchEventEnvelope
   └── Push minimal data
```

共同 target：

```json
{
  "companyId": 42,
  "category": "TASK",
  "resourceId": "901"
}
```

`companyId` 只做 tenant routing hint；真正访问仍由认证上下文、公司成员关系和资源数据权限决定。

不发送数据库 `schemaName`。

---

## 3. Event identity

每个业务事件生成稳定 `eventId`：

```text
UUID string
```

同一业务事件进入 IM / ClientEvent / Push 时复用同一个 `eventId`。

用途：

- 客户端 realtime 去重；
- Push duplicate suppression；
- 日志关联；
- 后续 Outbox retry trace。

`eventId` 不是资源 ID，也不是幂等写 API 的授权 token。

---

## 4. IM Workbench Card Envelope V1

MessageType：

```text
WORKBENCH
```

`Message.content` JSON：

```json
{
  "version": 1,
  "eventId": "5a8da1a3-27ad-45af-aeb2-41205c1e1577",
  "category": "TASK",
  "action": "ASSIGNED",
  "resourceId": "901",
  "companyId": 42,
  "title": "任务已指派",
  "summary": "完成工作台接口",
  "fallbackText": "[任务] 完成工作台接口",
  "status": "TODO",
  "occurredAt": "2026-08-19T08:30:00Z",
  "deepLink": "group://workbench/task/901?companyId=42"
}
```

### Required

```text
version
eventId
category
action
resourceId
companyId
title
fallbackText
occurredAt
deepLink
```

### Optional

```text
summary
status
```

V1 不提供任意 `metadata` / `extra` object，避免不同 domain 无约束塞入敏感数据。以后出现稳定、跨端需要的新字段时采用可选字段演进或提升 protocol version。

---

## 5. 字段约束

建议服务端统一 validator：

| Field | Rule |
| --- | --- |
| version | integer, V1 = 1 |
| eventId | UUID string |
| category | stable uppercase enum string |
| action | stable uppercase enum string |
| resourceId | non-empty string, <= 128 |
| companyId | positive long |
| title | <= 120 characters |
| summary | optional, <= 300 characters |
| fallbackText | <= 300 characters |
| status | optional, <= 32 characters |
| occurredAt | UTC ISO-8601 instant |
| deepLink | server generated canonical URI |

客户端不能把用户输入直接拼成 Workbench envelope；卡片由服务端 Domain/Integration 层生成。

---

## 6. Category / Action

V1 categories：

```text
TASK
APPROVAL
ANNOUNCEMENT
SCHEDULE
REPORT
```

推荐首批 actions：

```text
TASK
  ASSIGNED
  COMPLETED
  REOPENED

APPROVAL
  PENDING
  APPROVED
  REJECTED
  RETURNED

ANNOUNCEMENT
  PUBLISHED

SCHEDULE
  CREATED
  UPDATED
```

未来新增 action 优先新增 string 值，不提升 envelope version；只有 envelope 结构/语义不兼容变化时才提升 version。

---

## 7. 卡片路由不自动创建 Conversation

`WORKBENCH` 是消息协议能力，不表示每个 Workbench 事件都必须写入聊天历史。

当前 Message 模型依赖 Conversation，因此：

- 如果 Task/Approval 已关联明确 conversation，并且消息接收者有该 conversation 权限，可发送 Workbench IM 卡片；
- 如果没有合法 conversation route，不自动创建私聊/群聊来承载 OA 卡片；
- 此时使用 Workbench Todo / ClientEvent / Push；
- 如果未来设计“系统通知会话/工作通知 Feed”，必须独立 Issue/ADR 决定。

禁止为了 notification 副作用隐式改变用户的聊天关系。

---

## 8. ClientEvent V1

新增 coarse type：

```text
WORKBENCH_RESOURCE_EVENT
```

payload：

```json
{
  "version": 1,
  "eventId": "5a8da1a3-27ad-45af-aeb2-41205c1e1577",
  "category": "TASK",
  "action": "ASSIGNED",
  "resourceId": "901",
  "companyId": 42,
  "occurredAt": "2026-08-19T08:30:00Z",
  "deepLink": "group://workbench/task/901?companyId=42"
}
```

不把 `title/summary/status` 等完整卡片字段无条件复制到 ClientEvent。

客户端收到事件后可：

- invalidate Workbench Overview；
- 增量更新 badge；
- 刷新当前 resource detail；
- 展示应用内 toast；
- 根据用户动作执行 Deep Link。

不要直接根据 event payload 修改服务端业务状态。

---

## 9. Notification Policy key

Workbench Push/提醒策略使用：

```text
(category, action)
```

作为业务 policy key，而不是不断扩展顶层 `ClientEventType`。

示例：

```text
(TASK, ASSIGNED)       -> offline/policy push
(TASK, DUE_SOON)       -> push
(TASK, STATUS_CHANGED) -> default no push
(APPROVAL, PENDING)    -> push
(APPROVAL, APPROVED)   -> push
```

现有在线状态和 Push decision 继续经过 `NotificationPolicyService`，Workbench 不自己查询“用户是否在线”。

---

## 10. Push 最小化

Push data：

```json
{
  "eventType": "WORKBENCH_RESOURCE_EVENT",
  "version": 1,
  "eventId": "5a8da1a3-27ad-45af-aeb2-41205c1e1577",
  "category": "APPROVAL",
  "action": "PENDING",
  "resourceId": "8801",
  "companyId": 42,
  "deepLink": "group://workbench/approval/8801?companyId=42"
}
```

不进入 Push data：

- Approval form data；
- 金额；
- 请假原因；
- 附件名/URL；
- Task 完整 description；
- 参与人列表；
- tenant schemaName。

敏感场景 notification body 使用通用低敏文案，例如：

```text
你有新的待审批事项
```

---

## 11. Deep Link Contract

```text
group://workbench/task/{resourceId}?companyId={companyId}
group://workbench/approval/{resourceId}?companyId={companyId}
group://workbench/announcement/{resourceId}?companyId={companyId}
group://workbench/schedule/{resourceId}?companyId={companyId}
```

### Client flow

```text
URI parse
  ↓
validate scheme/category/resourceId/companyId
  ↓
if current company differs
  -> authenticated company switch flow
  ↓
GET resource detail
  ↓
server tenant + data permission check
  ↓
open resource UI
```

如果：

- user 已离开公司；
- resource 已删除；
- user 无数据权限；
- companyId 被伪造；

则服务端 detail API 返回受控错误，客户端显示“内容不可访问或已失效”。

禁止根据卡片缓存内容绕过 detail API 执行写动作。

---

## 12. Immutable history + current resource

WorkBench 卡片保存的是事件快照：

```text
ASSIGNED at 10:00 / status TODO
```

如果 11:00 Task 已完成：

- 10:00 的聊天卡片仍代表 10:00 的历史事件；
- 卡片打开后详情显示当前 COMPLETED；
- 不后台 rewrite 10:00 message；
- ClientEvent 用于通知当前页面刷新。

这样 IM 不成为 Task/Approval 写模型复制品。

---

## 13. Unknown / malformed payload fallback

认识 `WORKBENCH` 的客户端必须：

### Unknown version

- 不执行 action；
- 显示 `fallbackText`；
- 可提供“打开工作台”通用入口。

### Unknown category/action

- 不猜测业务 UI；
- 显示 fallback；
- 不 crash。

### Malformed JSON

- 显示“工作台消息”；
- 记录 telemetry/log；
- 不暴露 raw JSON 给普通用户。

### Unknown future Proto message type

新客户端的 generic unknown-message fallback 应尽量显示安全占位，减少未来再扩 enum 的升级风险。

---

## 14. Rollout Order

新的 `WORKBENCH` MessageType 有老客户端兼容风险，因此固定 client-first：

```text
1. Proto/Java contract + clients understand WORKBENCH
2. Web/Electron renderer + unknown-safe fallback
3. Android renderer + unknown-safe fallback
4. Deep Link company-aware routing
5. verify supported-version coverage / minimum-version gate
6. server feature flag / rollout allows actual WORKBENCH emission
```

在第 6 步前，服务端不能因为协议代码已存在就开始向老客户端发送新 enum。

不采用“同时发 TEXT + WORKBENCH 两条消息”的双写方案，避免聊天历史重复。

---

## 15. 推荐 Delivery Matrix

| Domain event | IM | ClientEvent | Push |
| --- | --- | --- | --- |
| Task assigned | if legal conversation route | yes | offline/policy |
| Task normal status change | no by default | yes | no by default |
| Task due soon | no | yes | yes/policy |
| Approval pending | if legal conversation/system route | yes | yes |
| Approval result | if legal conversation/system route | yes | yes |
| Normal announcement | optional | yes | offline/policy |
| Urgent announcement | if legal route | yes | yes |
| Schedule reminder | no | yes | yes/policy |

`IM = yes` 不是“自动创建聊天关系”。

---

## 16. Server implementation boundary

后续 Server PR 建议新增：

```text
WorkbenchCardEnvelope
WorkbenchEventEnvelope
WorkbenchTarget
WorkbenchCategory
WorkbenchCardAction / category-specific action validation
WorkbenchCardSerializer
WorkbenchDeepLinkFactory
WorkbenchNotificationPolicyKey
WorkbenchClientEventPublisher
```

Integration 层接受 domain event，不允许 Controller 自己拼 JSON。

推荐：

```text
Task domain transaction
  ↓ AFTER_COMMIT
TaskAssignedEvent
  ↓
Workbench integration
  ├─ ClientEvent
  ├─ Push policy
  └─ optional IM card if legal route exists
```

后续升级 Outbox 时保持 envelope contract 不变。

---

## 17. Client implementation boundary

### Web/Electron

```text
WorkbenchCardRenderer
WorkbenchDeepLinkHandler
UnknownWorkbenchCard
```

### Android

```text
WorkbenchMessageCard
WorkbenchDeepLinkRouter
UnknownWorkbenchCard
```

业务 action button 只导航到 detail/action UI，不直接从 MessageBubble 发 approve/complete command。

---

## 18. Testing

### Contract

- V1 serialize/deserialize；
- unknown field tolerant；
- unknown version fallback；
- malformed JSON fallback；
- size/length validation；
- eventId UUID validation；
- deepLink generation。

### Security

- forged companyId 无法越权；
- user removed from company 无法读取；
- approval push 不含 form data；
- raw schemaName 不进入 envelope；
- card不能绕过 resource API 写状态。

### Compatibility

- existing BOT_CARD rendering 不回归；
- existing MEETING rendering 不回归；
- enum number 不重排；
- clients understand unknown Workbench category safely。

---

## 19. 实现拆分

ADR/本设计合并后建议独立 Issue：

1. Server/Proto Workbench envelope + ClientEvent contract；
2. Web/Electron renderer + Deep Link；
3. Android renderer + Deep Link；
4. Task notification integration；
5. Approval notification integration。

禁止一个 PR 同时修改完整协议、两个客户端、Task 和 Approval 业务逻辑。
