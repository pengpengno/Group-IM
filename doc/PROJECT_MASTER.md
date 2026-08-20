# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已实现什么、正在实现什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 本次状态边界：Issue #20 / PR #35 — New Tenant Provisioning
- 当前数据库下一项：Issue #21 Existing Tenant Baseline / Validate
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
- PR #33 / #25：Core Tenant Baseline Migrations，version `2026081906`；
- PR #35 / #20：New Tenant Provisioning（以本 PR 合并为状态边界）。

待完成：#6 master protection、#7 remove legacy `main` deploy trigger、#9 Electron/Web CI、#22 Maven duplicate dependencies。

---

## 3. 技术架构概览

### Server

Java 21 / Spring Boot 3.x / Maven multi-module / Spring Security + JWT / Spring Data JPA / PostgreSQL schema multi-tenancy / Redis / WebSocket / Spring AI。

### Client

Electron + React + TypeScript；Kotlin Multiplatform + Compose Android。

### Tenant database current facts

- PostgreSQL schema 是 company tenant boundary；
- `spring.jpa.hibernate.ddl-auto` 仍为 `update`；
- `spring.flyway.enabled=false`；
- SafeTenantSchemaSync 是 transitional compatibility/drift 工具；
- #19 runtime 提供 PLAN/APPLY/retry、advisory lock、control plane；
- #25 baseline 已证明空 schema 可只通过 Flyway 建完整 core schema；
- canonical tenant version：`2026081906`；
- #20 新公司主路径：inactive reservation → CREATE SCHEMA → Flyway → active；
- login/company switch 只接受 active company；
- legacy `public.create_or_sync_company_schema(...)` 仅保留给显式 compatibility sync，不再用于新公司主路径。

---

## 4. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| 登录/鉴权 | STABLE | JWT/Spring Security；company availability 受 active gate 约束 | RBAC 细化 |
| 多公司/多租户 | IN_PROGRESS | Runtime + baseline + new provisioning 已形成 | #21 |
| 单聊/群聊 | STABLE | 核心 IM 主链路存在 | 搜索/治理/一致性 |
| 联系人/组织 | STABLE | 公司/部门/员工能力存在 | 权限治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级授权 |
| 会议 | IN_PROGRESS | Meeting 服务与多端入口存在 | 协作联动 |
| AI 助手 | IN_PROGRESS | AI/Bot 能力持续演进 | 工具治理 |
| 群自动化 | IN_PROGRESS | 规则/执行/管理存在 | 审批/审计 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；正式设计已合并 | #12/#13 后 Overview |
| OA Task | PLANNED | 领域设计已形成 | platform foundation 后实现 |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后 |
| Tenant Migration Runtime | STABLE | #19 / PR #24 | #21 |
| Core Tenant Baseline | STABLE | #25 / PR #33，version `2026081906` | coverage rollout |
| New Tenant Provisioning | STABLE | #20 / PR #35：migration-backed lifecycle | observe + #21 |
| Existing Tenant Baseline | PLANNED | #21 使用 2026081906 contract | NEXT |
| Backend PR CI | STABLE | Java 21 compile + tests | required check |

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
  #21 ← NEXT Existing Tenant Baseline/Validate
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
3. APPLY 只允许 empty schema 或已有 Flyway history；
4. non-empty + no-history tenant 进入 #21，不自动 baseline；
5. 同 tenant 使用 PostgreSQL advisory lock；
6. migration immutable，发布后只新增版本。

### Core baseline

```text
V2026081901 tenant schema metadata
V2026081902 automation/collaboration tables
V2026081903 file/friendship/meeting tables
V2026081904 message/status/config/user tables
V2026081905 PK/UNIQUE/FK relationships
V2026081906 tenant identity views
```

Reviewed contract：18 core tables / 3 views / 17 identity sequences / 65 constraints / 26 PK/UNIQUE indexes。

Normalization：`messages.content=TEXT`、`meetings.scheduled_at=timestamp(6)`、message type CHECK 包含 `BOT_CARD`；tenant identity views 通过 schema name 绑定 global identity，不硬编码 captured companyId。

---

## 7. Issue #20 / PR #35 — New Tenant Provisioning

状态：`IMPLEMENTED`（以本 PR 合并为状态边界）。

新公司生命周期：

```text
validate metadata
        ↓
reserve public.company as active=false
        ↓  REQUIRES_NEW
create schema if missing
        ↓
Flyway APPLY all tenant migrations
        ↓
verify migration completed
        ↓
mark company active=true
        ↓  REQUIRES_NEW
publish CompanyCreatedEvent
```

