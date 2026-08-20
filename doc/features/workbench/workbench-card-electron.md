# Workbench Card + Deep Link — Web/Electron

> 状态：IMPLEMENTING  
> Issue：#29  
> Protocol：#28 / PR #51  
> Storage gate：#50

## 1. 目标

Web/Electron 在**服务端尚未启用 WORKBENCH emission** 的前提下，先具备安全的 V1 structured-card 消费能力：

```text
WORKBENCH message
→ parse / validate
→ render independent card
→ click "打开工作台"
→ validate canonical deep link
→ authenticated company switch if needed
→ reload tenant-scoped client state
→ GET current resource detail
→ render current server truth
```

卡片是 immutable event snapshot，不是当前 Task 状态，也不是授权凭证。

## 2. Message rendering

`MessageType` 增加客户端识别：

```text
BOT_CARD
WORKBENCH
```

WORKBENCH 不复用 BOT_CARD renderer。

现有 `ChatRoom.tsx` 很大且承载稳定 IM 行为。本 PR 使用兼容 bridge：

```text
ChatRoomLegacy.tsx = existing ChatRoom blob, byte-for-byte unchanged
ChatRoom.tsx       = thin wrapper
```

wrapper 根据 Redux 中实际 `message.type == WORKBENCH`，仅把该 message row 的默认 text host 通过 React Portal 渲染为 `WorkbenchMessageCard`。BOT_CARD、MEETING、文件、语音等全部继续由 legacy ChatRoom 原逻辑处理。

这是过渡性低风险接法；未来 ChatRoom 拆分 MessageBubble 独立组件后，可把 Workbench renderer 改为正常 switch 分支并删除 bridge。

## 3. Parser / fallback

`workbenchCard.ts`：

- 只接受 V1；
- category：TASK / APPROVAL / ANNOUNCEMENT / SCHEDULE / REPORT；
- action 必须是客户端已知稳定 action；
- eventId 必须 UUID；
- title/summary/fallback/status/resourceId 长度限制对齐 server；
- occurredAt 必须可解析；
- companyId 必须正整数；
- deepLink 必须 canonical，并与 category/resource/company 三元组完全一致；
- extra JSON fields 自动忽略，支持 additive server fields。

Fallback：

- malformed JSON：安全“工作台消息”占位；
- unknown version：fallbackText；
- unknown category/action：fallbackText；
- invalid contract/deep-link mismatch：fallbackText；
- fallback 不执行任何 navigation/action。

## 4. Deep Link

Canonical：

```text
group://workbench/task/{resourceId}?companyId={companyId}
```

以及 approval/announcement/schedule/report 对应 path。

客户端拒绝：

- 非 `group://workbench`；
- 多余 path segment；
- 非正整数 companyId；
- 未知 query parameter；
- category path 未知；
- resourceId 为空/过长；
- 非 canonical 编码；
- card target 与 deepLink target 不一致。

## 5. Tenant switch

`WorkbenchNavigationRuntime` 挂在 App 根部，不改 Dashboard 全局 tab 逻辑。

跨公司：

```text
validate target
→ verify company exists in authenticated user.companies
→ sessionStorage pending deepLink
→ POST /api/company/switch/{companyId}
→ persist returned authenticated session via loginSuccess
→ full renderer reload
→ consume pending deepLink
```

full reload 继续复用现有公司切换后清空 tenant-scoped chat/client state 的行为。

`companyId` 只用于正常 authenticated company switch；它不会直接决定数据库 schema 或资源授权。

## 6. Resource authorization

当前只有 Task 具备已完成的 Workbench detail UI/API。

同公司或切换完成后：

```text
GET /api/workbench/tasks/{taskId}
```

只有该请求成功后，客户端才展示 Task 当前详情。

展示数据来自 detail response，不来自 card title/status/summary。

卡片/Deep Link 页面**没有 Complete / Approve 等写动作**。用户要修改 Task，继续通过正常 Task Center 和后端权限/状态机完成。

退出公司、无资源权限、资源删除：detail API fail closed，客户端显示 error notification，不展示 card snapshot 冒充当前资源。

尚未实现 detail UI 的 Approval/Announcement/Schedule/Report deep link：安全提示“当前客户端尚未提供此类详情页”，不猜数据。

## 7. Client state

`sessionStorage` 只保存 reload 前后的 pending deepLink，属于短生命周期 navigation handoff，不是业务状态缓存。

server 始终是当前资源唯一真相。

## 8. Rollout gate

本 PR 完成后仍不能启动 server WORKBENCH emission：

```text
#28 protocol ✅
#29 Web/Electron consumer ← current
#30 Android consumer
#50 message storage / managed core evolution
supported-client rollout policy
actual emission
```

## 9. Validation

Merge 前必须：

- Electron main/renderer build；
- Web production bundle；
- KMP regression；
- Repository Governance；
- no unresolved review threads。

同时人工/代码审查确认：

- BOT_CARD / MEETING legacy blob 未修改；
- WORKBENCH renderer 独立；
- malformed/unknown fallback safe；
- cross-company path 只走 authenticated switch；
- Task detail 始终重新 fetch server。
