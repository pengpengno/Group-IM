# ADR-0005: Workbench Structured Card and Client Event Protocol

- Status: Accepted
- Date: 2026-08-19
- Related Issue: #14

## Context

Group-IM 当前已有多种消息语义：

- 普通 TEXT / FILE / IMAGE / MEDIA 等消息；
- MEETING 会议消息；
- `BOT_CARD` 机器人动作卡片。

`BOT_CARD` 的现有语义明确属于机器人/AI 动作交互，不能为了复用渲染器把 Task、Approval、Announcement 等 OA 业务塞进机器人协议。

Workbench 后续需要同时支持：

- Task assigned / status / due；
- Approval pending / result；
- Announcement published；
- Schedule reminder；
- 未来 Report 等领域。

这些事件有三种不同交付通道：

1. IM 历史消息卡片；
2. 应用内 ClientEvent / realtime refresh；
3. Push notification。

它们不能共享一份“无边界大 JSON”，也不能让客户端把卡片 payload 当成资源访问授权或当前状态真相。

另外，Group-IM 是 schema 多租户系统，同一个 `resourceId` 在不同公司 schema 中可能重复，因此 Workbench navigation 必须具备 tenant routing 信息，同时仍由服务端权限校验决定是否允许访问。

## Decision

### 1. 新增独立 `WORKBENCH` MessageType

Workbench/OA 使用新的消息类型：

```text
WORKBENCH
```

实现时必须追加到现有 Java / Proto enum 末尾，**不得重排或复用已有 enum number**。

不选择：

- 复用 `BOT_CARD`；
- 把结构化 OA JSON 塞入 `TEXT`；
- 首版定义无领域边界的 `APP_CARD` / `STRUCTURED_CARD` 总线。

理由：Workbench 已经是明确的业务边界，一个 `WORKBENCH` type + versioned category 足以承载 OA 领域，同时不会污染 Bot 或未来其他产品协议。

### 2. Message.content 保存 versioned Workbench Card JSON

首版协议：

```json
{
  "version": 1,
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

规则：

- `version` 必须存在；
- `resourceId` 使用 string，避免把协议锁死在 bigint；
- `companyId` 是 routing hint，不是 authorization proof；
- 不暴露数据库 `schemaName`；
- `title/summary/fallbackText` 是可展示的安全摘要；
- `status` 是**事件发生时的状态快照**，不是当前状态权威值；
- `occurredAt` 使用 UTC ISO-8601；
- 未知字段客户端必须忽略。

### 3. Workbench category 固定领域边界

V1 category：

```text
TASK
APPROVAL
ANNOUNCEMENT
SCHEDULE
REPORT
```

首版实际实现可以只启用 Task/Approval；未实现 category 不应提前发送。

`action` 由 category 定义，使用稳定 uppercase string，不依赖 UI 文案。

示例：

```text
TASK:
  ASSIGNED
  COMPLETED
  REOPENED

APPROVAL:
  PENDING
  APPROVED
  REJECTED
  RETURNED

ANNOUNCEMENT:
  PUBLISHED

SCHEDULE:
  CREATED
  UPDATED
```

普通高频状态变化不要求全部生成新的 IM 卡片；是否进入聊天历史由 notification matrix 决定。

### 4. IM 卡片是不可变历史事件，不是资源当前状态

已经持久化的 Workbench message 不通过后台更新来同步 Task/Approval 当前状态。

例如：

```text
10:00 ASSIGNED card -> status=TODO
11:00 user completes task
```

10:00 的历史卡片仍然表示“10:00 发生了指派事件”。客户端如果需要显示最新状态，可以在打开卡片/详情时请求当前资源 API。

这样避免：

- 修改历史聊天记录；
- IM 数据成为 Task/Approval 第二份写模型；
- 跨端缓存不同步；
- 资源权限变化后仍依赖旧 payload 做操作。

### 5. ClientEvent 使用一个 coarse Workbench event type

ClientEvent 扩展一个类型：

```text
WORKBENCH_RESOURCE_EVENT
```

不为每一个 Task/Approval action 无限增加顶层 ClientEvent enum。

ClientEvent payload 至少包含：

```json
{
  "version": 1,
  "category": "TASK",
  "action": "ASSIGNED",
  "resourceId": "901",
  "companyId": 42,
  "occurredAt": "2026-08-19T08:30:00Z",
  "deepLink": "group://workbench/task/901?companyId=42"
}
```

客户端按 `(category, action)` 做局部刷新、badge 更新和导航。

Notification Policy 也应基于 Workbench category/action 做策略判断，而不是通过不断新增 `ClientEventType` 模拟业务状态机。

### 6. Push payload 必须最小化

Push data 只携带路由和最小安全提示，不携带完整业务对象。

推荐 data：

```json
{
  "eventType": "WORKBENCH_RESOURCE_EVENT",
  "version": 1,
  "category": "APPROVAL",
  "action": "PENDING",
  "resourceId": "8801",
  "companyId": 42,
  "deepLink": "group://workbench/approval/8801?companyId=42"
}
```

Approval 等敏感业务默认 Push title/body 使用低敏摘要：

```text
你有新的待审批事项
```

而不是把金额、请假原因、附件名称等敏感内容放入锁屏通知。

完整详情只能在认证后的资源 API 中获取。

### 7. Deep Link 是路由提示，不是权限凭证

Canonical URI：

```text
group://workbench/task/{resourceId}?companyId={companyId}
group://workbench/approval/{resourceId}?companyId={companyId}
group://workbench/announcement/{resourceId}?companyId={companyId}
group://workbench/schedule/{resourceId}?companyId={companyId}
```

客户端处理顺序：

```text
parse target
  ↓
