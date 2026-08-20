# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> ADR：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12 — COMPLETED  
> Core baseline：#25 / PR #33 — `2026081906`  
> New tenant provisioning：#20 / PR #35 — IMPLEMENTED  
> Existing tenant baseline：#21 / PR #36 — IMPLEMENTED  
> Managed migration target：`2026082001`  
> 下一平台阶段：#13 Workbench Platform Foundation

本文是 Group-IM 当前租户数据库迁移设计入口。冲突时以 ADR-0004、本文件和 `doc/PROJECT_MASTER.md` 的 current facts 为准。

---

## 1. Current Facts

- PostgreSQL schema 是 tenant boundary；
- #12 Tenant Versioned Migration Epic 已完成；
- #19 / PR #24 已建立显式 Migration Runtime；
- #25 / PR #33 固定 core business baseline `2026081906`；
- #20 / PR #35 已把新公司主路径切为 migration-backed provisioning；
- #21 / PR #36 已建立 existing tenant semantic preflight + explicit baseline + audit；
- 当前 managed target 为 `2026082001`；
- `spring.flyway.enabled=false`，普通应用启动不自动 migrate 全 tenant；
- 默认 `spring.jpa.hibernate.ddl-auto=update`；
- opt-in `application-schema-validate.yml` 仅用于 staged validate rollout；
- legacy `create_or_sync_company_schema` / Safe Sync 是 deprecated transitional compatibility surface，不是 migration authority；
- future Workbench/OA tenant DDL 必须继续通过新的 immutable Flyway migrations 发布。

---

## 2. Runtime Contract

```text
Git immutable migrations
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
4. non-empty + no-history tenant 必须先通过 existing-tenant preflight/baseline；
5. 同 tenant 使用 advisory lock；
6. migration 发布后不可修改，只新增版本；
7. 禁止 blind `baselineOnMigrate(true)`；
8. public 当前动态结构不充当 expected baseline contract。

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
- tenant identity view 的 legacy hard-coded predicate 与 schema-relative predicate 只按实际隔离行为判断，不以原始 SQL 文本差异判 drift。

---

## 4. New Tenant Provisioning — #20 / PR #35

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

允许：missing / empty / Flyway-managed schema。  
拒绝：business tables + no history → existing-tenant baseline path。

Admin retry：

```http
POST /api/admin/tenant-provisioning/companies/{companyId}/retry
```

---

## 5. Existing Tenant Preflight — #21 / PR #36

对 tenant schema 本身只做 read-only catalog / projection 行为检查。

```text
active tenant
  ↓
semantic catalog fingerprint
  ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
```

Fingerprint categories：

- tables；
- columns：type / nullability / default / identity / generated；
- constraints：PK / UNIQUE / FK / CHECK；
- indexes；
- views：name + output column shape；
- identity sequences。

每个 category 生成 deterministic SHA-256。Canonical hashes 由 PostgreSQL 16 中只运行 immutable migrations 的 schema 固定，测试持续 pin。

### Schema-independent FK normalization

Constraint fingerprint：

```text
same tenant FK referenced schema -> <tenant>
public/global FK referenced schema -> public
```

因此 `dingding`、`pingduoduo`、`yuansheng` 等真实 schema 名都可以比较同一个 canonical contract，同时不会隐藏 public/global boundary 错误。

### Identity view validation

`company` / `company_user` / `users` 不比较原始 view SQL，而验证实际行为：

- `company` 只能暴露当前 company；
- `company_user` 只能暴露当前 company membership；
- `users` 只能暴露当前 company users。

### Classification

`BASELINE_READY`：core set、所有 pinned category hashes、identity projection behavior 都匹配。  
`DRIFTED`：核心对象存在，但 column/constraint/index/view/sequence contract 不一致。  
`CONFLICT`：缺失/额外 core table/view、schema 不存在等结构冲突。  
`ERROR`：catalog/preflight 执行失败。

Drift/Conflict 仅返回 repair plan；不自动 ALTER/DROP，不从 public 复制覆盖。

---

## 6. Explicit Existing-Tenant Baseline

Admin API：

```http
POST /api/admin/schema-migrations/baselines/preflight
GET  /api/admin/schema-migrations/baselines/states
POST /api/admin/schema-migrations/baselines/companies/{companyId}
```

Baseline 请求必须携带 preflight 返回的 `expectedFingerprint`。

```text
require public control-plane bootstrap
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
post-check core fingerprint
  ↓
