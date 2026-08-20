# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：#39 / PR #40 Workbench Overview；#43 / PR #44 Baseline Fingerprint Scope
- 当前交付：#45 Workbench Task Backend V1
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目原则与治理

Group-IM 是多租户组织协作 IM/OA 平台。消息是协作主链路，Workbench 承载结构化办公，AI/Automation 不建立第二份业务真相。

- master 唯一开发主线；main legacy；
- 所有变更通过 Issue/PR；每 PR 更新本文；默认 Squash；
- 不使用 username == admin 构建新 Workbench 权限；
- feature 文件更新使用 tree/commit/ref 路径；
- merge gate：Governance + applicable Backend + KMP + no unresolved threads。

仍待治理：#6 master protection、#7 legacy main deploy trigger、#9 Electron/Web CI、#22 Maven duplicate dependencies。

---

## 2. Tenant Migration

```text
core business baseline = 2026081906
managed current target = 2026082002
```

- #12 migration foundation 完成；
- #43 已把 immutable core fingerprint 与 full tenant inventory 分离；
- no-history extra object 仍 CONFLICT；Flyway-managed later objects 不制造 false core drift；
- new tenant：inactive → empty schema → Flyway current target → verify → active；
- existing reviewed tenant：preflight → baseline 1906 → migrate current target → validate/audit；
- default ddl-auto=update，validate profile staged；
- public clone/Safe Sync deprecated compatibility。

`2026082002` 为 Task V1 migration，新增 `wb_task / wb_task_assignee / wb_task_comment / wb_task_activity`。Core baseline 仍保持 1906，不随 managed target 前移。

测试 tenant 数据可清理重建，不阻塞功能模块实现。

---

## 3. Workbench Platform / Overview

#13 Platform Foundation 已完成：CurrentWorkContext、stable errors、fail-closed PermissionService、Organization/File adapters、Audit、explicit TenantExecutor。

#39 Overview 已完成：`GET /api/workbench/overview`，tenant only from current authenticated company；Meeting today projection；不暴露 schema；首版无 cache/write model。

---

## 4. Task Backend V1 — #45 CURRENT

正式设计：

- `doc/features/workbench/task.md`
- `doc/features/workbench/task-backend-v1.md`

### Data model

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

Task row 不存 company_id；tenant schema 是边界。User/department/conversation 存 ID，不建跨模块 JPA 强关系。`wb_task.version` 使用 optimistic locking。

### State machine

```text
TODO -> IN_PROGRESS / COMPLETED / CANCELLED
IN_PROGRESS -> BLOCKED / COMPLETED / CANCELLED
BLOCKED -> IN_PROGRESS / CANCELLED
COMPLETED -> IN_PROGRESS
CANCELLED -> terminal
```

客户端不能 PATCH status；状态只通过 action API。

### API

Base `/api/workbench/tasks`：create/update/list/detail；start/block/resume/complete/reopen/cancel；assignee add/remove；comment；activities。

### Permission

- feature gate 复用 #13；
- creator/owner 可 manage；
- OWNER/COLLABORATOR assignee 可执行工作动作；
- WATCHER 只 view/comment；
- creator/owner/any assignee 可 view；
- owner/assignee 必须由 OrganizationAdapter 验证为当前公司 active member；
- list 只返回当前用户 creator/owner/assignee 可见任务。

### Activity / Audit

Create/update/workflow/assignee/comment 同步写 Task Activity，并记录 Workbench Audit。通知、Realtime、WORKBENCH Card 留到 after-commit 后续阶段，不在本 PR 提前发射。

### Overview

Task 上线后 Overview 使用真实 Task source：assignedTaskCount、overdueTaskCount、recentTasks；Quick Apps 增加 TASK。Approval/Announcement 继续 zero/empty 直到各自领域实现。

---

## 5. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Workbench Platform | STABLE | #13 完成 | 领域复用 |
| Workbench Overview | STABLE | #39 完成，Task projection 本 PR 扩展 | Web/Electron |
| Tenant Migration | STABLE | core 1906 / target 2002 | 后续 OA migrations |
| OA Task Backend | IN_PROGRESS | #45 migration/domain/API/permission/audit | CI + merge |
| OA Task Web/Electron | PLANNED | 等 Backend contract | UI |
| OA Approval | PLANNED | 设计已完成 | Task 闭环后 |

---

## 6. CI / Validation

#45 必须通过：

- Task state-machine tests；
- resource policy tests；
- Task service create/action tests；
- Overview Task projection tests；
- PostgreSQL Task migration Testcontainers；
- new tenant provisioning reaches 2026082002；
- core fingerprint pinned hashes unchanged；
- baseline/runtime regression；
- Governance / Backend / KMP。

---

## 7. Roadmap

```text
#13 Platform Foundation ✅
→ #39 Overview API ✅
→ #43 Baseline Scope ✅
→ #45 Task Backend ← CURRENT
→ Task Web/Electron
→ Task Realtime / Push / WORKBENCH Card
→ Approval Backend / UI
→ Calendar / Announcement / Android OA / Report / AI Office
```

#14 structured card protocol design 已接受；server actual WORKBENCH emission 继续受 client-first gate。

---

## 8. Change Log

### 2026-08-20 — #45 Task Backend V1

状态：IN_PROGRESS。Managed target 前进到 `2026082002`，实现 Task 四表、状态机、API、资源权限、Activity/Audit、Overview Task 真数据。

### 2026-08-20 — #43 / PR #44

状态：COMPLETED，merge `8384ebd69ef203b661f89db98b2169d35b06ec7e`。

### 2026-08-20 — #39 / PR #40

状态：COMPLETED，merge `979f1f9a0efe407efe4981f3880835d40490f9f5`。

### 2026-08-20 — #13 / PR #38

状态：COMPLETED，merge `4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