validate scheme/path/version
  ↓
if company differs -> use normal authenticated company switch flow
  ↓
fetch resource detail from server
  ↓
server validates tenant membership + data permission
  ↓
open detail
```

禁止：

- 客户端因为 URL 中有 companyId 就直接获得 tenant 权限；
- 通过 schemaName 切租户；
- 仅根据 card payload 执行 approve/complete 等写动作。

伪造 Deep Link 最多只能发起一次受权限保护的导航请求。

### 8. 未知 payload 必须安全降级

认识 `WORKBENCH` type 的客户端：

- 未知 `version` -> 显示 `fallbackText`，禁用业务动作；
- 未知 `category/action` -> 显示 `fallbackText`，允许“打开工作台”但不猜测详情路径；
- payload 解析失败 -> 显示“工作台消息”，不得 crash。

任何卡片按钮都只是进入受权限保护的业务 UI，不直接信任 payload 执行 server command。

### 9. 新 MessageType 上线必须采用 client-first rollout

老客户端可能完全不认识新的 Proto/MessageType number，因此不能假设 `fallbackText` 能拯救所有旧版本。

上线顺序固定为：

```text
Phase A: Web/Electron + Android 先支持 WORKBENCH + unknown-safe fallback
        ↓
Phase B: 验证支持版本已经达到可接受覆盖率 / 最低版本门禁
        ↓
Phase C: Server 开始实际发送 WORKBENCH messages
```

如果未来建立 device capability registry，可以按设备能力 gate；在此之前使用版本/发布门禁，不做双消息重复发送。

### 10. IM / Realtime / Push 分工

推荐 V1 matrix：

| Event | IM card | ClientEvent | Push |
| --- | --- | --- | --- |
| Task assigned | yes | yes | offline/policy |
| Task normal status changed | no by default | yes | no by default |
| Task due soon | no | yes | yes/policy |
| Approval pending | yes | yes | yes |
| Approval result | yes | yes | yes |
| Normal announcement | optional | yes | offline/policy |
| Urgent announcement | yes | yes | yes |
| Schedule reminder | no | yes | yes/policy |

具体 Push 是否发送继续复用 `NotificationPolicyService`，Workbench 不建立第二套在线状态判断。

## Consequences

### Positive

- Bot 和 OA 协议边界清晰；
- 一个 Workbench MessageType 可以扩展多个 OA domain；
- category/action 比无限增加顶层 enum 更稳定；
- tenant route 可消除跨公司 resourceId 歧义；
- 历史 IM 与当前业务状态不再双写；
- Push 不携带敏感完整业务数据；
- 新客户端具备未来 version/category 的安全降级路径。

### Cost

- 需要同步修改 Proto、Java enum、enum mapping、Web/Electron renderer、Android renderer；
- server emit 必须等待 client-first rollout；
- NotificationPolicyService 需要支持 Workbench category/action 上下文；
- Deep Link handler 必须处理 company switch + resource permission fetch。

## Alternatives Considered

### Reuse `BOT_CARD`

Rejected。机器人动作卡片和 OA domain 生命周期不同，长期会造成 renderer、权限和协议语义耦合。

### Generic `APP_CARD`

Rejected for V1。当前只有 Workbench 有明确需求，过早抽象为任意应用卡片会失去领域约束。未来如果多个独立产品都需要统一卡片协议，可以新增 ADR 将 Workbench schema 抽象为更通用 envelope。

### Separate MessageType for Task / Approval / Announcement

Rejected。会快速扩大协议 enum 和多端 switch 分支；一个 `WORKBENCH` envelope + category 足够。

### Put JSON inside `TEXT`

Rejected。会把结构化协议伪装成文本，破坏类型语义、渲染和兼容策略。

## Follow-ups

本 ADR 合并后拆分实现：

1. Server/Proto Workbench envelope + ClientEvent contract；
2. Web/Electron Workbench card renderer + Deep Link；
3. Android Workbench card renderer + Deep Link；
4. Task notification integration；
5. Approval notification integration。

每个实现 PR 必须同步更新 Workbench Feature Design 和 `doc/PROJECT_MASTER.md`。