audit success + tenant_schema_state=UP_TO_DATE
```

已有 Flyway history 的 tenant 禁止重复 baseline。

---

## 7. Public Control Plane — `V2026082001`

新增：

- `public.tenant_schema_preflight_state`；
- `public.tenant_schema_baseline_audit`。

部署包含 PR #36 的 master 后必须先执行：

```http
POST /api/admin/schema-migrations/bootstrap
```

让 public Flyway 达到 `2026082001`，之后 #21 admin API 才可用。

---

## 8. Post-baseline Verification Migration — `2026082001`

Tenant migration：

```text
V2026082001__record_core_baseline_contract.sql
```

只确保 `tenant_schema_metadata` 并写：

```text
migration_runtime = group-im
core_baseline_contract = 2026081906
```

它不修改 core business tables。

```text
core business baseline = 2026081906
managed current target = 2026082001
```

---

## 9. PostgreSQL Integration Evidence

`CoreTenantBaselineContractFingerprintTest`：

- 从 immutable migrations 构造 canonical schema；
- pin 六类 deterministic hashes；
- 验证 view behavior。

`ExistingTenantBaselineIntegrationTest`：

- 两个合规 legacy tenant 无 history → `BASELINE_READY`；
- 两者显式 baseline `1906` → migrate `2001` → validate；
- post-fingerprint 保持一致；
- state 标记 `UP_TO_DATE`；
- `messages.content` 改为 `varchar(255)` → `DRIFTED`，不建 history、不自动修；
- 缺失 core table → `CONFLICT`，不建 history、不自动补；
- success / failed attempts 均写 audit；
- 已有 history 拒绝重复 baseline。

同时回归：

- `MigrationRuntimeIntegrationTest`；
- `CoreTenantBaselineIntegrationTest`；
- `TenantSchemaInventorySqlIntegrationTest`；
- `TenantSchemaProvisionerIntegrationTest`。

PR #36 latest head 已通过 Repository Governance、Backend compile/tests、Build KMP APK。

---

## 10. Real Tenant Rollout — `dingding`

`钉钉` / schema `dingding` 是 PR #35 合并前创建的 tenant，不能直接当作 migration-backed new tenant。

正确顺序：

```text
SELECT public.company metadata
  ↓
inspect dingding.flyway_schema_history
  ↓
deploy latest master
  ↓
public bootstrap to 2026082001
  ↓
POST /baselines/preflight
  ↓
BASELINE_READY -> baseline with returned fingerprint
DRIFTED/CONFLICT -> review repair plan first
```

`pingduoduo` / `yuansheng` 同理。

---

## 11. Hibernate Validate Staged Rollout

默认：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Opt-in：

```text
application-schema-validate.yml
→ ddl-auto: validate
```

只有满足以下 gate 才启用：

1. 所有 active tenant 有最新 preflight；
2. 无 `DRIFTED / CONFLICT / ERROR`；
3. 所有 active tenant 都有 Flyway history 且 current >= `2026082001`；
4. new provisioning 已稳定；
5. CI 绿；
6. staging validate；
7. production canary；
8. production full rollout。

Rollback 只撤销 validate profile/property 回默认 `update`。禁止删除 Flyway history 或反向改 schema 来“回滚”。

---

## 12. Legacy Compatibility Surface

仍存在：

```text
POST /api/organization/company/sync-schema
GET  /api/organization/company/schema-sync/status
POST /api/organization/company/schema-sync/apply
```

这些是 `DEPRECATED / TRANSITIONAL COMPATIBILITY`，不是 migration authority。真实 active tenant coverage 完成后再单独删除 legacy write path。

---

## 13. Epic #12 Completion / Next

```text
#18 Architecture ✅ PR #23
#19 Runtime ✅ PR #24
#26 Snapshot Tooling ✅ PR #27
#32 Snapshot Review ✅
#25 Core Tenant Baseline 2026081906 ✅ PR #33
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline / Validate ✅ PR #36
        ↓
#12 Tenant Versioned Migration Epic ✅ COMPLETED
        ↓
#13 Workbench Platform Foundation ← NEXT
```

Migration foundation is ready for Workbench/OA tenant-table migrations. Real historical-tenant rollout remains an operational use of the completed mechanism, not an architectural blocker for #13.

---

## 14. Historical Inputs

以下仅为 Historical Design Input：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

它们不覆盖 ADR-0004 或本 canonical README。
