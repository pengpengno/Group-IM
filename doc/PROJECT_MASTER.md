# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已实现什么、正在实现什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 当前数据库执行项：Issue #21 Existing Tenant Baseline / Validate（branch `feature/21-existing-tenant-baseline`）
- 最近完成：Issue #20 / PR #35 — New Tenant Provisioning
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
- `main` 是历史遗留分支；
- 所有代码/数据库/文档变更必须通过 PR；
- Bug 必须有 Issue；重要功能/架构/DB 变更原则上也必须先有 Issue；
- 每个 PR 必须更新本文件；
- 默认 Squash Merge。

已实现：

- PR #5：Issue-driven PR、PROJECT_MASTER、模板、Governance CI、ADR；
- PR #16：Backend PR Validation；
- PR #23 / #18：ADR-0004 Tenant Migration Architecture；
- PR #24 / #19：Tenant Migration Runtime；
- PR #27 / #26：Trusted Tenant Schema Snapshot Tooling；
- #32：Trusted Snapshot Review；
- PR #33 / #25：Core Tenant Baseline Migrations，core baseline `2026081906`；
- PR #35 / #20：New Tenant Provisioning，migration-backed new-company lifecycle。

待完成：#6 master protection、#7 remove legacy `main` deploy trigger、#9 Electron/Web CI、#22 Maven duplicate dependencies。

---

## 3. 技术架构概览

### Server

Java 21 / Spring Boot 3.x / Maven multi-module / Spring Security + JWT / Spring Data JPA / PostgreSQL schema multi-tenancy / Redis / WebSocket / Spring AI。

### Client

Electron + React + TypeScript；Kotlin Multiplatform + Compose Android。

### Tenant database current facts

- PostgreSQL schema 是 company tenant boundary；
- `spring.flyway.enabled=false`，migration 由显式 runtime 驱动；
- 默认 `spring.jpa.hibernate.ddl-auto=update` 暂不改变；
- #19 runtime 提供 PLAN/APPLY/retry、advisory lock、control plane；
- #25 core baseline `2026081906` 已证明空 schema 可只靠 Flyway 建完整 core schema；
- #20 新公司主路径：inactive reservation → CREATE SCHEMA → Flyway → active；
- login/company switch 只接受 active company；
- legacy `public.create_or_sync_company_schema(...)` 仅保留给显式 compatibility sync；
- #21 正在建立既有 tenant 的只读 preflight、显式 baseline、audit 和 staged validate gate；
- #21 引入 post-baseline non-destructive migration target `2026082001`；**core baseline 仍固定为 `2026081906`**。

---

## 4. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| 登录/鉴权 | STABLE | JWT/Spring Security；company availability 受 active gate 约束 | RBAC 细化 |
| 多公司/多租户 | IN_PROGRESS | Runtime + core baseline + new provisioning；existing baseline WIP | #21 |
| 单聊/群聊 | STABLE | 核心 IM 主链路存在 | 搜索/治理/一致性 |
| 联系人/组织 | STABLE | 公司/部门/员工能力存在 | 权限治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级授权 |
| 会议 | IN_PROGRESS | Meeting 服务与多端入口存在 | 协作联动 |
| AI 助手 | IN_PROGRESS | AI/Bot 能力持续演进 | 工具治理 |
| 群自动化 | IN_PROGRESS | 规则/执行/管理存在 | 审批/审计 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；正式设计已合并 | #12/#13 后 Overview |
| OA Task | PLANNED | 领域设计已形成 | platform foundation 后实现 |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后 |
| Tenant Migration Runtime | STABLE | #19 / PR #24 | coverage rollout |
| Core Tenant Baseline | STABLE | #25 / PR #33，baseline `2026081906` | immutable contract |
| New Tenant Provisioning | STABLE | #20 / PR #35 | observe + #21 |
| Existing Tenant Baseline | IN_PROGRESS | #21：preflight/fingerprint/audit/explicit baseline | CI → test tenants |
| Backend PR CI | STABLE | Java 21 compile + tests | #6 required check |

---

## 5. Workbench / OA

正式设计：

- `doc/features/workbench/README.md`
- `task.md`
- `approval.md`
- `platform-integration.md`
- `implementation-roadmap.md`

关键 ADR：ADR-0002 modular monolith；ADR-0003 Task-first；ADR-0005 structured Workbench card/event protocol。

依赖链：

