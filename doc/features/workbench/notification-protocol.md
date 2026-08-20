# Workbench Structured Card / ClientEvent / Push Protocol

> 状态：IMPLEMENTING / CANONICAL  
> Related Issue：#14 / #28 / #29 / #30 / #50  
> Architecture：ADR-0005

本文定义 Workbench 业务事件在 IM、应用内实时事件和 Push 三个通道中的协议边界。

## 1. 当前实现状态

#28 提供协议基础，但**不启用任何 Task / Approval 实际发射**：

- Java `MessageType.WORKBENCH` append；
- Proto `MessageType.WORKBENCH = 9` append，不重排旧 wire number；
- Java ↔ Proto mapping；
- `ClientEventType.WORKBENCH_RESOURCE_EVENT`；
- `WorkbenchCategory / Target / CardEnvelope / EventEnvelope`；
- V1 serializer / validator / canonical deep-link factory；
- notification policy key `(category, action)`；
- unknown JSON field tolerant deserialization；
- UUID / length / category / action / tenant-aware deep-link validation。

当前 rollout gates：

```text
#28 protocol foundation
  ↓
#29 Web/Electron parser + renderer + deep link
  ↓
#30 Android parser + renderer + deep link
  ↓
#50 WORKBENCH message storage / managed-core evolution
  ↓
minimum supported-client / rollout policy
  ↓
actual Task/Approval emission
```

在所有 gate 完成前，服务端不能因为 `WORKBENCH` enum 已存在就开始持久化或发送该消息类型。

---

## 2. 设计目标

协议必须同时满足：

- Task / Approval / Announcement / Schedule / Report 可扩展；
- `WORKBENCH` 与机器人 `BOT_CARD` 语义严格分离；
- 不为每个业务动作无限增加顶层 MessageType / ClientEventType；
- 多租户 resourceId 不歧义；
- Deep Link 不成为权限凭证；
- Push 不泄露敏感详情；
- 旧/新客户端可以 client-first rollout；
- IM 历史消息不成为业务当前状态第二份真相。

---

## 3. 三层协议

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

`companyId` 只做 tenant routing hint。真正访问仍由 authenticated current company、公司成员关系和资源数据权限决定。

禁止进入协议：

```text
schemaName
permission token
raw Approval form data
Task full sensitive description in Push
```

---

## 4. Event identity

每个业务事件生成稳定 UUID `eventId`。同一业务事件进入 IM / ClientEvent / Push 时复用同一个 `eventId`，用于去重、日志关联和未来 Outbox retry trace。

`eventId` 不是资源 ID，也不是写 API 授权 token。

---

## 5. IM Workbench Card Envelope V1

Java / Proto message type：

```text
WORKBENCH
```

Proto wire number：

```text
TEXT      = 0
FILE      = 1
VIDEO     = 3
VOICE     = 4
IMAGE     = 6
MEETING   = 7
BOT_CARD  = 8
WORKBENCH = 9   # append only
```

旧编号是 wire compatibility contract，禁止重排。

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

Required：

```text
version / eventId / category / action / resourceId / companyId
/ title / fallbackText / occurredAt / deepLink
```

Optional：

```text
summary / status
```

V1 不提供任意 metadata/extra object，避免不同 domain 无约束塞入敏感数据。

---

## 6. 字段约束

| Field | Rule |
| --- | --- |
| version | integer, V1 = 1 |
| eventId | UUID string |
| category | stable uppercase enum |
| action | stable uppercase string |
| resourceId | non-empty, <= 128 |
| companyId | positive long |
| title | <= 120 |
| summary | optional, <= 300 |
| fallbackText | <= 300 |
| status | optional, <= 32 |
| occurredAt | UTC Instant |
| deepLink | server-generated canonical URI |

客户端不能把用户输入直接拼成 envelope；卡片由 server Domain/Integration 层生成。

---

## 7. Category / Action

V1 categories：

```text
TASK
APPROVAL
ANNOUNCEMENT
SCHEDULE
REPORT
```

推荐 actions：

```text
TASK: ASSIGNED / COMPLETED / REOPENED
APPROVAL: PENDING / APPROVED / REJECTED / RETURNED
ANNOUNCEMENT: PUBLISHED
SCHEDULE: CREATED / UPDATED
```

新增稳定 action 通常不提升 envelope version；结构/语义不兼容变化才提升 version。

---

## 8. ClientEvent V1

coarse event type：

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

客户端收到后只做 invalidate / badge refresh / resource refresh / safe toast / navigation，不直接据此修改业务状态。

---

## 9. Notification policy

Workbench policy key：

```text
(category, action)
```

示例：

```text
(TASK, ASSIGNED)
(TASK, DUE_SOON)
(APPROVAL, PENDING)
(APPROVAL, APPROVED)
```

在线状态、免打扰和 Push decision 继续复用现有 Notification Policy，不在 Workbench 内另建在线判断。

---

## 10. Push 最小化

Push data 只包含定位资源需要的低敏字段：

