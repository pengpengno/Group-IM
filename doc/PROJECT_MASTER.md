# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：#45 / PR #46 Workbench Task Backend V1；#9 / PR #48 Electron/Web PR Validation
- 当前交付：#47 Workbench Task Web/Electron
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目原则与治理

Group-IM 是多租户组织协作 IM/OA 平台。消息是协作主链路，Workbench 承载结构化办公，AI/Automation 不建立第二份业务真相。

- `master` 是唯一开发主线；`main` 是 legacy；
- 所有变更通过 Issue/PR；每个 PR 更新本文；默认 Squash Merge；
- 新 Workbench 权限禁止使用 `username == admin`；
- feature 文件更新必须明确目标分支，优先使用 branch-aware tree/commit/ref；
- merge gate：Repository Governance + applicable Backend + applicable Electron/Web + KMP + no unresolved review threads。

仍待治理：#6 master protection、#7 legacy main deploy trigger、#22 Maven duplicate dependencies。

---

## 2. Tenant Migration

```text
core business baseline = 2026081906
managed current target = 2026082002
```

- #12 migration foundation 完成；
- #43 已把 immutable core fingerprint 与 full tenant inventory 分离；
- no-history extra object 仍 `CONFLICT`；Flyway-managed later objects 不制造 false core drift；
- new tenant：inactive → empty schema → Flyway current target → verify → active；
- existing reviewed tenant：preflight → baseline 1906 → migrate current target → validate/audit；
- default `ddl-auto=update`，validate profile staged；
- public clone / Safe Sync 是 deprecated compatibility，不是 migration authority。

`2026082002` 为 Task V1 migration，新增：

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

Core baseline 仍保持 1906，不随 managed target 前移。测试 tenant 数据可清理重建，不阻塞功能模块实现。

---

## 3. Workbench Platform / Overview

### Platform Foundation — #13 COMPLETED

已提供：

- `CurrentWorkContext`；
- stable Workbench errors；
- fail-closed `WorkbenchPermissionService`；
- Organization/File adapters；
- Workbench Audit；
- explicit tenant executor。

### Overview — #39 COMPLETED

`GET /api/workbench/overview` 已稳定：

- tenant 只来自 authenticated current company；
- response 不暴露 schemaName；
- Meeting today 使用轻量 projection；
- 无独立 Overview write model/cache；
- Task Backend 上线后已接入真实 assigned/overdue/recent Task projection。

---

## 4. Task Backend V1 — #45 COMPLETED

PR #46 Squash merge：`6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

正式设计：

- `doc/features/workbench/task.md`
- `doc/features/workbench/task-backend-v1.md`

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

Base `/api/workbench/tasks`：

- create / update / list / detail；
- start / block / resume / complete / reopen / cancel；
- assignee add/remove；
- comment；
- activities。

### Permission

- feature gate 复用 #13；
- creator/owner 可 manage；
- OWNER/COLLABORATOR assignee 可执行工作动作；
- WATCHER 可 view/comment；
- list 只返回当前用户 creator/owner/assignee 可见任务；
- owner/assignee 由 OrganizationAdapter 验证为当前公司 active member。

### Activity / Audit

Create/update/workflow/assignee/comment 同事务写 Task Activity，并记录 Workbench Audit。Realtime / Push / WORKBENCH Card 留给 after-commit 后续阶段。

---

## 5. Electron / Web Client

当前 Electron/Web：React 18 + TypeScript + Webpack，主目录：

```text
Group-app/Group-Electronjs
```

### Electron/Web PR Validation — #9 COMPLETED

PR #48 Squash merge：`d5570e0145a549a4869118b957a059a857e0d6ee`。

`.github/workflows/electron-web.yml` 在 Electron/Web 路径变化时执行：

```text
npm ci
production desktop env
npm run app:build
npm run web:build
```

首次 workflow 自验证已通过 Electron main/renderer、Web bundle 和 KMP。项目目前没有稳定 lint/test script，因此当前门禁以 TypeScript/Webpack build 为真实证据；#6 后续可把该 check 配成 master required check。

---

## 6. Task Web/Electron V1 — #47 CURRENT

正式实现文档：`doc/features/workbench/task-web-v1.md`。

目标用户链路：

```text
Workbench Overview
→ Task Center
→ create
→ list/detail
→ Start / Block / Resume / Complete / Reopen / Cancel
→ comment / activity
→ back to refreshed Overview
```

