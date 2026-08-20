# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> ADR：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> Core baseline：#25 / PR #33 — IMPLEMENTED  
> Canonical core baseline version：`2026081906`  
> New tenant provisioning：#20 / PR #35 — IMPLEMENTED（以本 PR 合并为状态边界）  
> 当前下一阶段：#21 Existing Tenant Baseline / Validate

本文是 Group-IM 当前租户数据库迁移设计入口。冲突时以 ADR-0004、本文件和 `doc/PROJECT_MASTER.md` 的 current facts 为准。

---

## 1. Current Facts

- PostgreSQL schema 是 tenant boundary；
- #19 / PR #24 已建立显式 Migration Runtime；
- #25 / PR #33 已证明空 tenant 可只通过 Flyway 构建完整 core schema；
- canonical tenant version 为 `2026081906`；
- #20 / PR #35 已把新公司主路径切换为 migration-backed provisioning；
- 新 tenant 先以 `active=false` reservation 写入 public.company，Flyway 成功后才激活；
- password login / company switch 只接受 active company；
- `CompanyCreatedEvent` 不再承担 tenant DDL；
- `spring.flyway.enabled=false`，普通应用启动不 migrate 全部 tenant；
- `spring.jpa.hibernate.ddl-auto` 仍为 `update`；
- `SafeTenantSchemaSyncService` 仍是 transitional drift/compatibility tool；
- legacy `public.create_or_sync_company_schema(...)` 仅保留给显式 compatibility sync，不再用于新公司主路径；
- #21 负责 non-empty + no-Flyway-history 的既有 tenant。

---

## 2. Migration Runtime Contract

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
3. APPLY 只允许空 schema 或已有可信 Flyway history 的 schema；
4. non-empty + no-history tenant 必须走 #21；
5. 同 tenant 并发 APPLY 使用 advisory lock；
6. migration 发布后不可修改，只能新增版本。

---

## 3. Canonical Tenant Baseline

```text
V2026081901__create_tenant_schema_metadata.sql
V2026081902__create_core_automation_and_collaboration_tables.sql
V2026081903__create_core_file_and_meeting_tables.sql
V2026081904__create_core_message_and_user_tables.sql
V2026081905__add_core_tenant_keys_and_relationships.sql
V2026081906__create_tenant_identity_views.sql
```

Reviewed core contract：

```text
18 core tenant tables
3 tenant identity views
17 identity sequences
65 core constraints
26 PK/UNIQUE backing indexes
```

Canonical normalization：

- `messages.content = TEXT`；
- `meetings.scheduled_at = timestamp(6) without time zone`；
- `messages.type` CHECK 包含 `BOT_CARD`；
- tenant identity views 不写死 captured companyId / source schema；
- global identity authority 继续位于 `public.company` / `public.company_user` / `public.users`。

---

## 4. #20 New Tenant Provisioning

状态：`IMPLEMENTED / PR #35`（以本 PR 合并为状态边界）。

新 tenant 生命周期固定为：

```text
validate metadata / uniqueness
        ↓
reserve public.company row as active=false
        ↓   REQUIRES_NEW commit
create tenant schema if missing
        ↓
Flyway APPLY all tenant migrations
        ↓
verify pendingCount = 0 and currentVersion exists
        ↓
mark public.company active=true
        ↓   REQUIRES_NEW commit
publish CompanyCreatedEvent
```

关键原则：

- company metadata reservation 与 tenant DDL/migration 不共享一个长事务；
- provisioning 未完成时 company 必须保持 `active=false`；
- password login 和 company switch 只解析 active company；
- 即使 admin 也不能切换到 inactive/unprovisioned company；
- `CompanyCreatedEvent` 只在 provisioning 成功后发布，不承担 DDL；
- public/default company 不走 tenant provisioning；
- 新 tenant 主路径不调用 `public.create_or_sync_company_schema(...)`。

### Retry semantics

`TenantSchemaProvisioner` 只接受四种明确状态：

