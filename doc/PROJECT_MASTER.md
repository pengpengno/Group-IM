# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：#45 Task Backend；#9 Electron/Web CI；#47 Task Web/Electron；#28 Workbench Protocol
- 当前交付：#29 Web/Electron Workbench Card + tenant-aware Deep Link
- 后续 emission blockers：#30 Android consumer；#50 WORKBENCH message storage/core evolution；supported-client rollout policy
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目原则与治理

Group-IM 是多租户组织协作 IM/OA 平台。消息是协作主链路，Workbench 承载结构化办公，AI/Automation 不建立第二份业务真相。

- `master` 是唯一开发主线；`main` 是 legacy；
- 所有变更通过 Issue/PR；每个 PR 更新本文；默认 Squash Merge；
- Workbench 权限禁止 `username == admin`；
- feature 写入必须明确目标 branch；
- merge gate：Repository Governance + applicable Backend + applicable Electron/Web + KMP + no unresolved review threads。

仍待治理：#6 master protection、#7 legacy `main` deploy trigger、#22 Maven duplicate dependencies。

---

## 2. Tenant Migration

```text
core business baseline = 2026081906
managed current target = 2026082002
```

当前事实：

- #12 versioned tenant migration foundation 完成；
- #43 将 immutable core baseline fingerprint 与 later managed object inventory 分离；
- no-history tenant extra object 仍 `CONFLICT`；
- Flyway-managed later tables/sequences 不污染 core baseline hash；
- new tenant：inactive → empty schema → Flyway current target → verify → active；
- existing reviewed tenant：preflight → explicit baseline 1906 → migrate current target → validate/audit；
- `2026082002` 增加 `wb_task / wb_task_assignee / wb_task_comment / wb_task_activity`；
- core baseline 1906 不随 managed target 前移。

### WORKBENCH message storage gate — #50

`2026081906` 的 `messages_type_check` 当前只允许既有 message types，不包含 WORKBENCH，而且该 CHECK 属于 pinned core constraint fingerprint。

#50 负责建立 versioned managed-core evolution，并在 actual WORKBENCH persistence/emission 前通过后续 immutable migration 扩展该 CHECK；未知手工 core ALTER 仍必须 fail closed。

因此：

```text
MessageType.WORKBENCH protocol recognition != WORKBENCH persistence enabled
```

---

## 3. Workbench Platform / Overview / Task

### #13 Platform Foundation — COMPLETED

`CurrentWorkContext`、fail-closed permission、Organization/File adapters、Audit、explicit tenant executor 已稳定。

### #39 Overview — COMPLETED

`GET /api/workbench/overview` 只使用 authenticated current company，response 不暴露 schemaName；Meeting 使用轻量 projection；Task 上线后接入真实 assigned/overdue/recent projection。

### #45 / PR #46 Task Backend — COMPLETED

Merge：`6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

能力：Task 四表 migration、create/update/list/detail、状态动作、assignee、comments、activity、resource permission、Audit、Overview Task projection。

### #47 / PR #49 Task Web/Electron — COMPLETED

Merge：`b590bbd6a18737b504d0c3876a8d12831e3efe15`。

Workbench shell 已提供真实 Overview + Task Center：create/list/detail/state actions/comment/activity。Task 是 Workbench 子视图，不扩散为 Dashboard 顶级 tab。

---

## 4. Electron/Web CI — #9 COMPLETED

PR #48 merge：`d5570e0145a549a4869118b957a059a857e0d6ee`。

Electron/Web 相关 PR 执行：

```text
npm ci
production desktop env
npm run app:build
npm run web:build
```

已在 #47 实际验证。

---

## 5. Workbench Protocol — #28 COMPLETED

PR #51 merge：`7f261e881bdc3eb3cebcd1d0468777f647d3ea43`。

已进入 master：

```text
Java MessageType.WORKBENCH
Proto WORKBENCH = 9 (append only)
Java <-> Proto MEETING / BOT_CARD / WORKBENCH mapping
WorkbenchCardEnvelope / WorkbenchEventEnvelope
WorkbenchEnvelopeValidator
WorkbenchDeepLinkFactory
WorkbenchCardSerializer
WorkbenchNotificationPolicyKey
ClientEventType.WORKBENCH_RESOURCE_EVENT
```

Proto 旧 wire number 固定不变；`MEDIA` 继续现有 TEXT fallback。

#28 **没有**启用 Task/Approval emission，也没有修改 `messages_type_check`。

---

## 6. Web/Electron Workbench Card + Deep Link — #29 CURRENT

实现文档：`doc/features/workbench/workbench-card-electron.md`。

### Parser / renderer

Web/Electron 增加独立 `WorkbenchMessageCard`，不复用 BOT_CARD renderer。

V1 parser：

- 只接受 version=1；
- known category/action；
- UUID / length / occurredAt 校验；
- canonical `group://workbench/...?...companyId=`；
- card category/resource/company 必须与 deep link 三元组一致；
- additive unknown JSON fields tolerant；
- malformed / unknown version/category/action / invalid contract 显示安全 fallback，不 crash、不执行 action。

### ChatRoom compatibility bridge

