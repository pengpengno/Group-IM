# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> ADR：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> Core baseline：#25 / PR #33 — `2026081906`  
> New tenant provisioning：#20 / PR #35 — IMPLEMENTED  
> 当前阶段：#21 Existing Tenant Baseline / Validate — IN_PROGRESS  
> Managed migration target：`2026082001`

本文是 Group-IM 当前租户数据库迁移设计入口。冲突时以 ADR-0004、本文件和 `doc/PROJECT_MASTER.md` 的 current facts 为准。

---

## 1. Current Facts

- PostgreSQL schema 是 tenant boundary；
- #19 / PR #24 已建立显式 Migration Runtime；
- #25 / PR #33 已固定 core business baseline `2026081906`；
- #20 / PR #35 已把新公司主路径切为 migration-backed provisioning；
- `spring.flyway.enabled=false`，普通应用启动不自动 migrate 全 tenant；
- 默认 `spring.jpa.hibernate.ddl-auto=update`；
- legacy `create_or_sync_company_schema` / Safe Sync 仍是 transitional compatibility surface；
- non-empty + no-history tenant 必须走 #21；
- #21 引入 `2026082001` 作为 baseline 后第一个无破坏性 managed migration，**不改变 core baseline version**。

---

## 2. Runtime Contract

```text
Git versioned migrations
        ↓
TenantFlywayFactory(defaultSchema = tenant)
        ↓
TenantMigrationExecutor
        ↓
PostgreSQL advisory lock
        ↓
PLAN / APPLY / RETRY
```

规则：

1. `<tenant>.flyway_schema_history` 是 tenant version authority；
2. PLAN 只读；
3. normal APPLY 只允许 empty schema 或已有可信 history；
4. non-empty + no-history tenant 必须先通过 #21 preflight/baseline；
5. 同 tenant 使用 advisory lock；
6. migration 发布后不可修改，只新增版本；
7. 禁止 blind `baselineOnMigrate(true)`。

---

## 3. Canonical Core Baseline — `2026081906`

```text
V2026081901__create_tenant_schema_metadata.sql
V2026081902__create_core_automation_and_collaboration_tables.sql
V2026081903__create_core_file_and_meeting_tables.sql
V2026081904__create_core_message_and_user_tables.sql
V2026081905__add_core_tenant_keys_and_relationships.sql
V2026081906__create_tenant_identity_views.sql
```

Reviewed contract：

```text
18 core tenant tables
3 tenant identity views
17 identity sequences
65 core constraints
26 PK/UNIQUE backing indexes
```

Normalization：

- `messages.content = TEXT`；
- `meetings.scheduled_at = timestamp(6) without time zone`；
- `messages.type` CHECK 包含 `BOT_CARD`；
- global identity authority 位于 `public.company` / `public.company_user` / `public.users`；
- legacy tenant view predicate 可以是等价实现，不能只按 SQL 文本差异判 drift。

---

## 4. New Tenant Provisioning — #20

状态：`IMPLEMENTED / PR #35`。

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

允许：missing / empty / Flyway-managed schema。  
拒绝：business tables + no history → `MIGRATION_BASELINE_REQUIRED` → #21。

Admin retry：

```http
POST /api/admin/tenant-provisioning/companies/{companyId}/retry
```

---

## 5. Existing Tenant Preflight — #21

#21 对 legacy tenant 的 tenant schema 本身只做只读 catalog / projection 行为检查。

```text
active tenant
  ↓
semantic catalog fingerprint
  ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
```

### Fingerprint categories

- tables；
- columns：type / nullability / default / identity / generated；
- constraints：PK / UNIQUE / FK / CHECK；
- indexes；
- views：name + output column shape；
- identity sequences。

每个 category 生成 deterministic SHA-256，canonical hash 从只运行 immutable migrations 的 PostgreSQL Testcontainers schema 固定下来。

### Identity view validation

`company` / `company_user` / `users` 不比较原始 view SQL 文本，而验证实际行为：

- `company` 只能暴露当前 `company_id/schema_name`；
- `company_user` 只能暴露当前 company membership；
- `users` 只能暴露当前 company users。

这是为了允许 legacy hard-coded companyId view 与当前 schema-relative view 在语义等价时通过，同时阻止跨 tenant 泄漏。

### Classification

`BASELINE_READY`：core table/view set 与所有 pinned category hashes 一致，identity projection 行为正确。  
`DRIFTED`：核心对象存在，但 column/constraint/index/view/sequence contract 不一致。  
`CONFLICT`：缺失/额外 core table/view、schema 不存在等结构冲突。  
`ERROR`：catalog/preflight 执行失败。

Drift/Conflict 仅给 repair plan；不自动 `ALTER/DROP`，不从 public 复制覆盖。

---

## 6. Explicit Baseline

Admin API（#21）：

