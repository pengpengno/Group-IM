# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-24
- 唯一开发主线：`master`
- 当前 `master`：`eb332edafa68c2ab8a73a62f345742a7f61ddb42`
- 最近完成：#45 Task Backend、#47 Task Web/Electron、#28 Workbench Protocol、#29 Web/Electron Workbench Card + Deep Link
- **当前交付：#30 Android/KMP Workbench Card + tenant-aware Deep Link**
- **下一阻塞项：#50 WORKBENCH Message Storage / managed core evolution**
- 后续：#54 supported-client rollout gate → #55 Task Realtime/Push/WORKBENCH notifications → #56 Approval Backend V1
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目原则与治理

Group-IM 是多租户组织协作 IM/OA 平台。消息是协作主链路，Workbench 承载结构化办公，AI/Automation 不建立第二份业务真相。

核心规则：

- `master` 是唯一开发主线；`main` 是 legacy；
- 所有仓库变更必须 `Issue -> Branch -> PR -> CI -> PROJECT_MASTER -> Merge`；
- 默认 Squash Merge；
- Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么；
- Workbench 权限禁止 `username == admin`；
- tenant/resource 权限必须 fail closed；
- client payload 中的 `companyId` 只是路由提示，不是授权证明；
- feature 写入必须显式指定目标 branch；
- merge gate：Repository Governance + applicable Backend + applicable Electron/Web + KMP + no unresolved review threads。

仍待治理：#6 master protection、#7 legacy `main` deploy trigger、#22 Maven duplicate dependencies。

---

## 2. 当前完成度总览

| 阶段 | Issue / PR | 状态 | 当前事实 |
| --- | --- | --- | --- |
| Workbench 正式设计 | #10 / PR #11 | COMPLETED | Task / Approval / platform / roadmap 已固定 |
| Tenant Versioned Migration | #12 | COMPLETED | Flyway 成为 tenant DDL authority |
| Workbench Platform Foundation | #13 / PR #38 | COMPLETED | context / permission / audit / adapters / tenant executor |
| Overview API | #39 / PR #40 | COMPLETED | authenticated company + Meeting + Task projection |
| Baseline scope compatibility | #43 / PR #44 | COMPLETED | immutable core fingerprint 与 later managed objects 分离 |
| Task Backend | #45 / PR #46 | COMPLETED | Task 四表、状态机、权限、Activity/Audit、Overview projection |
| Electron/Web PR CI | #9 / PR #48 | COMPLETED | renderer + web build gate |
| Task Web/Electron | #47 / PR #49 | COMPLETED | Workbench 内 Task Center 核心闭环 |
| Structured OA Card design | #14 / PR #31 | COMPLETED | ADR-0005 / client-first policy |
| WORKBENCH Protocol | #28 / PR #51 | COMPLETED | Java/Proto/envelope/event/deep-link contract |
| Web/Electron Card + Deep Link | #29 / PR #52 | COMPLETED | safe renderer + tenant-aware navigation + server re-fetch |
| Android/KMP Card + Deep Link | #30 | **CURRENT** | branch `feature/30-workbench-card-deeplink` 已建立，正在实现 |
| WORKBENCH Storage | #50 | NEXT BLOCKER | 合法 managed-core evolution + `messages.type=WORKBENCH` |
| Supported-client rollout | #54 | QUEUED | minimum version / feature gate / rollback policy |
| Task Notification | #55 | BLOCKED | 等 #30 + #50 + #54 后开启 actual emission |
| Approval Backend | #56 | QUEUED | Task 纵向闭环完成后进入下一领域模块 |

---

## 3. Tenant Migration 当前契约

```text
core business baseline = 2026081906
managed current target = 2026082002
```

当前事实：

- #12 versioned tenant migration foundation 已完成；
- core baseline `2026081906` 是 immutable adoption contract；
- `2026082002` 已加入 `wb_task / wb_task_assignee / wb_task_comment / wb_task_activity`；
- #43 将 core fingerprint 与 later managed object inventory 分离；
- no-history tenant 出现 extra object 仍必须 `CONFLICT`；
- Flyway-managed 后续业务表/sequence 不污染 1906 core baseline hash；
- new tenant：inactive company → empty schema → Flyway current target → verify → active；
- existing reviewed tenant：preflight → explicit baseline 1906 → migrate target → validate/audit；
- Safe Sync / public clone 只保留 deprecated/transitional compatibility，不再是正式 provisioning/release authority；
- 测试 tenant 数据可以清理重建，不作为 Workbench 功能开发阻塞项。

