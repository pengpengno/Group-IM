# Workbench / OA 实施 Roadmap

> 状态：ACTIVE PLAN  
> 最后同步：2026-08-24  
> 设计基线：#10 / ADR-0002 / ADR-0003 / ADR-0005  
> 路线同步 Issue：#53  
> 规则：每个阶段必须 `Issue -> Branch -> PR -> CI -> PROJECT_MASTER -> Merge`

本文只描述**当前可执行路线**。历史设计细节分别保留在：

- `README.md`：Workbench/OA 业务边界；
- `task.md`：Task 领域设计；
- `approval.md`：Approval 领域设计；
- `platform-integration.md`：tenant/security/file/job 集成；
- `notification-protocol.md` + ADR-0005：WORKBENCH Card / ClientEvent / Push / Deep Link 协议。

项目实时状态以 `doc/PROJECT_MASTER.md` 为唯一事实入口。

---

## 1. 当前路线结论

截至 2026-08-24，Workbench 已经完成从“设计与基础设施”到“Task Web/Electron 可用闭环”的主体建设：

```text
Formal Design                         ✅
Tenant Versioned Migration            ✅
Workbench Platform Foundation         ✅
Overview                              ✅
Task Backend                          ✅
Task Web/Electron                     ✅
Structured OA Notification Design     ✅
WORKBENCH Protocol                    ✅
Web/Electron WORKBENCH Consumer       ✅
Android/KMP WORKBENCH Consumer        ✅
WORKBENCH Storage/Core Evolution      ← CURRENT
Supported-client Rollout Gate         NEXT
Task Realtime/Push/Card                BLOCKED
Approval Backend                       QUEUED
```

当前唯一优先主线：

```text
#30 Android/KMP Card + Deep Link ✅
→ #50 WORKBENCH Message Storage / Managed Core Evolution ← CURRENT
→ #54 Supported-client Rollout Gate
→ #55 Task Realtime / Push / WORKBENCH Notification
→ Task V1 cross-client E2E
→ #56 Approval Backend V1
```

选择这一路线的原因：先把一个 Task 纵向切片从数据库、API、Web/Electron、Android consumer、通知、安全 rollout 全部闭环，再展开 Approval，避免两个领域同时处于半完成状态。

---

## 2. 已完成阶段

| 阶段 | Issue | 状态 | 产物 |
| --- | --- | --- | --- |
| 正式 Feature Design | #10 | ✅ COMPLETED | Workbench / Task / Approval / platform / roadmap |
| Tenant Versioned Migration | #12 | ✅ COMPLETED | Flyway provisioning / baseline / validate / audit |
| Workbench Platform Foundation | #13 | ✅ COMPLETED | CurrentWorkContext / permission / audit / adapters / tenant executor |
| Overview | #39 | ✅ COMPLETED | `/api/workbench/overview` + Meeting/Task projection |
| Baseline Scope Compatibility | #43 | ✅ COMPLETED | immutable core baseline 与 later managed object 分离 |
| Task Backend | #45 | ✅ COMPLETED | `wb_task*` + API + state machine + permission + activity/audit |
| Electron/Web CI | #9 | ✅ COMPLETED | clean runner app/web build gate |
| Task Web/Electron | #47 | ✅ COMPLETED | Workbench Task Center 核心操作闭环 |
| Structured OA Card Design | #14 | ✅ COMPLETED | ADR-0005 / client-first rollout / delivery matrix |
| WORKBENCH Protocol | #28 | ✅ COMPLETED | Java/Proto/envelope/event/deep-link contract |
| Web/Electron Consumer | #29 | ✅ COMPLETED | safe card renderer / tenant-aware Deep Link / server re-fetch |
| Android/KMP Consumer | #30 | ✅ COMPLETED | PR #59 / safe Compose renderer / authenticated company switch / server re-fetch |

### 已完成的 Task 非通知闭环