```text
schema missing
  -> CREATE SCHEMA -> migrate

schema exists + empty
  -> migrate

schema exists + Flyway history
  -> resume/apply pending migrations

schema exists + business tables + no Flyway history
  -> reject MIGRATION_BASELINE_REQUIRED -> #21
```

管理员可显式重试 inactive company：

```http
POST /api/admin/tenant-provisioning/companies/{companyId}/retry
```

Active company 会被拒绝，避免 legacy cloned tenant 静默进入 new-tenant path。

---

## 5. Transaction Boundary

#20 不把以下步骤放在同一个数据库事务里：

```text
insert company row
CREATE SCHEMA
Flyway migrate
activate company
```

采用：

1. `CompanyProvisioningTransactionService.reserveInactive(...)`：`REQUIRES_NEW`；
2. `TenantSchemaProvisioner.provision(...)`：schema creation + Flyway；
3. `markActive(...)`：`REQUIRES_NEW`；
4. failure path：`markInactive(...)`，保留 metadata 供显式 retry。

这允许 Flyway partial-history 的合法重试，同时保证失败 company 不进入可用状态。

---

## 6. Legacy Compatibility Boundary

以下旧能力暂时保留：

```text
POST /api/organization/company/sync-schema
  -> CompanyService.syncSchemas(...)
  -> public.create_or_sync_company_schema(...)
```

它是显式 legacy compatibility operation，不是新公司创建主路径。

退役条件：

- #20 新 tenant provisioning 稳定；
- #21 所有 active legacy tenants baseline/validate 完成；
- staging / production tenant coverage 达标。

---

## 7. #21 Existing Tenant Baseline / Validate

#21 使用 `2026081906` contract 对既有 tenant 做只读 preflight：

```text
inspect existing tenant
        ↓
compare with canonical core contract
        ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
        ↓
explicit authorized baseline
        ↓
normal Flyway migrations
```

禁止 blind `baselineOnMigrate(true)`。

在 #20 合并前创建的 tenant（例如测试环境的 `dingding`）不能自动认定为“新 tenant”。如果它包含业务表但没有 `flyway_schema_history`，必须走 #21。

---

## 8. Validation Gates

### Core baseline gate

`CoreTenantBaselineIntegrationTest` 已在 PostgreSQL 16 验证：empty schema -> `2026081906`，并核对 18 tables / 3 views / 17 sequences / 65 constraints / 26 indexes、normalized columns/checks 与 tenant identity isolation。

### New provisioning gate

PR #35 新增并通过：

- `TenantSchemaProvisionerIntegrationTest`：missing schema 自动创建、完整 migrate、retry 幂等、legacy no-history 拒绝；
- `CompanyServiceProvisioningSpec`：成功后才 active，失败保持 inactive；
- `AuthenticationServiceCompanyAvailabilitySpec`：admin 无法切入 inactive/unprovisioned company；
- Backend compile/test、Repository Governance、Build KMP APK。

---

## 9. Safe Sync / Hibernate Rollout

```text
#19 runtime ✅
  ↓
#26 snapshot tooling ✅
  ↓
#32 reviewed evidence ✅
  ↓
#25 core baseline 2026081906 ✅
  ↓
#20 new tenant migration provisioning ✅ PR #35
  ↓
#21 existing tenant baseline/validate ← NEXT
  ↓
all active tenants covered
  ↓
staging ddl-auto=validate
  ↓
production ddl-auto=validate
```

当前仍保持 `ddl-auto=update`。

---

## 10. Roadmap

```text
#18 Architecture ✅ PR #23
#19 Runtime ✅ PR #24
#26 Snapshot Tooling ✅ PR #27
#32 Snapshot Review ✅
#25 Core Tenant Baseline ✅ PR #33
#20 New Tenant Provisioning ✅ PR #35
#21 Existing Tenant Baseline/Validate ← NEXT
```

#21 完成后才能考虑全面退役 legacy public clone 与 Safe Sync write path。

---

## 11. Historical Inputs

以下文件继续保留为 Historical Design Input：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

它们不覆盖 ADR-0004 或本 canonical README。