```text
#12 Tenant Migration Epic
  #18 ✅ Architecture
  #19 ✅ Runtime
  #26 ✅ Snapshot Tooling
  #32 ✅ Snapshot Review
  #25 ✅ Core Baseline
  #20 ✅ New Tenant Provisioning / PR #35
  #21 ← IN PROGRESS Existing Tenant Baseline/Validate
      ↓
#12 complete
      ↓
#13 Workbench Platform Foundation
      ↓
Overview / Task Backend
```

#14 protocol implementation (#28/#29/#30) 可并行，但 actual WORKBENCH emission 仍受 client-first rollout gate 约束。

---

## 6. Tenant Versioned Migration

Canonical docs：

- `doc/architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md`
- `doc/architecture/tenant-migration/README.md`

### Runtime rules

1. `<tenant>.flyway_schema_history` 是 tenant migration authority；
2. PLAN 只读；
3. normal APPLY 只允许 empty schema 或已有 Flyway history；
4. non-empty + no-history tenant 必须走 #21；
5. 同 tenant 使用 PostgreSQL advisory lock；
6. migration immutable，发布后只新增版本；
7. 禁止 blind `baselineOnMigrate(true)`。

### Core baseline contract

```text
V2026081901 tenant schema metadata
V2026081902 automation/collaboration tables
V2026081903 file/friendship/meeting tables
V2026081904 message/status/config/user tables
V2026081905 PK/UNIQUE/FK relationships
V2026081906 tenant identity views
```

Reviewed contract：18 core tables / 3 views / 17 identity sequences / 65 constraints / 26 PK/UNIQUE indexes。

Normalization：`messages.content=TEXT`、`meetings.scheduled_at=timestamp(6)`、message type CHECK 包含 `BOT_CARD`；tenant identity views 绑定 global identity，不把 captured companyId 当 canonical contract。

### Post-baseline managed target

#21 新增：

```text
V2026082001__record_core_baseline_contract.sql
```

它只写 `tenant_schema_metadata`，不改 core business tables。既有 tenant 被显式 baseline 在 `2026081906` 后必须成功执行该 migration，才能证明它已进入正常 Flyway lifecycle。

---

## 7. Issue #20 / PR #35 — New Tenant Provisioning

状态：`IMPLEMENTED`。

```text
validate metadata
        ↓
reserve public.company active=false (REQUIRES_NEW)
        ↓
create schema if missing
        ↓
Flyway APPLY all tenant migrations
        ↓
verify no pending
        ↓
mark active=true (REQUIRES_NEW)
        ↓
publish CompanyCreatedEvent
```

- missing / empty / Flyway-managed schema 可 provision/retry；
- non-empty + no-history 拒绝并转 #21；
- admin retry：`POST /api/admin/tenant-provisioning/companies/{companyId}/retry`；
- inactive tenant 不可登录/切入；
- legacy sync 不是新公司主路径。

测试环境的 `钉钉 / dingding` 在 #20 合并前已创建，因此不能自动当作 #20 成功证据。

---

## 8. Issue #21 — Existing Tenant Baseline / Validate

状态：`IN_PROGRESS`。

目标流程：

```text
active tenant
  ↓
read-only catalog + identity-view behavior preflight
  ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
  ↓
explicit operator authorization + expectedFingerprint
  ↓
advisory lock + fingerprint re-check
  ↓
Flyway explicit baseline 2026081906
  ↓
run V2026082001 + validate
  ↓
post-check fingerprint + audit
```

### Preflight contract

只读检查：

- core tables；
- columns / type / nullability / default / identity；
- PK / UNIQUE / FK / CHECK；
- indexes；
- view shape；
- identity sequences；
- `company` / `company_user` / `users` 的实际 tenant isolation 行为。

分类：

- `BASELINE_READY`：结构与 pinned baseline contract 一致；
- `DRIFTED`：核心对象存在但 column/constraint/index/view/sequence contract 不一致；
- `CONFLICT`：缺失/额外 core table/view、schema 不存在等结构级冲突；
- `ERROR`：catalog/preflight 执行失败。

Drift/Conflict 只返回 repair plan；不会自动 drop/alter/copy public。

### Admin API (WIP)

```http
POST /api/admin/schema-migrations/baselines/preflight
GET  /api/admin/schema-migrations/baselines/states
POST /api/admin/schema-migrations/baselines/companies/{companyId}
```

baseline 请求必须携带最近 preflight 返回的 `expectedFingerprint`，并在 advisory lock 内重新计算，防止 preflight 后结构变化。

### Audit / control plane (WIP)