```text
create
→ assign
→ query/detail
→ start/block/resume
→ complete/reopen/cancel
→ comment/activity
→ Overview aggregation
→ Web/Electron UI
```

当前缺口不是 Task 领域本身，而是**跨客户端安全通知闭环**。

---

## 3. COMPLETED — #30 Android/KMP Workbench Card + Deep Link

### 目标

让 Android/KMP 在 server 开启 WORKBENCH emission 之前，先具备与 Web/Electron 同等的安全 consumer 能力。

### 已确认的技术边界

1. KMP 本地 `MessageType` 必须 append `WORKBENCH`；
2. Proto `WORKBENCH` 映射不能在 `MessageType.valueOf(...)` 阶段崩溃；
3. `MessageBubble.kt` 新增独立 Workbench renderer，不复用 BOT_CARD；
4. V1 parser 校验 version/category/action/target/deepLink；
5. unknown/malformed payload 只显示安全 fallback；
6. company switch 必须复用 authenticated `CompanyApi.switchCompany` / `UserViewModel.switchWorkspace`；
7. 跨公司时必须等 credential/current company 真正更新后再读取资源；
8. Task Detail 必须重新调用 server API，不能信任卡片 snapshot；
9. 卡片不直接执行 Complete/Approve 等业务命令；
10. BOT_CARD / MEETING 行为必须回归；
11. KMP APK CI green。

### 验收

```text
valid WORKBENCH V1
→ Compose card renders
→ tap
→ optional authenticated workspace switch
→ GET current resource detail
→ server authorizes
→ current detail / forbidden / deleted safe state
```

#30 已由 PR #59 合并完成；server actual WORKBENCH emission 仍由 #50 + #54 + #55 禁止。

---

## 4. CURRENT — #50 WORKBENCH Message Storage / Managed Core Evolution

### 背景

`messages_type_check` 属于 `2026081906` immutable core baseline，当前不允许 `WORKBENCH`。

直接手工 ALTER 会破坏 core fingerprint 语义，因此 #50 不是一个“改 CHECK 就结束”的小修，而是正式建立 **managed core evolution**。

### 必须保持

- 历史 migration `V2026081901..1906` 不修改；
- no-history tenant 仍严格按 1906 adoption contract；
- unknown/manual core drift 仍 fail closed；
- 已有可信 Flyway history 的 tenant 可以证明合法后续 core evolution；
- 新 immutable migration 扩展 `messages_type_check`；
- BOT_CARD / MEETING 等原类型不变；
- provisioning / baseline / target / Testcontainers regression 同步。

### 结果

#50 完成只代表：

```text
数据库可以合法保存 WORKBENCH
```

不代表 server 可以立即开始发消息。

---

## 5. NEXT — #54 Supported-client Rollout Gate

### 目标

把 ADR-0005 的“client-first”从设计原则变成发布规则。

### 必须固定

- supported-client matrix；
- Web/Electron minimum version；
- Android/KMP minimum version；
- rollout feature gate；
- 灰度策略；
- rollback/kill switch；
- old-client behavior；
- ClientEvent / Push / IM WORKBENCH Card 各自开启条件；
- 禁止 TEXT + WORKBENCH dual-write。

### Gate

只有：

```text
#29 Web/Electron consumer ✅
+ #30 Android/KMP consumer ✅
+ #50 WORKBENCH storage ⏳
+ #54 rollout policy ⏳
```

全部满足，才允许进入 actual emission。

---

## 6. #55 Task Realtime / Push / WORKBENCH Notification

状态：BLOCKED，依赖 #30 + #50 + #54。

### 领域原则

Task 表/状态仍是唯一业务真相。通知只是已经提交成功的业务事实的投影视图。

### 处理链路

```text
Task Domain Command
→ DB transaction
→ COMMIT
→ AFTER_COMMIT Domain Event Handler
→ NotificationPolicy(category=TASK, action=...)
→ ClientEvent / Push / optional IM WORKBENCH Card
```

