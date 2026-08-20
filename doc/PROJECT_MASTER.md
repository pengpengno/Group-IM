# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已经实现什么、正在交付什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 当前变更：Issue #21 / PR #36 — Existing Tenant Baseline / Validate（实现完成，合并即成为 master 当前事实）
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
- 所有代码、数据库、配置和文档变更必须通过 PR；
- Bug 必须有 Issue；重要功能、架构、数据库变更原则上也必须先有 Issue；
- 每个 PR 必须更新本文件；
- 默认 Squash Merge。

已实现：

- PR #5：Issue-driven PR、PROJECT_MASTER、模板、Governance CI、ADR；
- PR #16：Backend PR Validation；
- PR #23 / #18：ADR-0004 Tenant Migration Architecture；
- PR #24 / #19：Tenant Migration Runtime；
- PR #27 / #26：Trusted Tenant Schema Snapshot Tooling；
- #32：Trusted Snapshot Review；
- PR #33 / #25：Core Tenant Baseline Migrations；
- PR #35 / #20：New Tenant Provisioning；
- PR #36 / #21：Existing Tenant Baseline / Validate（本 PR 合并为状态边界）。

待完成：

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
- #20 新公司主路径已切为 inactive reservation → CREATE SCHEMA → Flyway → verify → active；
- login/company switch 只接受 active company；
- legacy `public.create_or_sync_company_schema(...)` 不再是新公司 provisioning 主路径；
- #21 已建立既有 tenant 的只读 preflight、显式 baseline、audit、TOCTOU fingerprint guard；
- #21 新增第一个 baseline 后无破坏性 managed migration `2026082001`；
- **core baseline version 与 managed target 必须区分：`1906` 是已存在结构的 baseline，`2001` 是进入正常 Flyway lifecycle 后的当前 target。**

---

## 4. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| 登录/鉴权 | STABLE | JWT/Spring Security；inactive company 不可登录/切换 | RBAC 细化 |
| 多公司/多租户 | STABLE | Runtime + core baseline + new provisioning + existing baseline path | 真实 tenant rollout |
| 单聊/群聊 | STABLE | 核心 IM 主链路存在 | 搜索/治理/一致性 |
| 联系人/组织 | STABLE | 公司/部门/员工能力存在 | 权限治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级授权 |
| 会议 | IN_PROGRESS | Meeting 服务与多端入口存在 | 协作联动 |
| AI 助手 | IN_PROGRESS | AI/Bot 能力持续演进 | 工具治理 |
| 群自动化 | IN_PROGRESS | 规则/执行/管理存在 | 审批/审计 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；正式设计已合并 | #12 收口后 #13 |
| OA Task | PLANNED | 领域设计已形成 | Platform Foundation 后实现 |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后 |
| Tenant Migration Runtime | STABLE | #19 / PR #24 | 运营观测 |
| Core Tenant Baseline | STABLE | #25 / PR #33，baseline `2026081906` | immutable contract |
| New Tenant Provisioning | STABLE | #20 / PR #35 | 观察真实创建 |
| Existing Tenant Baseline | STABLE | #21 / PR #36，preflight + explicit baseline + audit | `dingding` 等真实 preflight |
| Backend PR CI | STABLE | Java 21 compile + tests | #6 required check |

---

## 5. Workbench / OA

正式设计：

- `doc/features/workbench/README.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- ADR-0002：Workbench modular monolith；
- ADR-0003：Task-first + lightweight Approval；
- ADR-0005：Workbench structured card/event protocol。

Tenant Migration Epic 依赖链：

```text
#18 Architecture ✅
#19 Runtime ✅ PR #24
#26 Snapshot Tooling ✅ PR #27
#32 Snapshot Review ✅
#25 Core Baseline ✅ PR #33
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline / Validate ✅ PR #36 merge boundary
        ↓
#12 Tenant Migration Epic 收口
        ↓
#13 Workbench Platform Foundation
        ↓