Public migration `V2026082001` 建立：

- `tenant_schema_preflight_state`；
- `tenant_schema_baseline_audit`。

Baseline audit 记录 operator、baseline version、expected/observed fingerprint、成功/失败状态。

Candidate set：`pingduoduo`、`yuansheng`，以及任何 #20 合并前创建且 non-empty/no-history 的 tenant（可能包括 `dingding`）。

---

## 9. Hibernate validate rollout

默认配置继续：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

#21 新增 opt-in profile：

```text
application-schema-validate.yml
→ ddl-auto: validate
```

启用 gate：

1. 所有 active tenant 都已有最新 preflight 结果；
2. 不存在 `DRIFTED / CONFLICT / ERROR`；
3. 所有 active tenant 都有 Flyway history 且 current version ≥ `2026082001`；
4. #20 new provisioning 已稳定；
5. Backend/Governance CI 通过；
6. 先 staging `schema-validate`，再 production canary，最后全量。

Rollback：只撤回 validate profile/property 回默认 `update`；**不删除 Flyway history、不反向修改 tenant schema**，先定位 drift 后再重新推进。

---

## 10. Legacy compatibility boundary

以下路径仍暂时存在：

```text
POST /api/organization/company/sync-schema
GET  /api/organization/company/schema-sync/status
POST /api/organization/company/schema-sync/apply
```

它们属于 deprecated/transitional compatibility surface，不是 migration authority。#21 完成 active tenant coverage 后再单独移除 write path。

---

## 11. CI / Validation

Backend PR Validation：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

Stable PostgreSQL tests：

- `MigrationRuntimeIntegrationTest`；
- `CoreTenantBaselineIntegrationTest`；
- `TenantSchemaInventorySqlIntegrationTest`；
- `TenantSchemaProvisionerIntegrationTest`；
- #21 canonical fingerprint pin test（WIP）；
- #21 existing-tenant baseline + validate integration test（待加入）。

---

## 12. Current Risks

1. #6 未完成，master 尚未强制保护；
2. #9 未完成，Electron/Web 缺独立 build gate；
3. #21 未合并/覆盖前，legacy tenants 仍未统一纳入 Flyway history；
4. #22 Maven duplicate dependency warnings 尚未清理；
5. 默认 `ddl-auto=update` 尚未切到 validate；
6. MigrationAdminAuthorizer 仍是 configured-admin bridge；
7. legacy schema sync write path 仍存在；
8. Workbench protocol 仍需 client-first rollout；
9. attachment/resource authorization 仍需后续加强。

---

## 13. Roadmap

### P0 Governance

#6 / #7 / #9 / #22。

### P1 Tenant / Workbench Foundation

```text
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline/Validate ← IN PROGRESS
#12 Tenant Migration Epic complete
#13 Workbench Platform Foundation
Overview / Task Backend
```

### P1 Workbench Business

Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 OA Expansion

Calendar → Announcement → Android OA → Report / AI Office。

---

## 14. Change Log

### 2026-08-20 — Issue #21 — Existing Tenant Baseline / Validate

状态：`IN_PROGRESS`

- 建立 semantic catalog fingerprint / preflight classification；
- 建立 explicit baseline + expectedFingerprint TOCTOU guard；
- 建立 public preflight/audit control plane；
- core baseline 固定 `2026081906`，managed target 推进到 `2026082001`；
- 新增 opt-in `schema-validate` profile，默认仍为 update；
- `dingding` 等既有 tenant 必须先 preflight，禁止手工 blind baseline。

### 2026-08-20 — Issue #20 / PR #35 — New Tenant Provisioning

状态：`IMPLEMENTED`

- 新 tenant 主路径切为 migration-backed provisioning；
- company 先 inactive reservation，成功后再 active；
- migration failure 保持 inactive 并允许显式 retry；
- login / company switch 强制 active availability gate；
- CompanyCreatedEvent 移除 DDL；
- legacy non-empty/no-history tenant 明确转 #21。

### 2026-08-19 — Issue #25 / PR #33 — Core Tenant Baseline

状态：`IMPLEMENTED`，canonical baseline `2026081906`。

### 2026-08-19 — #32 / #26 / #19 / #18

- #32 Trusted Snapshot Review：COMPLETED；
- #26 / PR #27 Snapshot Tooling：IMPLEMENTED；
- #19 / PR #24 Migration Runtime：IMPLEMENTED；
- #18 / PR #23 ADR-0004：IMPLEMENTED。

---

项目原则：

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
