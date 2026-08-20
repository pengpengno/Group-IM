# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已经实现什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：Issue #12 — Tenant Versioned Migration Epic
- 当前下一阶段：Issue #13 — Workbench Platform Foundation
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
- Bug 必须有 Issue；重要功能、架构、数据库变更原则上也必须先有 Issue；
- 每个 PR 必须更新本文件；
- 默认 Squash Merge。

### 已实现治理 / 基础设施

- PR #5：Issue-driven PR、PROJECT_MASTER、模板、Governance CI、ADR；
- PR #16：Backend PR Validation；
- PR #23 / #18：ADR-0004 Tenant Migration Architecture；
- PR #24 / #19：Tenant Migration Runtime；
- PR #27 / #26：Trusted Tenant Schema Snapshot Tooling；
- #32：Trusted Snapshot Review；
- PR #33 / #25：Core Tenant Baseline Migrations；
- PR #35 / #20：New Tenant Provisioning；
- PR #36 / #21：Existing Tenant Baseline / Validate；
- Issue #12：Tenant Versioned Migration Epic — COMPLETED。

### 仍待治理项

- #6：保护 `master` 并设置 required checks；
- #7：部署 workflow 移除历史 `main` trigger；
- #9：Electron/Web PR CI；
- #22：清理 Maven duplicate dependency warnings。

---

## 3. 技术架构概览

### Server

Java 21 / Spring Boot 3.x / Maven multi-module / Spring Security + JWT / Spring Data JPA / PostgreSQL schema multi-tenancy / Redis / WebSocket / Spring AI。

### Client

Electron + React + TypeScript；Kotlin Multiplatform + Compose Android。

### Tenant database current facts

- PostgreSQL schema 是 company tenant boundary；
- `spring.flyway.enabled=false`，migration 由显式 runtime 驱动；
- 默认 `spring.jpa.hibernate.ddl-auto=update`，尚未全量切 `validate`；
- #19 runtime 提供 PLAN/APPLY/retry、public control plane、advisory lock；
- #25 core business baseline 固定为 `2026081906`；
- 当前 managed migration target 为 `2026082001`；
- #20 新公司主路径：inactive reservation → CREATE SCHEMA → Flyway → verify → active；
- login/company switch 只接受 active company；
- #21 既有 tenant：read-only preflight → explicit baseline → migrate/validate → audit/state；
- legacy `public.create_or_sync_company_schema(...)` / Safe Sync 只保留为 deprecated transitional compatibility surface，不再是 provisioning 或 versioned release authority；
- opt-in `application-schema-validate.yml` 已存在，但只有 active tenant coverage 完成后才允许 staged rollout。

---

## 4. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| 登录/鉴权 | STABLE | JWT/Spring Security；inactive company 不可登录/切换 | RBAC 细化 |
| 多公司/多租户 | STABLE | Runtime + core baseline + new provisioning + existing baseline path | 真实 tenant rollout / observability |
| 单聊/群聊 | STABLE | 核心 IM 主链路存在 | 搜索/治理/一致性 |
| 联系人/组织 | STABLE | 公司/部门/员工能力存在 | 权限治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级授权 |
| 会议 | IN_PROGRESS | Meeting 服务与多端入口存在 | 协作联动 |
| AI 助手 | IN_PROGRESS | AI/Bot 能力持续演进 | 工具治理 |
| 群自动化 | IN_PROGRESS | 规则/执行/管理存在 | 审批/审计 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；正式业务设计已合并 | #13 Platform Foundation |
| OA Task | PLANNED | 正式领域设计已形成 | #13 后 Backend |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后 |
| Tenant Migration Foundation | STABLE | #12 全阶段完成；baseline 1906 / target 2001 | 支撑 Workbench 新 migration |
| Backend PR CI | STABLE | Java 21 compile + tests | #6 required check |
| Repository Governance | IN_PROGRESS | PR/Issue/PROJECT_MASTER/Governance CI 已有 | #6/#7/#9/#22 |

---

## 5. Workbench / OA

正式设计：

- `doc/features/workbench/README.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval；
- ADR-0005：Workbench structured card / client event protocol。

### 当前依赖链

```text
#12 Tenant Versioned Migration Epic ✅ COMPLETED
        ↓
#13 Workbench Platform Foundation ← NEXT
        ↓
Workbench Overview
        ↓
Task Backend
        ↓
Task Web/Electron
        ↓
Realtime / Push / Structured Card
        ↓