Overview / Task Backend
```

#14 protocol implementation (#28/#29/#30) 可并行，但 actual WORKBENCH emission 继续受 client-first rollout gate 约束。

---

## 6. Tenant Versioned Migration

Canonical docs：

- `doc/architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md`
- `doc/architecture/tenant-migration/README.md`

### Runtime rules

1. `<tenant>.flyway_schema_history` 是 tenant migration authority；
2. PLAN 只读；
3. normal APPLY 只允许 empty schema 或已有可信 Flyway history；
4. non-empty + no-history tenant 必须走 #21 preflight/baseline；
5. 同 tenant 使用 PostgreSQL advisory lock；
6. migration 发布后不可修改，只新增版本；
7. 禁止 blind `baselineOnMigrate(true)`；
8. public/current schema 不能动态充当 expected contract。

### Core baseline contract — `2026081906`

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
- global identity authority 位于 `public.company` / `public.company_user` / `public.users`；
- tenant-local FK schema 名在 fingerprint 中归一化为 `<tenant>`，因此不同真实 schema 名不会制造假 drift；
- 指向 `public` 的跨 schema FK 保留 `public`，不能被归一化掉。

### Managed target — `2026082001`

Tenant migration：

```text
V2026082001__record_core_baseline_contract.sql
```

它不修改 core business tables，只确保 `tenant_schema_metadata` 并记录：

```text
migration_runtime = group-im
core_baseline_contract = 2026081906
```

因此版本语义：

```text
core baseline = 2026081906
managed current target = 2026082001
```

---

## 7. #20 / PR #35 — New Tenant Provisioning

状态：`IMPLEMENTED`。

```text
validate metadata
  ↓
reserve public.company active=false (REQUIRES_NEW)
  ↓
CREATE SCHEMA if missing
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
- non-empty + no-history 拒绝 `MIGRATION_BASELINE_REQUIRED` 并转 #21；
- admin retry：`POST /api/admin/tenant-provisioning/companies/{companyId}/retry`；
- inactive tenant 不可登录/切换；
- CompanyCreatedEvent 不再承担 DDL。

---

## 8. #21 / PR #36 — Existing Tenant Baseline / Validate

状态：`IMPLEMENTED IN PR / MERGE BOUNDARY`。

### Read-only preflight

对 active tenant 检查：

- core tables；
- columns / type / nullability / default / identity / generated；
- PK / UNIQUE / FK / CHECK；
- indexes；
- view output shape；
- identity sequences；
- `company` / `company_user` / `users` 的实际 tenant isolation 行为。

每类结构形成 deterministic SHA-256；canonical hashes 由 PostgreSQL 16 中“只运行 immutable migrations”的 schema 固定。

分类：

- `BASELINE_READY`：与 pinned contract 一致；
- `DRIFTED`：核心对象存在，但 column/constraint/index/view/sequence contract 有偏差；
- `CONFLICT`：缺失/额外 core table/view、schema 不存在等结构冲突；
- `ERROR`：preflight/catalog 执行失败。

Drift/Conflict 只返回 repair plan，不自动 ALTER/DROP，不从 public 复制覆盖。

### Portable fingerprint

constraint fingerprint 中：

- tenant → same tenant FK 的 referenced schema 统一为 `<tenant>`；
- tenant → `public` FK 保留 `public`；
- 因此 `dingding`、`pingduoduo`、`yuansheng` 等不同 schema 名可与同一 canonical contract 比较。

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
require BASELINE_READY + no history
  ↓
require fingerprint unchanged
  ↓
explicit Flyway.baseline(2026081906)
  ↓
Flyway.migrate() -> 2026082001
  ↓
Flyway.validate()
  ↓
post-check same core fingerprint
  ↓
audit + tenant_schema_state=UP_TO_DATE
```

Admin API：

```http
POST /api/admin/schema-migrations/baselines/preflight
GET  /api/admin/schema-migrations/baselines/states
POST /api/admin/schema-migrations/baselines/companies/{companyId}
```

### Public control plane

Public `V2026082001` 创建：

- `tenant_schema_preflight_state`；
- `tenant_schema_baseline_audit`。

部署后必须先执行 public bootstrap，再调用 #21 admin API。

### PostgreSQL validation

`ExistingTenantBaselineIntegrationTest` 证明：

1. 两个结构合规、无 history 的 legacy tenant 都能被识别为 `BASELINE_READY`；
2. 两者显式 baseline 在 `2026081906` 后只继续到 `2026082001`；
3. `Flyway.validate()` 与 post-fingerprint 通过；
4. `tenant_schema_state` 更新到 `UP_TO_DATE / 2026082001`；
5. `messages.content` 人工漂移为 `varchar(255)` 的 tenant 被判 `DRIFTED`，不建 history、不自动修；
6. 缺失 core table 的 tenant 被判 `CONFLICT`，不建 history、不自动补；
7. success / failed baseline attempts 都写 audit；
8. 已有 history 的 tenant 禁止重复 baseline。

---

## 9. Real tenant rollout — `dingding`

`钉钉` / schema `dingding` 是在 PR #35 合并前注册的，因此不能直接当作 #20 migration-backed new tenant。

正确顺序：

```text
查 public.company 的 company_id / active
  ↓