### Navigation boundary

Task 是 Workbench OA 子模块，不新增 Dashboard 顶级 tab：

```text
Dashboard activeTab = workbench
        ↓
Workbench local view
  ├── overview
  └── tasks
```

Meeting / Contacts / Automation / Settings 继续使用现有全局导航。

### Typed client

新增：

```text
renderer/features/workbench/workbenchTypes.ts
renderer/services/api/workbenchAPI.ts
```

Workbench 新代码使用后端真实 envelope：

```text
code + message + data + timestamp
```

客户端不传 companyId 作为 tenant route。API client 复用现有 `BASE_URL` 与 Bearer token 存储约定。

### Overview UI

Workbench 页面读取真实：

- current company；
- assigned / overdue Task counts；
- pending Approval / unread Announcement counts；
- recent Task；
- today Meeting schedules；
- quick apps。

Task 修改后返回 Overview 会重新请求服务端，不维护第二份本地真相。

### Task Center UI

已实现页面结构：

- 可见 Task list；
- Task detail；
- create modal；
- state-based action buttons；
- assignee summary；
- comments + add comment；
- activity timeline；
- loading / empty / error / mutation busy；
- desktop list/detail 双栏 + narrow-screen 单栏退化。

客户端的动作显示只是 UX 提示；服务端 `TaskAccessPolicy + TaskStateMachine` 始终是最终权限/状态权威。

#47 不包含 Realtime / Push / WORKBENCH Card、#29 structured-card renderer/deep-link、Android Task UI、附件 relation 或新的 Task Redux/global store。

---

## 7. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Workbench Platform | STABLE | #13 完成 | 领域复用 |
| Workbench Overview | STABLE | #39 + Task projection | #47 UI 消费 |
| Tenant Migration | STABLE | core 1906 / target 2002 | 后续 OA migrations |
| OA Task Backend | STABLE | #45 / PR #46 已合并 | UI / events |
| OA Task Web/Electron | IN_PROGRESS | #47 Overview + Task Center | build / review / merge |
| Electron/Web PR CI | STABLE | #9 / PR #48 已合并 | #6 required check |
| OA Approval | PLANNED | 设计已完成 | Task 闭环后 |

---

## 8. CI / Merge Gates

当前 checks：

- Repository Governance；
- Backend PR Validation（后端适用路径）；
- Electron Web PR Validation（Electron/Web 适用路径）；
- Build KMP APK。

#47 必须通过 Electron main/renderer + Web build，并保持 no unresolved review threads。#6 仍负责把稳定 checks 配置为 master required checks。

---

## 9. Roadmap

```text
#13 Platform Foundation ✅
→ #39 Overview API ✅
→ #43 Baseline Scope ✅
→ #45 Task Backend ✅
→ #9 Electron/Web CI ✅
→ #47 Task Web/Electron ← CURRENT
→ Task Realtime / Push / WORKBENCH Card
→ Approval Backend / UI
→ Calendar / Announcement / Android OA / Report / AI Office
```

#14 structured card protocol design 已接受；server actual WORKBENCH emission 继续受 client-first gate。

---

## 10. Change Log

### 2026-08-20 — #47 Task Web/Electron

状态：IN_PROGRESS。Workbench 接真实 Overview，新增 Workbench 内部 Task Center，覆盖 create/list/detail/state actions/comments/activity 和响应式页面状态。

### 2026-08-20 — #9 / PR #48 Electron/Web PR Validation

状态：COMPLETED，merge `d5570e0145a549a4869118b957a059a857e0d6ee`。Electron main/renderer + Web bundle build gate 已进入 master。

### 2026-08-20 — #45 / PR #46 Task Backend V1

状态：COMPLETED，merge `6ff1d1f99567e9d203ca1e27e20c47db24551d62`。Managed target 前进到 `2026082002`，Task 四表、状态机、API、资源权限、Activity/Audit、Overview Task 真数据已进入 master。

### 2026-08-20 — #43 / PR #44

状态：COMPLETED，merge `8384ebd69ef203b661f89db98b2169d35b06ec7e`。

### 2026-08-20 — #39 / PR #40

状态：COMPLETED，merge `979f1f9a0efe407efe4981f3880835d40490f9f5`。

### 2026-08-20 — #13 / PR #38

状态：COMPLETED，merge `4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