### #50 — WORKBENCH message storage / managed core evolution

`2026081906` 的 `messages_type_check` 属于 pinned core constraint fingerprint，目前不允许 `WORKBENCH`。

#50 必须同时满足：

1. 不修改历史 migration `V2026081901..1906`；
2. no-history 1906 tenant 仍按原 contract adoption；
3. Flyway history 可证明后续合法 core evolution；
4. unknown/manual core ALTER 仍 fail closed；
5. 新 immutable migration 扩展 `messages_type_check` 允许 `WORKBENCH`；
6. provisioning/baseline/Testcontainers target 同步前移。

因此：

```text
MessageType.WORKBENCH protocol recognition
!= WORKBENCH persistence enabled
!= WORKBENCH server emission enabled
```

---

## 4. Workbench Platform / Overview / Task

### Platform Foundation — STABLE

#13 已提供：

- `CurrentWorkContext`；
- fail-closed permission boundary；
- Workbench Audit；
- Organization / File adapters；
- explicit background tenant executor。

后续 Task/Approval 必须复用这些平台能力，不建立领域私有 tenant/security 框架。

### Overview — STABLE

`GET /api/workbench/overview`：

- 只使用 authenticated current company；
- response 不暴露 schemaName；
- Meeting 使用轻量 projection；
- 已接入真实 Task assigned/overdue/recent projection；
- 后续 Approval 上线后继续接 pending projection。

### Task Backend — STABLE

#45 / PR #46，merge：`6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

能力：

- Task 四表 migration；
- create/update/list/detail；
- TODO / IN_PROGRESS / BLOCKED / COMPLETED / CANCELLED 状态机；
- assignee / comment / activity；
- creator/owner/collaborator/watcher 资源级权限；
- Activity + Workbench Audit；
- Overview Task projection；
- PostgreSQL Testcontainers / migration regression。

### Task Web/Electron — STABLE

#47 / PR #49，merge：`b590bbd6a18737b504d0c3876a8d12831e3efe15`。

Workbench shell 已提供真实 Overview + Task Center：create/list/detail/state action/comment/activity。

Task 是 Workbench 子视图，不扩散为 Dashboard 顶级 tab；客户端不保存 Task 第二份业务真相。

---

## 5. Workbench Structured Notification Contract

### #14 Structured OA Card / ClientEvent Protocol — COMPLETED

ADR-0005 固定：

- 独立 `WORKBENCH` MessageType，不复用 BOT_CARD；
- versioned Workbench Card JSON；
- stable `eventId` UUID；
- `category + action + resourceId + companyId` target；
- `companyId` 只是 routing hint；
- client payload 不包含 schemaName；
- card 是 immutable historical event snapshot，不是当前业务状态；
- ClientEvent 使用粗粒度 `WORKBENCH_RESOURCE_EVENT`；
- Push 只携带低敏感、最小 routing 信息；
- canonical tenant-aware `group://workbench/...?...companyId=` Deep Link；
- client-first rollout；
- 禁止为了兼容老客户端进行 TEXT + WORKBENCH 双写。

### #28 WORKBENCH Protocol — COMPLETED

PR #51 merge：`7f261e881bdc3eb3cebcd1d0468777f647d3ea43`。

进入 master：

```text
Java MessageType.WORKBENCH
Proto WORKBENCH = 9 (append only)
Java <-> Proto mapping
WorkbenchCardEnvelope / WorkbenchEventEnvelope
WorkbenchEnvelopeValidator
WorkbenchDeepLinkFactory
WorkbenchCardSerializer
WorkbenchNotificationPolicyKey
ClientEventType.WORKBENCH_RESOURCE_EVENT
```

旧 Proto wire numbers 固定不变；#28 没有开启 persistence/emission。

### #29 Web/Electron Card + Deep Link — COMPLETED

PR #52 merge：`eb332edafa68c2ab8a73a62f345742a7f61ddb42`。