### V1 事件候选

- created；
- assigned；
- started；
- blocked；
- resumed；
- completed；
- reopened；
- cancelled；
- comment-added（按 policy 决定）。

### 安全边界

- rollback 不产生成功通知；
- stable `eventId` 去重；
- Push 只传最低敏感度信息；
- IM Card 只在已有合法 route 时使用；
- 不为了 OA 通知自动创建 conversation；
- Deep Link 不授权写操作；
- client 打开后必须 server re-fetch + re-authorize；
- 禁止 TEXT + WORKBENCH 双写。

### Task V1 E2E Gate

```text
A(company X) create/assign Task to B
→ commit
→ B receives policy-selected notification on supported client
→ B opens Deep Link
→ authenticated company switch if needed
→ server returns current Task detail
→ B start/complete
→ A sees current state / Overview refresh
→ company Y cannot access X Task
→ rollback emits nothing
```

达到该 Gate 后，Task V1 才算真正跨端闭环。

---

## 7. #56 Approval Backend V1

状态：QUEUED。

技术上 Approval Backend 可以独立于 WORKBENCH notification 开发，但项目交付优先级选择在 #55 之后开始，以保证一个领域完整闭环后再进入下一领域。

### 数据

```text
wb_approval_definition
wb_approval_instance
wb_approval_node
wb_approval_action
wb_approval_cc
```

### V1 流程

```text
draft
→ submit
→ serial pending node
→ approve / reject / return
→ resubmit when returned
→ terminal approved/rejected/cancelled
```

### 必须复用

- #13 CurrentWorkContext；
- Workbench Permission；
- Workbench Audit；
- Organization/File adapters；
- Flyway tenant migration；
- optimistic/concurrency control；
- ADR-0005 notification protocol（通知阶段）。

### 后端验收

```text
A submit
→ B becomes current assignee
→ B approve
→ next node / terminal result correct
→ duplicate/concurrent approve safe
→ tenant/resource permission fail closed
→ audit/action history complete
```

---

## 8. Approval Web/Electron 与通知

#56 后拆独立 Issue/PR：

### UI

- 发起审批；
- 待我审批；
- 我发起的；
- 我处理过的；
- 抄送我的；
- Detail / Timeline；
- Approve / Reject / Return / Cancel；
- Attachment。

### Overview

接入真实：

```text
pendingApprovalCount
recent approvals
```

### Notification

复用 #54 rollout policy 与 WORKBENCH protocol，不再为 Approval 发明第二套消息协议。

---

## 9. Android OA 后续完整度

#30 只负责 **WORKBENCH structured card consumer/deep-link gate**，不是完整 Android Task/Approval UI。

后续 Android OA 推荐顺序：

1. Overview real API；
2. Task list/detail；
3. Task Start/Complete/Comment；
4. Approval pending/detail/actions；
5. Announcement；
6. Task/Approval 创建体验；
7. 管理配置最后补。

Android 和 Web/Electron 共享 server DTO/permission/domain truth，不建立移动端专属业务模型。

---

## 10. 后续业务领域

Task + Approval 稳定后：

```text
Calendar / Meeting aggregation
→ Announcement
→ Report
→ AI Office / Automation proposals
```

### Calendar / Schedule

- `wb_schedule`；
- participant/reminder；
- Meeting adapter；
- Task due projection；
- Today Schedule。

Meeting 的真实参与状态继续归 Meeting 模块维护。

### Announcement

- Company / Department / User target；
- read receipt；
- publish permission；
- normal/urgent notification policy；
- Overview unread。

### AI Office

AI/Automation 可以：

- 总结 Task/Approval/Meeting；
- 从会议/消息提出 Task 建议；
- 建议自动化流程；

但创建/修改真实业务对象必须调用 Workbench Domain Service，并经过权限与必要的人机确认；禁止直接写 OA 表。

