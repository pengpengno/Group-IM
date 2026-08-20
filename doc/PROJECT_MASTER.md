# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已经实现什么、正在交付什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：Issue #39 / PR #40 — Workbench Overview API；Issue #41 / PR #42 — master recovery
- 当前交付：Issue #43 — Core Baseline Fingerprint Scope
- 下一业务阶段：Workbench Task Backend
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目目标

Group-IM 是面向组织协作的多端 IM 与办公平台。即时通信是协作主链路，Workbench 承载结构化办公能力，AI / Automation 嵌入会话和业务流程。

长期原则：消息是协作主链路；Workbench 承载 Task/Approval/Schedule/Announcement/Report；AI/Automation 不建立第二份业务真相；多租户、权限、审计、迁移、文件和通知作为平台能力复用。

---

## 2. 仓库治理

- `master` 是唯一开发主线；`main` 是 legacy；
- 所有代码/数据库/配置/文档变更通过 PR；
- Bug 必须有 Issue；重要功能/架构/DB 变更原则上先有 Issue；
- 每个 PR 更新本文；默认 Squash Merge；
- branch 必须满足 Governance CI；
- master 事故不 force-reset/history rewrite，使用可审计 hotfix PR；
- feature 文件更新使用 tree/commit/ref 路径，避免再次触发默认分支逐文件写入问题。

项目原则：

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。

仍待治理：#6 master protection、#7 legacy main deploy trigger、#9 Electron/Web PR CI、#22 Maven duplicate dependencies。

---

## 3. 当前技术事实

### Server / Client

Java 21 + Spring Boot 3.x + Maven；Spring Security/JWT；Spring Data JPA；PostgreSQL schema multi-tenancy；Redis/WebSocket/Spring AI。客户端为 Electron/React/TypeScript 与 Kotlin Multiplatform/Compose Android。

### Tenant migration

```text
core business baseline = 2026081906
managed current target = 2026082001
```

- #12 migration foundation 已完成；
- new tenant：inactive reservation → empty schema → Flyway → verify → active；
- existing no-history tenant：read-only preflight → explicit baseline → migrate/validate → audit；
- default `ddl-auto=update`，validate profile 仅 staged rollout；
- public clone / Safe Sync 是 deprecated compatibility；
- 测试 tenant 数据可清理重建，不阻塞功能实现。

### #43 Baseline Fingerprint Scope — CURRENT

Task 新表上线前必须修复 #21 的未来 migration 兼容性。

新契约：

```text
immutable core fingerprint
  = 1906 core tables/columns/constraints/indexes
  + identity views
  + core-owned sequences

full object inventory
  = all tenant tables/views/sequences
```

安全规则：

- **no history legacy tenant**：full inventory 必须没有 baseline 外未知 object；extra => `CONFLICT`；
- **Flyway-managed tenant**：later managed objects 不参与 core hash，不制造 false conflict；
- core pinned hashes 必须保持不变；
- existing history 仍禁止重复 baseline；
- baseline preflight 不是未来最新业务 schema 的全量 validator，latest schema 由 Flyway history/checksum/pending/validate + migration integration tests 保证。

---

## 4. Workbench 状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Workbench Platform | STABLE | #13 / PR #38 完成 | 领域复用 |
| Workbench Overview | STABLE | #39 / PR #40 已实现 `GET /api/workbench/overview` | 前端接入/随领域扩展 |
| Tenant Migration Compatibility | IN_PROGRESS | #43 core fingerprint scope | Task migration gate |
| OA Task Backend | BLOCKED_BY_43 | Task 正式设计已完成 | #43 后 migration + API |
| OA Approval | PLANNED | 轻量串行审批设计完成 | Task 闭环后 |

### Platform Foundation

`workbench.common` 已提供 CurrentWorkContext、stable errors、fail-closed PermissionService、Organization/File adapters、Audit、explicit WorkbenchTenantExecutor。客户端 companyId 不决定 tenant；不使用 `username == admin`。

### Overview

`GET /api/workbench/overview` 已在 PR #40 合并：

- currentCompany 只返回 companyId/name，不暴露 schema；
- Task/Approval/Announcement 未实现时 zero/empty；
- todaySchedules 使用 current-user Meeting lightweight projection；
- participant INVITED/JOINED/LEFT，排除 REJECTED；meeting SCHEDULED/ACTIVE；
- no cache / no independent write model；
- Quick Apps 返回 stable key/title。

---

## 5. Task Backend Ready Design

正式设计：`doc/features/workbench/task.md`。

核心表计划：

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

状态：`TODO / IN_PROGRESS / BLOCKED / COMPLETED / CANCELLED`。状态变化必须走 action API，不允许客户端直接 PATCH status。

权限：feature permission + resource/data permission；owner/creator/assignee/collaborator/department 等均服务端校验。

附件复用 File adapter，仍要求 current tenant + task visibility + relation；知道 fileId 不等于可下载。

事务内写 Task/Activity/Audit，通知/Realtime/Card 在 commit 后处理；第一版 Task Backend 不提前 emit WORKBENCH card。

---

## 6. CI / Merge Gates

Backend：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

PR merge gate：Repository Governance + applicable Backend + KMP + no unresolved review threads + PROJECT_MASTER update。

#43 PostgreSQL gate：

- original core pinned hashes unchanged；
- later managed table/view/sequence does not alter core fingerprint；
- no-history extra object remains CONFLICT；
- managed history tenant with later objects remains core-compatible；
- repeat baseline rejected。

---

## 7. Roadmap

```text
#13 Platform Foundation ✅
→ #39 Overview API ✅
→ #43 Baseline Fingerprint Scope ← CURRENT
→ Task Backend
→ Task Web/Electron
→ Task Realtime / Push / WORKBENCH Card
→ Approval Backend / UI
→ Calendar / Announcement / Android OA / Report / AI Office
```

#14 structured card protocol design 已接受；server actual WORKBENCH emission 继续受 client-first gate 约束。

---

## 8. Repository Incident #41

#39 开发期间 connector 逐文件更新意外进入 default master。#41 / PR #42 使用正常 hotfix PR 恢复内容，merge `85b78f1236169774aa2befd5bf11f9ccb3b6fd20`；无 force-reset，无公开历史改写。#39 随后从 repaired master 以单一 tree/commit 重建并通过 PR #40 合入。

---

## 9. Change Log

### 2026-08-20 — Issue #43

状态：`IN_PROGRESS`。分离 immutable core fingerprint 与 full tenant inventory，作为 Task migration 前置 gate。

### 2026-08-20 — Issue #39 / PR #40

状态：`COMPLETED`，merge `979f1f9a0efe407efe4981f3880835d40490f9f5`。

### 2026-08-20 — Issue #13 / PR #38

状态：`COMPLETED`，merge `4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

### 2026-08-20 — Issue #12

状态：`COMPLETED`，core baseline `2026081906`，managed target `2026082001`。