现有大 `ChatRoom.tsx` 不直接重写：

```text
ChatRoomLegacy.tsx = 原 ChatRoom blob，字节级不变
ChatRoom.tsx       = thin compatibility wrapper
```

wrapper 从 Redux 读取真实 `message.type`，只有 `WORKBENCH` message 的默认 text host 通过 React Portal 渲染 `WorkbenchMessageCard`。BOT_CARD / MEETING / file / voice 等仍由 legacy ChatRoom 原逻辑负责。

这是降低回归风险的过渡方案；未来 MessageBubble 拆组件后可删除 bridge。

### Tenant-aware navigation

`WorkbenchNavigationRuntime` 挂在 App 根部：

```text
card click
→ canonical deep-link validate
→ if company differs: verify authenticated membership
→ POST /api/company/switch/{companyId}
→ persist returned session
→ full renderer reload
→ consume pending deepLink
→ GET current Task detail
→ render read-only current Task detail
```

sessionStorage 只保存 reload 前后的短生命周期 pending deepLink，不保存业务真相。

`companyId` 只是正常 authenticated company switch 的目标，不是 schema/资源授权凭证。

### Resource authority

当前只有 Task 已有稳定 detail API。Task deep link 必须成功执行：

```text
GET /api/workbench/tasks/{taskId}
```

客户端才显示当前 Task。展示内容来自 detail response，不来自 immutable card snapshot。

Deep-link detail 面板没有 Complete/Approve 等写动作。退出公司、无权限、资源删除都由 server detail API fail closed 并显示 error notification。

尚未实现 detail UI 的其他 Workbench category 安全提示，不猜测业务状态。

---

## 7. Remaining Client-first Gates

### #30 Android/KMP — OPEN

需实现 V1 parser/model、Compose card、unknown/malformed fallback、tenant-aware deep link、authenticated company switch + resource fetch，以及 BOT_CARD/MEETING regression。

### #50 DB Storage / Managed Core Evolution — OPEN

需在 immutable 1906 adoption contract 不变的前提下，允许 Flyway history 证明的合法 core evolution，并最终通过新 migration 允许 `messages.type=WORKBENCH`。

### Actual emission — BLOCKED

只有在：

```text
#29 Web/Electron consumer
+ #30 Android consumer
+ #50 storage
+ supported-client/min-version rollout policy
```

全部满足后，才允许 Task/Approval 真实 WORKBENCH persistence/emission。禁止 TEXT + WORKBENCH 双写。

---

## 8. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Workbench Platform | STABLE | #13 | 领域复用 |
| Workbench Overview | STABLE | #39 + Task projection | events refresh |
| Tenant Migration | STABLE | baseline 1906 / target 2002 | #50 core evolution |
| Task Backend | STABLE | #45 / PR #46 | notification after gates |
| Task Web/Electron | STABLE | #47 / PR #49 | #29 card/deep-link |
| Electron/Web PR CI | STABLE | #9 / PR #48 | #6 required check |
| Workbench Protocol | STABLE | #28 / PR #51 | client consumers |
| Workbench Web Card | IN_PROGRESS | #29 parser/renderer/navigation | build/review/merge |
| Workbench Android Card | PLANNED | #30 open | after #29 |
| WORKBENCH Message Storage | BLOCKED | #50 open | before emission |
| OA Approval | PLANNED | design complete | after Task notification gates |

---

## 9. CI / Merge Gates

当前 checks：Repository Governance、Backend PR Validation、Electron Web PR Validation（applicable paths）、Build KMP APK、no unresolved review threads。

#6 仍负责配置 master required checks。

---

## 10. Roadmap

```text
#13 Platform Foundation ✅
→ #39 Overview API ✅
→ #43 Baseline Scope ✅
→ #45 Task Backend ✅
→ #9 Electron/Web CI ✅
→ #47 Task Web/Electron ✅
→ #28 Workbench Protocol ✅
→ #29 Web/Electron Card + Deep Link ← CURRENT
→ #30 Android Card + Deep Link
→ #50 WORKBENCH Message Storage / Managed Core Evolution
→ supported-client rollout gate
→ Task Realtime / Push / optional IM Card
→ Approval Backend / UI
```

---

## 11. Change Log

### 2026-08-20 — #29 Web/Electron Workbench Card

状态：IN_PROGRESS。独立 parser/renderer、ChatRoom compatibility bridge、tenant-aware App navigation runtime、server-refetched read-only Task deep-link detail 已进入 feature branch；server emission 仍禁用。

### 2026-08-20 — #28 / PR #51

状态：COMPLETED，merge `7f261e881bdc3eb3cebcd1d0468777f647d3ea43`。

### 2026-08-20 — #50 WORKBENCH storage gate

状态：OPEN。

### 2026-08-20 — #47 / PR #49

状态：COMPLETED，merge `b590bbd6a18737b504d0c3876a8d12831e3efe15`。

### 2026-08-20 — #9 / PR #48

状态：COMPLETED，merge `d5570e0145a549a4869118b957a059a857e0d6ee`。

### 2026-08-20 — #45 / PR #46

状态：COMPLETED，merge `6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
