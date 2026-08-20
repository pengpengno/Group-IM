# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：#45 / PR #46 Workbench Task Backend V1
- 当前交付：#9 Electron/Web PR Validation
- 下一业务交付：#47 Workbench Task Web/Electron
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

Workbench 已有静态入口，但 #47 开始前仍未读取 Overview，也没有 Task 页面。

### #9 Electron/Web PR Validation — CURRENT

新增 `.github/workflows/electron-web.yml`：

- 仅在 Electron/Web 路径或 workflow 自身变化时触发；
- Node.js 20；
- `npm ci`；
- production desktop env + `npm run app:build`；
- `npm run web:build`；
- concurrency cancel-in-progress；
- 可作为未来 master required check。

项目目前没有稳定 lint/test script，因此 #9 先以 TypeScript/Webpack build 作为真实门禁；未来新增 lint/test 后再扩展。

---

## 6. 下一业务交付 — #47 Task Web/Electron

目标用户链路：

```text
Workbench Overview
→ Task Center
→ create
→ list/detail
→ Start / Block / Resume / Complete / Reopen / Cancel
→ comment / activity
→ refresh Overview
```

约束：

- API 复用现有 Bearer interceptor；
- Workbench/Task 使用后端真实 `ApiResponse { code, message, data, timestamp }`；
- client 不传 companyId 作为 tenant route；
- server 仍是 Task 唯一业务真相；
- 不在 #47 混入 Realtime / Push / WORKBENCH Card；
- 不在 #47 混入 Android Task UI 或 #29 structured-card renderer/deep-link。

---

## 7. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Workbench Platform | STABLE | #13 完成 | 领域复用 |
| Workbench Overview | STABLE | #39 + Task projection | #47 UI |
| Tenant Migration | STABLE | core 1906 / target 2002 | 后续 OA migrations |
| OA Task Backend | STABLE | #45 / PR #46 已合并 | UI / events |
| OA Task Web/Electron | PLANNED | #47 已登记 | Electron/Web UI |
| Electron/Web PR CI | IN_PROGRESS | #9 正在交付 | merge 后成为 applicable gate |
| OA Approval | PLANNED | 设计已完成 | Task 闭环后 |

---

## 8. CI / Merge Gates

已存在：

- Repository Governance；
- Backend PR Validation；
- Build KMP APK。

#9 合并后新增：

- Electron Web PR Validation：Electron/Web 相关 PR 执行 dependency install + Electron main/renderer build + Web build。

#6 仍负责把这些稳定 checks 配置为 master required checks。

---

## 9. Roadmap

```text
#13 Platform Foundation ✅
→ #39 Overview API ✅
→ #43 Baseline Scope ✅
→ #45 Task Backend ✅
→ #9 Electron/Web CI ← CURRENT ENABLER
→ #47 Task Web/Electron
→ Task Realtime / Push / WORKBENCH Card
→ Approval Backend / UI
→ Calendar / Announcement / Android OA / Report / AI Office
```

#14 structured card protocol design 已接受；server actual WORKBENCH emission 继续受 client-first gate。

---

## 10. Change Log

### 2026-08-20 — #9 Electron/Web PR Validation

状态：IN_PROGRESS。增加针对 `Group-app/Group-Electronjs/**` 的 PR build gate，验证 Electron main/renderer 与 Web bundle。

### 2026-08-20 — #45 / PR #46 Task Backend V1

状态：COMPLETED，merge `6ff1d1f99567e9d203ca1e27e20c47db24551d62`。Managed target 前进到 `2026082002`，Task 四表、状态机、API、资源权限、Activity/Audit、Overview Task 真数据已进入 master。

### 2026-08-20 — #43 / PR #44

状态：COMPLETED，merge `8384ebd69ef203b661f89db98b2169d35b06ec7e`。

### 2026-08-20 — #39 / PR #40

状态：COMPLETED，merge `979f1f9a0efe407efe4981f3880835d40490f9f5`。

### 2026-08-20 — #13 / PR #38

状态：COMPLETED，merge `4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