```http
POST /api/admin/schema-migrations/baselines/preflight
GET  /api/admin/schema-migrations/baselines/states
POST /api/admin/schema-migrations/baselines/companies/{companyId}
```

baseline 请求必须携带最近 preflight 返回的 `expectedFingerprint`。

执行顺序：

```text
require control-plane bootstrap
  ↓
resolve active company
  ↓
advisory lock
  ↓
re-run preflight
  ↓
require BASELINE_READY + no Flyway history
  ↓
require observedFingerprint == expectedFingerprint
  ↓
explicit Flyway.baseline(version=2026081906)
  ↓
Flyway.migrate()
  ↓
V2026082001 + Flyway.validate()
  ↓
post-check same core fingerprint
  ↓
audit success + tenant_schema_state=UP_TO_DATE
```

任何一步失败都记录 audit；不会用 `baselineOnMigrate(true)` 吞掉未知结构。

---

## 7. Public Control Plane — `V2026082001`

新增：

- `public.tenant_schema_preflight_state`；
- `public.tenant_schema_baseline_audit`。

Preflight state 记录 classification / baseline version / history flag / fingerprint / category hashes / repair plan / operator / checked time。

Audit 记录 baseline operator / version / expected+observed fingerprint / ATTEMPTED-SUCCEEDED-FAILED / safe error / timestamps。

部署 #21 后必须先执行：

```http
POST /api/admin/schema-migrations/bootstrap
```

让 public Flyway 从 `2026081901` 升到 `2026082001`，之后 baseline/preflight admin API 才可用。

---

## 8. Post-baseline Verification Migration — `V2026082001`

Tenant migration：

```text
V2026082001__record_core_baseline_contract.sql
```

只确保 `tenant_schema_metadata` 存在，并写入：

```text
migration_runtime = group-im
core_baseline_contract = 2026081906
```

它不修改 core business tables。用途是证明一个显式 baseline 的 legacy tenant 能继续执行普通版本化 migration。

因此版本含义明确分离：

```text
core baseline version    = 2026081906
managed current target   = 2026082001
```

---

## 9. Hibernate Validate Staged Rollout

默认仍是：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

#21 新增 opt-in profile：

```text
application-schema-validate.yml
```

使用 `ddl-auto=validate`，但只有满足以下 gate 才能启用：

1. 所有 active tenant 有最新 preflight；
2. 无 `DRIFTED / CONFLICT / ERROR`；
3. 所有 active tenant 都有 Flyway history 且 current version ≥ `2026082001`；
4. #20 new provisioning 已稳定；
5. Backend/Governance CI 绿；
6. staging validate 先通过；
7. 再 production canary；
8. 最后全量。

Rollback 只撤销 validate profile/property 回默认 `update`。禁止删除 Flyway history 或反向修改 tenant schema 来“回滚”。

---

## 10. Legacy Compatibility Surface

当前仍存在：

```text
POST /api/organization/company/sync-schema
GET  /api/organization/company/schema-sync/status
POST /api/organization/company/schema-sync/apply
```

这些接口从 #21 起定义为 `DEPRECATED / TRANSITIONAL COMPATIBILITY`，不是 migration authority。等 active tenant coverage 完成后，再单独删除 legacy write path。

---

## 11. Test Tenants

`pingduoduo`、`yuansheng`、以及在 #20 合并前创建的 `dingding` 都只能作为 #21 candidate。

对 `dingding` 的正确顺序：

```text
inspect company metadata
  ↓
check history/table state
  ↓
run #21 preflight
  ↓
if BASELINE_READY -> explicit baseline with fingerprint
if DRIFTED/CONFLICT -> repair review first
```

不允许因为它是测试数据就跳过结构证明直接写 Flyway history。

---

## 12. Validation Gates

已稳定：

- `MigrationRuntimeIntegrationTest`；
- `CoreTenantBaselineIntegrationTest`；
- `TenantSchemaInventorySqlIntegrationTest`；
- `TenantSchemaProvisionerIntegrationTest`；
- `CompanyServiceProvisioningSpec`；
- `AuthenticationServiceCompanyAvailabilitySpec`。

#21 新增：

- canonical baseline category-hash pin test；
- 至少两个 baseline-ready legacy tenants 的 baseline + post-migration validation；
- drift/conflict rejection；
- baseline audit / state assertions。

---

## 13. Roadmap

```text
#18 Architecture ✅ PR #23
#19 Runtime ✅ PR #24
#26 Snapshot Tooling ✅ PR #27
#32 Snapshot Review ✅
#25 Core Tenant Baseline 2026081906 ✅ PR #33
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline / Validate ← IN PROGRESS
```

#21 完成后才能评估全面退役 legacy public clone / Safe Sync write path，并进入 #12 Epic 收口。

---

## 14. Historical Inputs

以下文件继续保留为 Historical Design Input：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

它们不覆盖 ADR-0004 或本 canonical README。