已完成：

- independent `WorkbenchMessageCard`；
- V1 safe parser；
- unknown/malformed safe fallback；
- BOT_CARD / MEETING 旧渲染保持；
- tenant-aware company switch；
- reload 后恢复 pending Deep Link；
- Task detail 必须重新从 server fetch；
- forbidden/deleted/left-company fail closed；
- card/deep-link 不直接执行 Complete/Approve 等写命令。

当前 Web/Electron consumer 已 READY，但 server emission 仍关闭。

---

## 6. CURRENT — #30 Android/KMP Workbench Card + Deep Link

Issue：#30  
Branch：`feature/30-workbench-card-deeplink`

当前已确认的实现边界：

- KMP `MessageType` 需要增加 `WORKBENCH`，否则 Proto `WORKBENCH` 在 `MessageType.valueOf(message.type.name)` 阶段会先于 Compose renderer 抛错；
- `MessageBubble.kt` 已有正常 renderer 边界，可直接新增独立 WORKBENCH 分支，不需要 Electron 的 ChatRoom bridge；
- 必须独立 Workbench parser/card，不复用 BOT_CARD；
- unknown/malformed payload 安全 fallback；
- company switch 复用 `UserViewModel.switchWorkspace(companyId)` / `CompanyApi.switchCompany`；
- 跨公司导航必须等 authenticated company/token 状态切换成功；
- Task detail 必须重新从 server fetch，card snapshot 不授权写操作；
- BOT_CARD / MEETING 必须回归；
- KMP APK CI 必须 green。

#30 完成前，server WORKBENCH emission 禁止开启。

---

## 7. NEXT GATES — #50 → #54 → #55

### #50 Storage / Managed Core Evolution — NEXT BLOCKER

目标：在不破坏 immutable 1906 adoption contract 的前提下，通过合法 Flyway managed-core evolution 允许 `messages.type=WORKBENCH`。

#50 只开放**存储能力**，不自动产生任何 WORKBENCH 消息。

### #54 Supported-client Rollout Gate — QUEUED

目标：把 ADR-0005 的 client-first 原则变成可执行发布策略：

- supported-client matrix；
- minimum version；
- feature gate；
- 灰度与 rollback；
- old-client behavior；
- Push / ClientEvent / IM Card 各自开启条件；
- 禁止 dual-write。

### #55 Task Realtime / Push / WORKBENCH Notification — BLOCKED

依赖：`#30 + #50 + #54`。

开启后才实现：

```text
Task domain event
→ AFTER_COMMIT
→ NotificationPolicy(TASK, action)
→ ClientEvent / Push / optional existing-route IM WORKBENCH Card
```

关键安全条件：

- DB rollback 不产生成功通知；
- eventId 可去重；
- Push 不携带敏感 Task detail；
- 不为了 OA 通知自动创建 conversation；
- Deep Link 仍由客户端重新获取当前资源并由 server 授权；
- 不做 TEXT + WORKBENCH 双写。

---

## 8. NEXT FEATURE MODULE — #56 Approval Backend V1

Approval 设计已完成，#56 已建立正式实施 Issue。

项目优先级选择：**先完成 Task 的端到端通知闭环，再进入 Approval 实现**，避免同时展开两个未闭环领域。

技术上 Approval Backend 不依赖 WORKBENCH emission 才能编码，但交付顺序保持：

```text
#30 Android consumer
→ #50 storage
→ #54 rollout gate
→ #55 Task notification E2E
→ #56 Approval Backend
→ Approval Web/Electron
→ Approval notification
```

Approval V1：固定 Definition + 串行 Nodes，覆盖 draft/submit/approve/reject/return/resubmit/cancel/CC，并复用 #13 permission/audit/tenant foundation。

---