```text
eventType / version / eventId / category / action
/ resourceId / companyId / deepLink
```

不进入 Push data：

- Approval form data；
- 金额/请假原因；
- 附件 URL；
- Task 完整 description；
- 参与人列表；
- schemaName。

敏感 notification body 使用通用低敏文案。

---

## 11. Deep Link contract

```text
group://workbench/task/{resourceId}?companyId={companyId}
group://workbench/approval/{resourceId}?companyId={companyId}
group://workbench/announcement/{resourceId}?companyId={companyId}
group://workbench/schedule/{resourceId}?companyId={companyId}
```

Client flow：

```text
parse + validate URI
  ↓
if company differs -> normal authenticated company switch
  ↓
GET resource detail
  ↓
server tenant + data-permission check
  ↓
open current resource UI
```

伪造 companyId、退出公司、无权限、资源删除都必须由 server detail API fail closed。

禁止根据 card snapshot 绕过 detail API 直接执行 approve/complete 等写动作。

---

## 12. Immutable event snapshot

Workbench card 保存事件发生时的快照，不是业务当前状态副本。

例如 10:00 `ASSIGNED/TODO`，11:00 Task 已完成：

- 10:00 卡片仍代表历史事件；
- 点击后 detail API 返回当前 COMPLETED；
- 不后台 rewrite 历史 IM message；
- ClientEvent 负责提示当前页面刷新。

---

## 13. Unknown / malformed fallback

认识 WORKBENCH 的客户端必须：

- unknown version：显示 fallback，不执行 action；
- unknown category/action：显示 fallback，不猜业务 UI；
- malformed JSON：显示安全“工作台消息”占位，不 crash；
- future unknown message type：尽量使用 generic unknown-message fallback。

---

## 14. Conversation routing

`WORKBENCH` 协议存在不等于每个 OA 事件必须写 IM。

- 有明确、合法 conversation route 时可选 IM card；
- 没有 route 时不自动创建私聊/群聊；
- 使用 Workbench Todo / ClientEvent / Push；
- “系统通知会话/工作通知 Feed”需要独立设计。

禁止 notification 副作用隐式改变聊天关系。

---

## 15. Storage gate — #50

当前 tenant `2026081906` baseline 中 `messages_type_check` 只允许：

```text
TEXT / FILE / VOICE / VIDEO / IMAGE / MEDIA / MEETING / BOT_CARD
```

它是 core baseline fingerprint 的一部分。直接 ALTER 加 WORKBENCH 会让 #43 current core fingerprint 产生合法但不可区分的 constraint drift。

因此 #28 **只建立协议识别能力，不声明 WORKBENCH 已可持久化**。

#50 必须在 actual emission 前完成：

1. 保持 `2026081906` no-history adoption contract immutable；
2. 建立 versioned managed-core evolution 校验；
3. 通过后续 immutable Flyway migration 扩展 `messages_type_check`；
4. PostgreSQL tests 证明合法 migration 不误报 drift，未知手工 ALTER 仍 fail closed。

在 #50 完成前：

```text
Java enum can exist
Proto enum can exist
Serializer/event contract can exist
BUT Message.type=WORKBENCH must not be persisted or emitted
```

---

## 16. Rollout order

固定 client-first：

```text
1. #28 Java/Proto/envelope/event contract
2. #29 Web/Electron renderer + unknown-safe fallback + tenant-aware deep link
3. #30 Android renderer + unknown-safe fallback + tenant-aware deep link
4. #50 DB storage + managed-core evolution gate
5. verify supported-client coverage / minimum-version policy
6. server feature flag / rollout allows actual WORKBENCH emission
```

不采用同时发 TEXT + WORKBENCH 两条消息的双写方案。

---

## 17. Delivery matrix

| Domain event | IM | ClientEvent | Push |
| --- | --- | --- | --- |
| Task assigned | legal conversation route only | yes | offline/policy |
| Task normal status change | default no | yes | default no |
| Task due soon | no | yes | yes/policy |
| Approval pending | legal route only | yes | yes |
| Approval result | legal route only | yes | yes |
| Announcement | optional/legal route | yes | policy |
| Schedule reminder | no | yes | yes/policy |

`IM=yes` 永远不表示自动创建聊天关系。

---

## 18. Testing

Protocol / compatibility：

- V1 serialize/deserialize；
- unknown JSON fields tolerant；
- invalid version/eventId/deepLink rejected；
- existing Proto numbers pinned；
- MEETING/BOT_CARD bidirectional mapping regression；
- WORKBENCH Java ↔ Proto mapping；
- no domain emission enabled。

Client follow-ups：

- unknown version/category/action safe fallback；
- malformed JSON safe fallback；
- forged companyId cannot bypass detail API；
- cross-company deep link uses authenticated switch；
- existing BOT_CARD / MEETING rendering remains intact。

Storage follow-up #50：

- no-history baseline hashes unchanged；
- managed core evolution accepted only when justified by Flyway history；
- WORKBENCH type storage allowed only after migration；
- manual core constraint drift still rejected。
