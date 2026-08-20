# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已经实现什么、正在交付什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：Issue #41 / PR #42 — master recovery；Issue #13 / PR #38 — Workbench Platform Foundation
- 当前交付：Issue #39 / PR #40 — Workbench Overview API Foundation
- 下一业务阶段：Task Backend
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目目标

Group-IM 是面向组织协作的多端 IM 与办公平台。即时通信是协作主链路，Workbench 承载结构化办公能力，AI / Automation 嵌入会话和业务流程。

长期原则：

1. 消息是协作主链路；
2. Workbench 承载 Task / Approval / Schedule / Announcement / Report；
3. AI / Automation 不建立与业务服务冲突的第二份真相；
4. Web/Electron 与 Android 保持核心业务语义一致；
5. 多租户、权限、审计、迁移、文件和通知作为平台能力复用。

---

## 2. 仓库治理

- `master` 是唯一开发主线；
- `main` 是历史遗留分支，不参与正常开发与发布；
- 所有代码、数据库、配置和文档变更必须通过 PR；
- Bug 必须有 Issue；重要功能/架构/数据库变更原则上也必须先有 Issue；
- 每个 PR 必须更新本文件；
- 默认 Squash Merge；
- 分支命名必须满足 Governance CI；
- 不以 force-reset / history rewrite 修复 master 事故，优先可审计 hotfix PR。

项目原则：

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。

### 已完成关键基础

- PR #5：Repository Governance / PROJECT_MASTER / templates / ADR；
- PR #16：Backend PR Validation；
- #12 Tenant Versioned Migration Epic：#18/#19/#26/#32/#25/#20/#21 完成；
- PR #38 / #13：Workbench Platform Foundation；
- PR #42 / #41：恢复 accidental direct Overview commits 后的 master 完整性，无 history rewrite。

### 仍待治理项

- #6：master protection / required checks；
- #7：移除 legacy `main` deploy trigger；
- #9：Electron/Web PR CI；
- #22：Maven duplicate dependency warnings。

---

## 3. 技术架构概览

### Server

Java 21 / Spring Boot 3.x / Maven multi-module / Spring Security + JWT / Spring Data JPA / PostgreSQL schema multi-tenancy / Redis / WebSocket / Spring AI。

### Client

Electron + React + TypeScript；Kotlin Multiplatform + Compose Android。

### Tenant database current facts

- PostgreSQL schema 是 company tenant boundary；
- `spring.flyway.enabled=false`，migration 由显式 runtime 驱动；
- core business baseline = `2026081906`；
- managed current target = `2026082001`；
- 新公司：inactive reservation → CREATE SCHEMA → Flyway → verify → active；
- 既有 tenant：read-only preflight → explicit baseline → migrate/validate → audit/state；
- default `spring.jpa.hibernate.ddl-auto=update`；
- `application-schema-validate.yml` 仅供 staged rollout；
- legacy public clone / Safe Sync 是 deprecated transitional compatibility，不是 migration authority。

测试 tenant 数据不作为产品事实；需要时可清理重建，不阻塞 Workbench 功能实现。

---

## 4. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| 登录/鉴权 | STABLE | JWT/Spring Security；inactive company 不可进入 | RBAC 细化 |
| 多公司/多租户 | STABLE | #12 migration foundation 完成 | validate rollout / observability |
| 单聊/群聊 | STABLE | 核心 IM 主链路存在 | 搜索/治理/一致性 |
| 联系人/组织 | STABLE | 公司/部门/员工能力存在；Workbench adapter 已建立 | 权限治理 |
| 文件 | IN_PROGRESS | 上传/分片存在；Workbench availability adapter 已建立 | 资源级授权 |
| 会议 | IN_PROGRESS | Meeting 服务与多端入口存在 | Overview 聚合 / 协作联动 |
| AI 助手 | IN_PROGRESS | AI/Bot 能力持续演进 | 工具治理 |
| 群自动化 | IN_PROGRESS | 规则/执行/管理存在 | 审批/审计 |
| Workbench Platform | STABLE | #13 / PR #38 context/permission/audit/adapters/tenant executor | 供领域复用 |
| Workbench Overview | IN_PROGRESS | #39 / PR #40 后端 API 正在交付 | Web/Electron Overview |
| OA Task | PLANNED | 正式领域设计已形成 | #39 后 Backend |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后 |
| Tenant Migration | STABLE | #12 完成；baseline 1906 / target 2001 | 支撑 OA migrations |
| Backend PR CI | STABLE | Java 21 compile + tests | #6 required check |

---

## 5. Workbench Platform Foundation — #13 COMPLETED

```text
com.github.im.server.workbench.common
├── context
├── error
├── permission
├── audit
├── integration
└── tenant
```

当前事实：