## 9. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Repository Governance | STABLE | Issue/PR/PROJECT_MASTER + CI | #6 required checks |
| Tenant Migration | STABLE | baseline 1906 / target 2002 | #50 managed core evolution |
| Workbench Platform | STABLE | #13 | Task/Approval 复用 |
| Workbench Overview | STABLE | #39 + Task projection | Approval projection |
| Task Backend | STABLE | #45 / PR #46 | #55 notification |
| Task Web/Electron | STABLE | #47 / PR #49 | notification E2E |
| Electron/Web PR CI | STABLE | #9 / PR #48 | #6 required check |
| Workbench Protocol | STABLE | #14 + #28 | consumers/storage/emission |
| Workbench Web/Electron Card | STABLE | #29 / PR #52 | supported-client rollout |
| Workbench Android Card | **IN_PROGRESS** | #30 | implement + KMP CI + merge |
| WORKBENCH Message Storage | BLOCKED/NEXT | #50 | after #30 priority |
| Supported-client Rollout | QUEUED | #54 | after consumers/storage |
| Task Notification | BLOCKED | #55 | requires #30/#50/#54 |
| Approval Backend | QUEUED | #56 | after Task notification E2E |
| Approval Web/Electron | PLANNED | design complete | after #56 |
| Android OA full Task/Approval UI | PLANNED | Workbench shell exists | after core web vertical slices |

---

## 10. 执行路线

### 已完成

```text
#10 Formal Design ✅
→ #12 Tenant Migration ✅
→ #13 Platform Foundation ✅
→ #39 Overview ✅
→ #43 Baseline Scope ✅
→ #45 Task Backend ✅
→ #9 Electron/Web CI ✅
→ #47 Task Web/Electron ✅
→ #14 Structured Notification Design ✅
→ #28 WORKBENCH Protocol ✅
→ #29 Web/Electron Card + Deep Link ✅
```

### 当前到下一纵向闭环

```text
#30 Android/KMP Card + Deep Link        ← CURRENT
→ #50 WORKBENCH Storage/Core Evolution
→ #54 Supported-client Rollout Gate
→ #55 Task Realtime/Push/Card
→ Task V1 cross-client E2E DONE
```

### 下一业务领域

```text
#56 Approval Backend V1
→ Approval Web/Electron
→ Approval Realtime/Push/Card (reuse #54 policy)
→ Overview pending approvals
→ Android Approval capability
```

### 后续领域

```text
Calendar / Meeting aggregation
→ Announcement
→ Report
→ AI Office / Automation proposals
```

AI/Automation 只能调用真实 Workbench Domain Service，不能直接写 OA 表。

---

## 11. V1 E2E Definition of Done

### Task

```text
A(company X) creates/assigns Task to B
→ transaction commits
→ B receives policy-selected realtime/push/WORKBENCH notification on supported client
→ B opens canonical Deep Link
→ client switches authenticated company if required
→ server re-authorizes and returns current Task detail
→ B starts/completes Task
→ A observes current state / Overview refresh
→ switch to company Y: Task cannot be accessed
→ rollback path produces no success notification
```

### Approval（#56 后）

```text
A submit
→ B pending
→ B server-authorized approve/reject/return
→ A observes terminal/current state
→ Overview pending count changes
→ notification card remains event snapshot, not command authority
→ cross-tenant access denied
```

---

## 12. CI / Merge Gates

当前 checks：

- Repository Governance；
- Backend PR Validation（applicable paths）；
- Electron Web PR Validation（applicable paths，`npm ci + app:build + web:build`）；
- Build KMP APK；
- no unresolved review threads。

#6 仍负责把这些 checks 配成 `master` required checks；当前 `master` 仍未开启 branch protection。

---

## 13. Change Log

### 2026-08-24 — #53 Roadmap synchronization

状态：IN_PROGRESS。修正 PROJECT_MASTER / implementation roadmap 的历史状态漂移；新增 #54 rollout gate、#55 Task notification、#56 Approval Backend 真实追踪入口；CURRENT 切换到 #30。

### 2026-08-20 — #29 / PR #52

状态：COMPLETED，merge `eb332edafa68c2ab8a73a62f345742a7f61ddb42`。Web/Electron WORKBENCH safe consumer + tenant-aware Deep Link 已进入 master；server emission 继续禁用。

### 2026-08-20 — #28 / PR #51

状态：COMPLETED，merge `7f261e881bdc3eb3cebcd1d0468777f647d3ea43`。

### 2026-08-20 — #47 / PR #49

状态：COMPLETED，merge `b590bbd6a18737b504d0c3876a8d12831e3efe15`。

### 2026-08-20 — #45 / PR #46

状态：COMPLETED，merge `6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