---

## 11. 数据库路线

当前：

```text
core baseline = 2026081906
managed target = 2026082002
```

原则：

- core baseline 永不随普通业务 migration 前移；
- Task/Approval 新表使用新的 immutable tenant migration；
- managed target 随合法 migration 前移；
- no-history legacy adoption 与 managed current validation 是两个不同阶段；
- 不允许 Hibernate `ddl-auto=update` 作为 Workbench schema authority；
- `ddl-auto=validate` 继续按 staging/canary/production coverage 分阶段推进；
- 测试 tenant 可删除重建，不阻塞功能开发；真实数据环境必须走正式 migration/preflight。

---

## 12. 通知协议不变量

以下规则后续所有 Task/Approval/Announcement/Schedule/Report 都必须遵守：

1. `WORKBENCH` 与 `BOT_CARD` 语义隔离；
2. card payload versioned；
3. card 是 immutable event snapshot；
4. current domain state 必须 server fetch；
5. companyId 是 routing hint，不是 authorization；
6. schemaName 不下发客户端；
7. Deep Link 必须 tenant-aware；
8. client 不从 card payload 直接执行业务写命令；
9. unknown/malformed payload safe fallback；
10. Push 最小化；
11. no implicit conversation creation；
12. no TEXT + WORKBENCH dual-write；
13. server emission 必须经过 supported-client rollout gate。

---

## 13. PR / CI 路线规则

每个实现 Issue 必须包含：

```text
背景
目标
范围
非范围
依赖
数据/API/协议影响
验收标准
测试计划
兼容性/迁移风险
```

每个实现 PR 必须包含：

- `Closes #issue` 或 `Related to #issue`；
- 实现与测试；
- `doc/PROJECT_MASTER.md` 更新；
- 对应 Feature Design / Roadmap 更新（当状态或架构变化）；
- migration 说明（如有）；
- API/协议兼容性说明（如有）；
- follow-up blockers/Issues。

Merge gates：

```text
Repository Governance
+ Backend PR Validation (applicable)
+ Electron Web PR Validation (applicable)
+ Build KMP APK
+ no unresolved review threads
```

默认 Squash Merge。

---

## 14. 当前执行队列

### P0 — 正在做

```text
#50 WORKBENCH Message Storage / Managed Core Evolution
```

### P1 — 紧随其后

```text
#54 Supported-client Rollout Gate
```

### P2 — 完成 Task 纵向闭环

```text
#55 Task Realtime / Push / WORKBENCH Notification
```

### P3 — 下一业务模块

```text
#56 Approval Backend V1
→ Approval Web/Electron
→ Approval Notification
```

### P4 — 后续

```text
Android OA full capabilities
Calendar / Meeting aggregation
Announcement
Report
AI Office / Automation
```

---

## 15. 路线变化记录

### 2026-08-24 — #30 completed / #50 current

- PR #59 已合并，Android/KMP consumer 标记 COMPLETED；
- CURRENT 切到 #50 managed-core evolution；
- 2003 只开放 WORKBENCH 合法存储，不开启 server emission；
- actual emission 仍依赖 #54 rollout gate 与 #55 notification implementation。

### 2026-08-24 — #53

- 修正早期 roadmap 将 #10/#12/#13/#14 等已完成阶段仍标为 OPEN/IN_PROGRESS 的历史状态漂移；
- #29 Web/Electron consumer 标记 COMPLETED；
- CURRENT 切到 #30；
- #50 固定为 storage/core evolution blocker；
- 新建 #54 supported-client rollout gate；
- 新建 #55 Task notification implementation；
- 新建 #56 Approval Backend V1；
- 固定当前优先路线：`#30 -> #50 -> #54 -> #55 -> #56`。

---

> Workbench 当前不是“继续堆页面”，而是先完成 Task 的安全跨客户端通知纵向闭环，再复制已经验证过的平台模式到 Approval。