Approval Backend / UI
```

#14 protocol design 已接受；#28/#29/#30 按 client-first rollout gate 推进，server actual WORKBENCH emission 不能早于客户端兼容。

---

## 6. Tenant Versioned Migration — #12 COMPLETED

Canonical docs：

- `doc/architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md`
- `doc/architecture/tenant-migration/README.md`

### Runtime rules

1. `<tenant>.flyway_schema_history` 是 tenant migration authority；
2. PLAN 只读；
3. normal APPLY 只允许 empty schema 或已有可信 Flyway history；
4. non-empty + no-history tenant 必须走 existing-tenant preflight/baseline；
5. 同 tenant 使用 PostgreSQL advisory lock；
6. migration 发布后不可修改，只新增版本；
7. 禁止 blind `baselineOnMigrate(true)`；
8. public/current schema 不能动态充当 expected contract。

### Core business baseline — `2026081906`

```text
V2026081901 tenant schema metadata
V2026081902 automation/collaboration tables
V2026081903 file/friendship/meeting tables
V2026081904 message/status/config/user tables
V2026081905 PK/UNIQUE/FK relationships
V2026081906 tenant identity views
```

Reviewed contract：

- 18 core tables；
- 3 tenant identity views；
- 17 identity sequences；
- 65 core constraints；
- 26 PK/UNIQUE backing indexes。

Normalization：

- `messages.content=TEXT`；
- `meetings.scheduled_at=timestamp(6) without time zone`；
- `messages.type` CHECK 包含 `BOT_CARD`；
- global identity authority 位于 `public.company` / `public.company_user` / `public.users`。

### Managed current target — `2026082001`

Tenant migration：

```text
V2026082001__record_core_baseline_contract.sql
```

不修改 core business tables，只确保 `tenant_schema_metadata` 并记录：

```text
migration_runtime = group-im
core_baseline_contract = 2026081906
```

因此必须区分：

```text
core business baseline = 2026081906
managed current target = 2026082001
```

未来 Workbench/OA tenant table 必须从 current target 之后继续追加 immutable migrations。

---

## 7. New Tenant Provisioning — #20 / PR #35

状态：`IMPLEMENTED`。

```text
validate metadata
  ↓
reserve public.company active=false (REQUIRES_NEW)
  ↓
CREATE SCHEMA if missing
  ↓
Flyway APPLY current tenant migrations
  ↓
verify no pending
  ↓
mark active=true (REQUIRES_NEW)
  ↓
publish CompanyCreatedEvent
```

- missing / empty / Flyway-managed schema 可 provision/retry；
- non-empty + no-history 拒绝并转 existing-tenant baseline；
- migration failure 保持 inactive；
- inactive tenant 不可登录/切换；
- CompanyCreatedEvent 不承担 schema DDL；
- legacy public clone 不再是新公司主路径。

---

## 8. Existing Tenant Baseline / Validate — #21 / PR #36

状态：`IMPLEMENTED`。

### Preflight

Read-only semantic contract 检查：

- core tables；
- columns / type / nullability / default / identity / generated；
- PK / UNIQUE / FK / CHECK；
- indexes；
- view output shape；
- identity sequences；
- tenant `company/company_user/users` 的实际隔离行为。

分类：

- `BASELINE_READY`；
- `DRIFTED`；
- `CONFLICT`；
- `ERROR`。

Drift / Conflict 只返回 repair plan，不自动 ALTER/DROP，不复制 public 覆盖 tenant。

### Portable fingerprint

- same-tenant FK referenced schema 归一化为 `<tenant>`；
- global/public FK 保留 `public`；
- 因此不同真实 tenant schema 名可比较同一个 canonical contract，同时不会隐藏跨 tenant/global boundary 错误。

### Explicit baseline

```text
preflight
  ↓
operator supplies expectedFingerprint
  ↓
advisory lock
  ↓
re-preflight
  ↓
require BASELINE_READY + no history + fingerprint unchanged
  ↓
explicit Flyway.baseline(2026081906)
  ↓
Flyway.migrate() -> 2026082001
  ↓
Flyway.validate()
  ↓
post-fingerprint
  ↓
audit + tenant_schema_state=UP_TO_DATE
```

Admin API：

```http
POST /api/admin/schema-migrations/baselines/preflight
GET  /api/admin/schema-migrations/baselines/states
POST /api/admin/schema-migrations/baselines/companies/{companyId}
```

Public control plane `V2026082001` 创建：

- `tenant_schema_preflight_state`；
- `tenant_schema_baseline_audit`。

---

## 9. Real Tenant Rollout

Migration mechanism 完成不等于历史 tenant 已经全部被改写。

`dingding`（钉钉）、`pingduoduo`、`yuansheng` 必须按以下顺序运营接管：

```text
部署含 PR #36 的 master
  ↓
