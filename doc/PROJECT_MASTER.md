# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-24
- 唯一开发主线：`master`
- 最近完成：#45 Task Backend、#47 Task Web/Electron、#28 Workbench Protocol、#29 Web/Electron Workbench Card、#30 Android/KMP Workbench Card
- **当前交付：#50 WORKBENCH Message Storage / managed core evolution**
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
| Android/KMP Card + Deep Link | #30 / PR #59 | COMPLETED | safe Compose renderer + authenticated company switch + server re-fetch |
| WORKBENCH Storage | #50 | **IN_PROGRESS** | 2003 managed-core evolution + `messages.type=WORKBENCH` |
| Supported-client rollout | #54 | QUEUED | minimum version / feature gate / rollback policy |
| Task Notification | #55 | BLOCKED | 等 #50 + #54 后开启 actual emission |
| Approval Backend | #56 | QUEUED | Task 通知闭环后进入下一领域模块 |

---

## 3. Tenant Migration 当前契约

```text
core business adoption baseline = 2026081906
managed current target           = 2026082003   (#50 merge boundary)
```

### 3.1 Immutable adoption baseline

`2026081906` 永远是 existing no-history tenant 的 adoption contract：

- historical migrations `V2026081901..1906` 不修改；
- pinned tables/columns/constraints/indexes/views/sequences hash 不修改；
- no-history tenant 必须精确匹配该 contract 才可 baseline；
- no-history tenant 手工提前增加 WORKBENCH 或其他 core ALTER 仍为 `DRIFTED/CONFLICT`；
- Safe Sync / public clone 只保留 deprecated/transitional compatibility。

### 3.2 Later managed objects

`2026082002` 已加入：

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

#43 已保证 Flyway history 证明的 later business tables/sequences 不污染 1906 adoption fingerprint；无 history tenant 的 unknown extra objects 仍 fail closed。

### 3.3 #50 Managed core evolution

`messages_type_check` 本身属于 1906 pinned core constraint。#50 不通过修改历史 baseline 来加入 WORKBENCH，而采用两阶段验证：

```text
Adoption contract (1906, immutable)
        +
Flyway-proven managed core evolution
        ↓
Managed current contract (2003)
```

`V2026082003__allow_workbench_message_type.sql`：

- 只扩展 `messages_type_check`；
- 保留 TEXT / FILE / VOICE / VIDEO / IMAGE / MEDIA / MEETING / BOT_CARD；
- 新增 WORKBENCH；
- 记录 `managed_core_contract=2026082003` metadata；
- **不创建、不发送任何 WORKBENCH 消息**。

验证规则：

1. no-history tenant：不启用任何 managed projection，仍精确比较 1906；
2. history 未执行 2003：`messages_type_check` 必须仍是 baseline type set；
3. history 已执行 2003：当前 CHECK 必须精确是 baseline type set + WORKBENCH；
4. 只有“Flyway history 证明 2003 已执行 + 当前语义精确匹配”时，fingerprint 才把这一个已知 evolution 投影回 1906 adoption representation；
5. history 说 2003 但 CHECK 缺 WORKBENCH、增加未知类型或其他手工修改，仍为 `DRIFTED`；
6. 未来 core evolution 必须新增新的 immutable managed contract，禁止通配忽略 core drift。

因此：

```text
MessageType.WORKBENCH protocol recognition ✅
Web/Electron consumer                 ✅
Android/KMP consumer                  ✅
WORKBENCH persistence schema          #50
server WORKBENCH emission             ❌ 仍由 #54/#55 gate 控制
```

---

## 4. Workbench Platform / Overview / Task

### Platform Foundation — STABLE

#13 已提供 `CurrentWorkContext`、fail-closed permission、Audit、Organization/File adapters、explicit tenant executor。后续领域禁止建立私有 tenant/security 框架。

### Overview — STABLE

`GET /api/workbench/overview` 只使用 authenticated current company，不暴露 schemaName；Meeting 使用轻量 projection；已接入真实 Task assigned/overdue/recent projection。

### Task Backend — STABLE

#45 / PR #46，merge `6ff1d1f99567e9d203ca1e27e20c47db24551d62`：Task 四表、状态机、resource permission、assignee/comment/activity、Audit 与 Overview projection 已完成。

### Task Web/Electron — STABLE

#47 / PR #49，merge `b590bbd6a18737b504d0c3876a8d12831e3efe15`：Workbench 内 Task Center 已支持 create/list/detail/state action/comment/activity。

---

## 5. Workbench Structured Notification Contract

### #14 / #28 Protocol — COMPLETED

已固定：独立 `WORKBENCH` MessageType、versioned Card/Event envelope、stable eventId、`WORKBENCH_RESOURCE_EVENT`、最小 Push、tenant-aware Deep Link、client-first rollout、禁止 TEXT + WORKBENCH 双写。

PR #51 merge：`7f261e881bdc3eb3cebcd1d0468777f647d3ea43`。

### #29 Web/Electron consumer — COMPLETED

PR #52 merge：`eb332edafa68c2ab8a73a62f345742a7f61ddb42`。

独立 safe parser/renderer、authenticated company switch、server-refetched Task detail、forbidden/deleted fail closed；card snapshot 不执行 Complete/Approve 等写操作。

### #30 Android/KMP consumer — COMPLETED

PR #59 merge：`28036e6ccd6558c84fe95e2ece48ddb874514441`。

已完成：

- KMP local `MessageType.WORKBENCH`；
- safe V1 parser + malformed/unknown fallback；
- 独立 Compose `WorkbenchMessageCard`，不复用 BOT_CARD；
- existing `UserViewModel.switchWorkspace(companyId)` authenticated switch；
- target company state ready 后才 `GET /api/workbench/tasks/{taskId}`；
- resource current detail 来自 server，card snapshot 不授权写操作；
- conversation preview 使用 `[工作台]`，不泄漏 JSON payload；
- parser regression + KMP APK CI 通过。