- `CurrentWorkContext` 从 authenticated `User.currentCompany` 获得 user/company/schema；
- current company 必须 active；
- 已绑定 `SchemaContext` 与认证公司 schema 不一致时 fail closed；
- client companyId 不作为 Workbench tenant route；
- Workbench stable errors 使用 `WORKBENCH_*`；
- `WorkbenchPermissionService` 不使用 `username == admin`；
- active company member 是最低 feature permission baseline；
- 未配置高权限策略 fail closed；
- Organization/File adapters 隔离旧模块；
- File adapter 只证明 tenant-local availability，不代表业务资源授权；
- `WorkbenchAuditService` 提供统一审计边界；
- Background job 使用 explicit `WorkbenchTenantScope(companyId,schemaName)` + `WorkbenchTenantExecutor`，不依赖 HTTP filter。

PR #38 merge：`4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

---

## 6. Workbench Overview — #39 / PR #40

目标 endpoint：

```http
GET /api/workbench/overview
```

首版 response：

```text
currentCompany
+ todoSummary
+ recentTasks
+ pendingApprovals
+ todaySchedules
+ announcements
+ quickApps
```

实现规则：

1. tenant 只来自 #13 `CurrentWorkContext`；
2. 先 require `WorkbenchPermission.VIEW_WORKBENCH`；
3. response 不暴露 `schemaName`；
4. Task/Approval/Announcement 尚未实现时返回 count=0 / list=[]；
5. 不建立 Overview 写模型、临时 Todo 表或伪数据；
6. todaySchedules 首版聚合现有 Meeting 真相；
7. Meeting 使用 JPQL constructor projection，只读取 Overview 所需字段；
8. 只包含当前用户 participant 且 participant status 为 INVITED/JOINED/LEFT；REJECTED 明确排除；
9. Meeting status 只包含 SCHEDULED/ACTIVE；
10. scheduledAt 落在当天，或 scheduledAt 为空时 startedAt 落在当天；
11. Quick Apps 只返回 stable key/title；
12. 首版不缓存。

现有 Meeting 的日期字段是 `LocalDateTime`，Overview 首版显式沿用服务器默认时区解释“今天”；未来引入 user/company timezone 必须通过独立兼容变更完成。

正式文档：`doc/features/workbench/overview.md`。

---

## 7. Tenant Versioned Migration — #12 COMPLETED

Core baseline：`2026081906`。Managed target：`2026082001`。

关键规则：

1. `<tenant>.flyway_schema_history` 是 migration authority；
2. migrations immutable，只新增版本；
3. normal APPLY 仅对 empty/Flyway-managed tenant；
4. non-empty/no-history 必须 preflight + explicit baseline；
5. advisory lock；
6. 禁止 blind baselineOnMigrate；
7. public/current schema 不动态充当 expected contract；
8. Workbench/OA 新表必须从 current target 之后追加 migration。

---

## 8. Repository Incident #41 — COMPLETED

#39 开发期间四次 connector 逐文件更新意外进入 default `master`。处理方式：

```text
Issue #41
→ hotfix/41-revert-overview-master
→ PR #42
→ restore verified PR #38 content tree
→ Governance / Backend / KMP green
→ Squash Merge 85b78f1236169774aa2befd5bf11f9ccb3b6fd20
```

没有 force-reset master，没有删除或改写公开历史。#39 feature branch在 repaired master 上通过一次性 tree/commit 重建，后续禁止使用该逐文件默认分支路径更新 feature 文件。

---

## 9. CI / Merge Gates

Backend：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

所有 PR 继续以：

- Repository Governance；
- Backend PR Validation（适用路径）；
- Build KMP APK；
- no unresolved review threads；
- PROJECT_MASTER 同步；

作为 merge gate。

---

## 10. Roadmap

```text
#13 Platform Foundation ✅
        ↓
#39 Overview API ← CURRENT
        ↓
Task Backend
        ↓
Task Web/Electron
        ↓
Task Realtime / Push / WORKBENCH Card
        ↓
Approval Backend / UI
        ↓
Calendar / Announcement / Android OA / Report / AI Office
```

#14 structured card protocol design 已接受；server actual WORKBENCH emission 继续受 client-first gate 约束。

---

## 11. Change Log

### 2026-08-20 — Issue #39 / PR #40

状态：`IN_PROGRESS`。

- stable Overview DTO；
- current company 不暴露 schema；
- future domains zero/empty；
- current-user today Meeting lightweight projection；
- rejected invitations excluded；
- Quick Apps stable metadata；
- no cache / no write model。

### 2026-08-20 — Issue #41 / PR #42

状态：`COMPLETED`，merge `85b78f1236169774aa2befd5bf11f9ccb3b6fd20`。

### 2026-08-20 — Issue #13 / PR #38

状态：`COMPLETED`，merge `4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

### 2026-08-20 — Issue #12

状态：`COMPLETED`，core baseline `2026081906`，managed target `2026082001`。