public bootstrap -> 2026082001
  ↓
read-only baseline preflight
  ↓
BASELINE_READY -> 使用返回 fingerprint 显式 baseline
DRIFTED/CONFLICT -> review repair plan first
```

`dingding` 是在 PR #35 合并前注册，因此不能直接当作 migration-backed new tenant success evidence。

---

## 10. Hibernate Validate Staged Rollout

默认继续：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Opt-in：

```text
application-schema-validate.yml
→ ddl-auto: validate
```

启用 gate：

1. 所有 active tenant 有最新 preflight；
2. 无 `DRIFTED / CONFLICT / ERROR`；
3. 所有 active tenant 都有 Flyway history 且 current >= `2026082001`；
4. new provisioning 已稳定；
5. CI 绿；
6. staging validate；
7. production canary；
8. production full rollout。

Rollback 仅撤回 validate profile/property 回默认 `update`；不删除 Flyway history、不反向修改 tenant schema。

---

## 11. Legacy Compatibility Boundary

仍存在：

```text
POST /api/organization/company/sync-schema
GET  /api/organization/company/schema-sync/status
POST /api/organization/company/schema-sync/apply
```

它们是 `DEPRECATED / TRANSITIONAL COMPATIBILITY`，不是：

- new tenant provisioning；
- expected schema contract；
- Flyway history authority；
- normal versioned release path。

真实 active tenant coverage 完成后，可以单独创建 cleanup Issue 移除 legacy write path。

---

## 12. CI / Validation

Backend PR Validation：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

PostgreSQL integration coverage：

- `MigrationRuntimeIntegrationTest`；
- `CoreTenantBaselineIntegrationTest`；
- `TenantSchemaInventorySqlIntegrationTest`；
- `TenantSchemaProvisionerIntegrationTest`；
- `CoreTenantBaselineContractFingerprintTest`；
- `ExistingTenantBaselineIntegrationTest`。

PR #36 latest head 已通过 Repository Governance、Backend compile/tests、Build KMP APK。

---

## 13. 当前风险

1. #6 未完成，GitHub 尚未强制 master protection / required checks；
2. #9 未完成，Electron/Web 缺独立 PR build gate；
3. `dingding` / `pingduoduo` / `yuansheng` 尚未完成真实 preflight/baseline coverage；
4. 默认 `ddl-auto=update` 尚未切到 validate；
5. #22 Maven duplicate dependency warnings 尚未清理；
6. MigrationAdminAuthorizer 仍是 configured-admin bridge；
7. legacy schema sync write path 仍存在；
8. Workbench protocol actual emission 仍受 client-first gate；
9. attachment/resource authorization 仍需加强。

---

## 14. Roadmap

### P0 Governance

#6 / #7 / #9 / #22。

### P1 Platform Foundation

```text
#12 Tenant Versioned Migration Epic ✅ COMPLETED
        ↓
#13 Workbench Platform Foundation ← NEXT
        ↓
Overview / Task Backend
```

### P1 Workbench Business

Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 OA Expansion

Calendar → Announcement → Android OA → Report / AI Office。

---

## 15. Change Log

### 2026-08-20 — Issue #12 — Tenant Versioned Migration Epic

状态：`COMPLETED`。

- #18 Migration Architecture / ADR-0004 ✅；
- #19 Migration Runtime ✅ PR #24；
- #26 Snapshot Tooling + #32 Review ✅；
- #25 Core Tenant Baseline `2026081906` ✅ PR #33；
- #20 New Tenant Provisioning ✅ PR #35；
- #21 Existing Tenant Baseline / Validate ✅ PR #36；
- managed target 推进至 `2026082001`；
- 下一阶段切换为 #13 Workbench Platform Foundation。

### 2026-08-20 — Issue #21 / PR #36

状态：`IMPLEMENTED`。

- semantic preflight/fingerprint；
- explicit fingerprint-bound baseline；
- baseline 1906 / target 2001 分离；
- non-destructive drift/conflict rejection；
- public audit/state control plane；
- staged validate profile；
- PostgreSQL ready/drift/conflict integration coverage。

### 2026-08-20 — Issue #20 / PR #35

状态：`IMPLEMENTED`。

### 2026-08-19 — Issue #25 / PR #33

状态：`IMPLEMENTED`，canonical business baseline `2026081906`。

---

项目原则：

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