实现边界：

- `TenantSchemaProvisioner` 创建/恢复 tenant schema；
- missing / empty / Flyway-managed schema 可 provision/retry；
- non-empty + no-history schema 拒绝并转 #21；
- `CompanyProvisioningTransactionService` 固定 inactive/active 的独立事务边界；
- `CompanyCreatedEvent` 不再执行 DDL；
- admin retry：`POST /api/admin/tenant-provisioning/companies/{companyId}/retry`；
- `AuthenticationService` 只允许 active company 登录/切换，admin 也不能进入 inactive tenant；
- legacy `CompanyService.syncSchemas()` 暂时保留，但不是新 tenant 主路径。

验证：

- `TenantSchemaProvisionerIntegrationTest`：PostgreSQL 16 create→migrate→retry + legacy rejection；
- `CompanyServiceProvisioningSpec`：成功后才 active，失败保持 inactive；
- `AuthenticationServiceCompanyAvailabilitySpec`：inactive tenant 不可切入；
- Repository Governance / Backend compile+test / Build KMP APK 全绿。

测试环境已存在 `钉钉 / dingding`。由于它在 #20 合并前创建，必须先检查 `flyway_schema_history` 与 business-table 状态；不能直接当作 #20 新路径成功证据。

---

## 8. Issue #21 — Existing Tenant Baseline / Validate

状态：`NEXT`

```text
read-only inspect
  ↓
compare with 2026081906 contract
  ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
  ↓
explicit authorized baseline
  ↓
normal Flyway migrations
```

禁止 blind `baselineOnMigrate(true)`。

`pingduoduo`、`yuansheng`，以及任何 #20 合并前创建且 non-empty/no-history 的测试 tenant（可能包括 `dingding`）都属于 #21 的 candidate set。

---

## 9. CI / Validation

Backend PR Validation：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

Stable PostgreSQL tests：

- `MigrationRuntimeIntegrationTest`；
- `CoreTenantBaselineIntegrationTest`；
- `TenantSchemaInventorySqlIntegrationTest`；
- `TenantSchemaProvisionerIntegrationTest`。

Lifecycle/availability specs：

- `CompanyServiceProvisioningSpec`；
- `AuthenticationServiceCompanyAvailabilitySpec`。

---

## 10. Current Risks

1. #6 未完成，master 尚未强制保护；
2. #9 未完成，Electron/Web 缺独立 build gate；
3. #21 未完成前，legacy tenants 不能安全统一纳入 Flyway history；
4. #22 Maven duplicate dependency warnings 尚未清理；
5. `ddl-auto=update` 尚未进入 staged `validate`；
6. MigrationAdminAuthorizer 仍是 configured-admin bridge；
7. Workbench protocol 仍需 client-first rollout；
8. attachment/resource authorization 仍需后续加强。

---

## 11. Roadmap

### P0 Governance

#6 / #7 / #9 / #22。

### P1 Tenant / Workbench Foundation

```text
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline/Validate ← CURRENT NEXT
#12 Tenant Migration Epic complete
#13 Workbench Platform Foundation
Overview / Task Backend
```

### P1 Workbench Business

Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 OA Expansion

Calendar → Announcement → Android OA → Report / AI Office。

---

## 12. Change Log

### 2026-08-20 — Issue #20 / PR #35 — New Tenant Provisioning

状态：`IMPLEMENTED`（以本 PR 合并为状态边界）。

- 新 tenant 主路径切为 migration-backed provisioning；
- company 先 inactive reservation，成功后再 active；
- migration failure 保持 inactive 并允许显式 retry；
- login / company switch 强制 active availability gate；
- CompanyCreatedEvent 移除 DDL；
- legacy non-empty/no-history tenant 明确转 #21；
- PostgreSQL provisioning integration test、activation-boundary spec、availability spec 全绿。

### 2026-08-19 — Issue #25 / PR #33 — Core Tenant Baseline

状态：`IMPLEMENTED`

- canonical baseline `2026081906`；
- PostgreSQL 16 empty-schema contract 全绿。

### 2026-08-19 — #32 / #26 / #19 / #18

- #32 Trusted Snapshot Review：COMPLETED；
- #26 / PR #27 Snapshot Tooling：IMPLEMENTED；
- #19 / PR #24 Migration Runtime：IMPLEMENTED；
- #18 / PR #23 ADR-0004：IMPLEMENTED。

---

项目原则：

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