检查 dingding.flyway_schema_history 是否存在
  ↓
部署 PR #36 后先 public bootstrap
  ↓
POST baseline preflight
  ↓
BASELINE_READY -> 使用返回 fingerprint 显式 baseline
DRIFTED / CONFLICT -> 先审 repair plan，禁止 blind baseline
```

`pingduoduo` / `yuansheng` 同样按此流程处理。

---

## 10. Hibernate validate staged rollout

默认继续：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

新增 opt-in：

```text
application-schema-validate.yml
→ ddl-auto=validate
```

启用 gate：

1. 所有 active tenant 都有最新 preflight；
2. 不存在 `DRIFTED / CONFLICT / ERROR`；
3. 所有 active tenant 都有 Flyway history 且 current version ≥ `2026082001`；
4. #20 new provisioning 已稳定；
5. Backend/Governance CI 通过；
6. staging validate 先通过；
7. production canary 再通过；
8. 最后全量。

Rollback：仅撤回 validate profile/property 回默认 `update`；不删除 Flyway history、不反向修改 tenant schema。

---

## 11. Legacy compatibility boundary

以下仍是 `DEPRECATED / TRANSITIONAL COMPATIBILITY`：

```text
POST /api/organization/company/sync-schema
GET  /api/organization/company/schema-sync/status
POST /api/organization/company/schema-sync/apply
```

它们不是 migration authority，也不参与 #21 expected contract。active tenant coverage 完成后再单独删除 legacy write path。

---

## 12. CI / Validation

Backend PR Validation：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

PostgreSQL tests：

- `MigrationRuntimeIntegrationTest`；
- `CoreTenantBaselineIntegrationTest`；
- `TenantSchemaInventorySqlIntegrationTest`；
- `TenantSchemaProvisionerIntegrationTest`；
- `CoreTenantBaselineContractFingerprintTest`；
- `ExistingTenantBaselineIntegrationTest`。

PR #36 最终实现已通过 Backend compile + tests；Repository Governance 通过。合并前仍以最新 head 的完整 CI 状态为准。

---

## 13. 当前风险

1. #6 未完成，master 尚未强制保护；
2. #9 未完成，Electron/Web 缺独立 build gate；
3. 真实 active legacy tenants 尚未全部执行 #21 preflight/baseline；
4. 默认 `ddl-auto=update` 尚未切到 validate；
5. #22 Maven duplicate dependency warnings 尚未清理；
6. MigrationAdminAuthorizer 仍是 configured-admin bridge；
7. legacy schema sync write path 仍存在；
8. Workbench protocol 仍需 client-first rollout；
9. attachment/resource authorization 仍需后续加强。

---

## 14. Roadmap

### P0 Governance

#6 / #7 / #9 / #22。

### P1 Tenant / Workbench Foundation

```text
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline / Validate ✅ PR #36 merge boundary
#12 Tenant Migration Epic 收口
#13 Workbench Platform Foundation
Overview / Task Backend
```

### P1 Workbench Business

Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 OA Expansion

Calendar → Announcement → Android OA → Report / AI Office。

---

## 15. Change Log

### 2026-08-20 — Issue #21 / PR #36 — Existing Tenant Baseline / Validate

状态：`IMPLEMENTED IN PR / MERGE BOUNDARY`

- semantic catalog fingerprint / preflight classification；
- schema-independent tenant-local FK normalization；
- explicit baseline + expectedFingerprint TOCTOU guard；
- public preflight/audit control plane；
- baseline `2026081906` / managed target `2026082001` 分离；
- drift/conflict non-destructive rejection；
- PostgreSQL two-ready + drift + conflict integration coverage；
- opt-in schema-validate profile，默认仍为 update；
- `dingding` 等真实 tenant 下一步必须先 preflight。

### 2026-08-20 — Issue #20 / PR #35 — New Tenant Provisioning

状态：`IMPLEMENTED`。

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