两类 supported client consumer 均已具备安全消费能力，但这不等于允许 server emission。

---

## 6. CURRENT → NEXT：#50 → #54 → #55

### #50 Storage / Managed Core Evolution — CURRENT

完成条件：

- 2003 migration 在 PostgreSQL Testcontainers 通过；
- 1906 pinned adoption hashes保持不变；
- legacy 1906 no-history tenant 仍 BASELINE_READY；
- baseline 后正常 migrate 到 2003；
- 2003 managed tenant 不误报 drift；
- history/current CHECK 不一致时 fail closed；
- provisioning / migration runtime target 前移到 2003；
- server emission 仍关闭。

### #54 Supported-client Rollout Gate — NEXT

把 client-first 原则变成可执行发布策略：supported-client matrix、minimum version、feature gate、灰度/rollback、old-client behavior、各 delivery channel 开启条件。

### #55 Task Realtime / Push / WORKBENCH Notification — BLOCKED

依赖 `#50 + #54`。开启后才实现：

```text
Task domain event
→ AFTER_COMMIT
→ NotificationPolicy(TASK, action)
→ ClientEvent / Push / optional existing-route IM WORKBENCH Card
```

关键安全条件：rollback 不产生成功通知、eventId 去重、Push 不携带敏感 Task detail、不隐式创建 conversation、客户端重新鉴权、不双写 TEXT + WORKBENCH。

---

## 7. 开源复用策略

当前原则：**优先复用已有依赖和成熟 Spring 生态，不为了“用开源”增加重复架构。**

### Android/KMP

项目已有 Ktor + kotlinx.serialization + Voyager / AndroidX Navigation；#30 不再引入 Decompose 等第三套路由体系。

### #55 Event delivery 候选

在 #55 编码前做正式 spike：

1. **Spring Modulith Event Publication Registry — 首选候选**：适配当前模块化单体，可在原业务事务内记录 event publication，并提供完成/失败/重提能力；
2. `gruelbox/transaction-outbox` — Apache-2.0 备选，更偏 microservice/eventual-consistency；
3. 若两者都不能满足 tenant/audit/notification policy 边界，再自研最小 outbox。

禁止直接使用裸 `@Async @TransactionalEventListener` 承担可靠通知，因为进程在 commit 后、listener 执行前失败会丢事件。

### Approval

#56 V1 是固定 definition + 串行 nodes，保持显式领域状态机。Flowable 作为未来 BPMN/并行 gateway/timer/escalation 复杂化后的备选，不在 V1 引入；许可证不清晰的 workflow 项目不进入生产依赖。

---

## 8. 下一业务领域 — #56 Approval Backend V1

项目交付顺序保持：

```text
#50 storage
→ #54 rollout gate
→ #55 Task notification E2E
→ #56 Approval Backend
→ Approval Web/Electron
→ Approval notification
```

Approval V1 复用 #13 permission/audit/tenant foundation，固定 Definition + 串行 Nodes，覆盖 draft/submit/approve/reject/return/resubmit/cancel/CC。

---

## 9. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Tenant Migration | STABLE / EVOLVING | baseline 1906 / target 2003 on #50 | managed core regression |
| Workbench Platform | STABLE | #13 | Task/Approval 复用 |
| Workbench Overview | STABLE | #39 + Task projection | Approval projection |
| Task Backend | STABLE | #45 / PR #46 | #55 notification |
| Task Web/Electron | STABLE | #47 / PR #49 | notification E2E |
| Workbench Protocol | STABLE | #14 + #28 | storage/emission |
| Workbench Web/Electron Card | STABLE | #29 / PR #52 | #54 rollout |
| Workbench Android Card | STABLE | #30 / PR #59 | #54 rollout |
| WORKBENCH Message Storage | **IN_PROGRESS** | #50 | CI / merge |
| Supported-client Rollout | QUEUED | #54 | after #50 |
| Task Notification | BLOCKED | #55 | requires #50/#54 |
| Approval Backend | QUEUED | #56 | after Task notification E2E |

---

## 10. 执行路线

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
→ #30 Android/KMP Card + Deep Link ✅
→ #50 WORKBENCH Storage/Core Evolution        ← CURRENT
→ #54 Supported-client Rollout Gate
→ #55 Task Realtime/Push/Card
→ Task V1 cross-client E2E DONE
→ #56 Approval Backend V1
```

---

## 11. CI / Merge Gates

当前 checks：Repository Governance、Backend PR Validation（applicable paths）、Electron Web PR Validation（applicable paths）、Build KMP APK、no unresolved review threads。#6 仍负责把这些 checks 配成 `master` required checks。

---

## 12. Change Log

### 2026-08-24 — #50 WORKBENCH managed core storage

状态：IN_PROGRESS。新增 2003 managed-core contract：Flyway history 与当前 `messages_type_check` 语义双验证，只有已知合法 evolution 可投影回 immutable 1906 adoption fingerprint；actual server emission 仍禁用。

### 2026-08-24 — #30 / PR #59 Android/KMP Workbench consumer

状态：COMPLETED，merge `28036e6ccd6558c84fe95e2ece48ddb874514441`。

### 2026-08-24 — #53 / PR #57 Roadmap synchronization

状态：COMPLETED。CURRENT/NEXT 路线与真实 Issues 同步。

### 2026-08-20 — #29 / PR #52

状态：COMPLETED，merge `eb332edafa68c2ab8a73a62f345742a7f61ddb42`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
