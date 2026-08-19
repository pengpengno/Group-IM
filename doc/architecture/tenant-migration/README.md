# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> 架构决策：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> 当前阶段：#19 Migration Runtime — IMPLEMENTED / PR #24  
> 下一阶段：#26 Trusted Schema Snapshot → #25 Core Tenant Baseline

本文是 Group-IM **当前租户数据库迁移设计入口**。如果历史文档、PR 评论或旧实现蓝图与本文或 ADR-0004 冲突，以 ADR-0004 和本文为准。

---

## 1. 当前代码事实

### Tenant boundary

Group-IM 使用 PostgreSQL schema 作为公司租户边界。HTTP 请求通常通过当前登录用户/公司解析 schema，但数据库迁移属于后台/控制面能力，不依赖 HTTP `TenantContextFilter` 或 ThreadLocal 决定目标 schema。

### New company provisioning

当前 `CompanyService.save()` 仍然：

```text
save company
  -> SELECT public.create_or_sync_company_schema(schemaName, companyId)
```

legacy database function 会创建 schema，并从 public 复制/建立：

- all non-system tenant tables；
- company/company_user/users tenant views；
- foreign keys；
- ordinary indexes；
- CHECK constraints。

**状态：LEGACY / COMPATIBILITY。**

#19 的首个 tenant migration 只建立 migration metadata，不能替代完整 Group-IM tenant core schema。

### Safe Schema Sync

`SafeTenantSchemaSyncService` 当前可以检测结构差异，并自动补 nullable 且无 default 的缺失字段；它不会创建缺失整表、修改冲突字段、表达版本化数据 migration 或完整管理 index/constraint lifecycle。

**状态：TRANSITIONAL SAFETY TOOL。**

### Hibernate

```yaml
spring.jpa.hibernate.ddl-auto: update
```

**状态：TECHNICAL DEBT / NOT MIGRATION SOURCE OF TRUTH。**

#21 完成 tenant coverage 前不直接切生产 `validate`。

---

## 2. Implemented Runtime Architecture

PR #24 / #19 已建立：

```text
Git versioned migrations
        ↓
 Public Flyway scope          Tenant Flyway scope
        ↓                           ↓
public.flyway_schema_history   tenant_a.flyway_schema_history
        ↓                      tenant_b.flyway_schema_history
control plane                        ...
        ↓
MigrationRunService
        ↓
MigrationRunWorker (bounded)
        ↓
TenantMigrationExecutor
        ↓
Postgres advisory lock
        ↓
PLAN / APPLY / RETRY
```

核心原则：

1. Flyway history 是每个 schema 的版本事实；
2. public control tables 是运行/状态/审计投影；
3. `spring.flyway.enabled=false`，不让 Spring Boot 启动自动迁移 public；
4. 不在普通应用启动时自动迁移全部 tenant；
5. 后台 migration 显式指定 schema；
6. 同 tenant 使用 advisory lock；
7. 一个 tenant 失败不回滚其他已经成功 tenant；
8. migration 文件发布后不可修改，变化使用新版本。

---

## 3. Migration Resources

当前：

```text
server/src/main/resources/db/migration/
├── public/
│   └── V2026081901__create_schema_migration_control_plane.sql
└── tenant/
    └── V2026081901__create_tenant_schema_metadata.sql
```

### public scope

`V2026081901` 建立：

```text
schema_migration_run
schema_migration_run_item
tenant_schema_state
```

public 本身是历史非空 schema，因此首次**显式** bootstrap 使用 public-only baseline `2026081900`，随后执行 control-plane migration。这个规则不能复制到 tenant。

### tenant scope

`V2026081901` 只建立：

```text
tenant_schema_metadata
```

它用于验证 Migration Runtime 能从空 tenant schema 建立 Flyway history 和执行 versioned DDL。

**它不是 Group-IM 核心 tenant baseline。** Chat、Meeting、File、Automation 等现有业务结构尚未被 versioned baseline 覆盖。

---

## 4. Control Plane

### `schema_migration_run`

一次管理员 PLAN/APPLY 请求，状态：

```text
QUEUED
RUNNING
SUCCEEDED
PARTIAL_FAILED
FAILED
```

### `schema_migration_run_item`

每 tenant 记录：company/schema、from/target version、pending count、status、duration、safe error。

### `tenant_schema_state`

快速投影：

```text
UNKNOWN
UP_TO_DATE
PENDING
MIGRATING
FAILED
DRIFTED
DISABLED
```

它不是 migration history 的唯一来源。

---

## 5. PLAN / APPLY / RETRY

### PLAN

只读：

- 检查 schema；
- 检查 business tables / Flyway history；
- 读取 Flyway info；
- 列出 pending migrations；
- **不创建 tenant Flyway history**；
- **不执行 tenant DDL**。

非空且没有 Flyway history 的 tenant 被报告：

```text
blocked = true
state = DRIFTED
requires explicit baseline/preflight (#21)
```

### APPLY

- 不允许 public；
- 获取 tenant advisory lock；
- 空 schema 或已有可信 Flyway history 才执行；
- 保存真实 pre-apply `from_version` 与最终 `target_version`；
- 非空/no-history tenant 直接拒绝，不自动 baseline。

### RETRY

只重试失败的 APPLY tenant，不重跑成功项。

### BASELINE

由 #21 实现，并以 #25 固定的 core tenant baseline contract 为 expected structure。禁止 blind `baselineOnMigrate(true)`。

---

## 6. Target Resolution

目标来自 fully-qualified：

```sql
public.company
```

支持：

```json
{ "mode": "PLAN", "companyIds": [1, 2], "allActive": false }
```

或：

```json
{ "mode": "PLAN", "companyIds": [], "allActive": true }
```

规则：

- explicit IDs 与 allActive 不能同时使用；
- 空 scope 拒绝；
- inactive/unknown company 拒绝；
- `schema_name=public` 永远不作为 tenant target。

---

## 7. Concurrency

`MigrationRunWorker` 使用 bounded executor，默认：

```yaml
group.schema-migration.max-concurrency: 2
```

配置限制为 1–8。

同 tenant 使用 session-level PostgreSQL advisory lock：

```text
pg_try_advisory_lock(hashtext(namespace), hashtext(schemaName))
```

第二执行者收到 `MIGRATION_TENANT_LOCKED`。

---

## 8. Public Bootstrap

显式管理员 API：

```http
POST /api/admin/schema-migrations/bootstrap
```

```text
check public Flyway history
  ↓
if absent: explicit public baseline 2026081900
  ↓
run db/migration/public
  ↓
verify current version
```

不会扫描/迁移 tenant。

---

## 9. Admin API

```http
POST /api/admin/schema-migrations/bootstrap
POST /api/admin/schema-migrations/runs
GET  /api/admin/schema-migrations/runs/{runId}
POST /api/admin/schema-migrations/runs/{runId}/retry
GET  /api/admin/schema-migrations/tenants
```

仓库暂时没有真实 SYSTEM_ADMIN authority。#19 使用 `MigrationAdminAuthorizer` 集中 configured-admin 兼容逻辑；未来 RBAC 只替换该 authorizer，不改 migration service contract。

---

## 10. Trusted Snapshot — #26

#25 不能只根据 JPA Entity 手写 baseline，因为 legacy provisioning 的数据库事实还包含：

- view definitions；
- DB-level defaults / identity；
- PK/UK/FK/CHECK；
- indexes；
- sequences；
- 可能没有完整反映在 annotations 中的结构细节。

因此下一阶段 #26：

> 对一个管理员确认健康的 tenant 做**离线、只读 schema snapshot/inventory**。

目标输出：

1. machine-readable inventory：tables/views/columns/types/nullability/defaults/constraints/indexes/sequences/view definitions；
2. schema-only SQL reference；
3. source schema、timestamp、fingerprint；
4. 不包含运行数据和 credentials。

推荐评估：

- PostgreSQL catalog deterministic inventory；
- `pg_dump --schema-only --schema=<trusted_tenant>`；
- 两者结合交叉核对。

生成物是**审阅输入**，不能未经 normalization/review 自动变成 executable migration。

---

## 11. Core Tenant Baseline — #25

依赖：#19 + #26。

目标：

> 从真正空 schema 开始，只运行 immutable versioned tenant migrations，就能得到当前 Group-IM 核心业务要求的完整 tenant schema。

必须覆盖/确认：

- tenant-managed tables；
- global/public-only objects；
- global identity views；
- PK/UK/FK/CHECK；
- indexes；
- sequences/identity；
- expected baseline version/fingerprint。

Testcontainers 必须验证：

```text
empty schema
  -> all baseline migrations
  -> compare structure with #26 inventory contract
```

不允许依赖 Hibernate `ddl-auto=update` 在测试后补齐剩余结构。

---

## 12. New Tenant Lifecycle — #20

依赖：#19 + #26 + #25。

```text
Company metadata created as unavailable
        ↓
CREATE SCHEMA
        ↓
run complete tenant migrations
        ↓
verify target version / core schema
        ↓
activate company
```

#25 完成前，legacy `create_or_sync_company_schema` 必须继续作为兼容路径。

---

## 13. Existing Tenant Baseline — #21

依赖：#19 + #26 + #25。

```text
read-only structure inspection
        ↓
compare with #25 baseline contract
        ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
        ↓
explicit authorized baseline
        ↓
normal Flyway migration
```

#19 只负责识别 `non-empty + no history = BLOCKED`，不会猜测 baseline version。

---

## 14. Safe Sync and Hibernate

过渡期：

```text
Flyway Migration Runtime = authoritative versioned change
SafeTenantSchemaSyncService = diagnostic / conservative compatibility
```

目标顺序：

```text
#19 runtime ✅
  ↓
#26 trusted snapshot
  ↓
#25 core baseline
  ↓
#20 new tenants on migrations
  +
#21 old tenants baselined / drift resolved
  ↓
all active tenants >= required version
  ↓
staging validate
  ↓
production validate
```

`ddl-auto` 当前保持 `update`。

---

## 15. Testing Gate

PR #24 合并前已通过：

- Repository Governance；
- Backend Maven compile；
- Backend Maven test；
- PostgreSQL Testcontainers；
- KMP APK。

`MigrationRuntimeIntegrationTest` 验证：

1. public bootstrap explicit baseline + migration；
2. bootstrap 幂等；
3. PLAN 不创建 tenant history；
4. PLAN 不执行 tenant DDL；
5. allActive 排除 public；
6. APPLY company A 不改变 company B；
7. history 写入；
8. repeat APPLY 幂等；
9. from/target version 正确；
10. legacy non-empty/no-history tenant 被拒绝；
11. legacy table 保持不变；
12. advisory lock 互斥。

---

## 16. Issue Roadmap

```text
#12 Tenant Migration Epic
│
├── #18 Architecture Contract / ADR-0004  ✅
├── #19 Migration Runtime                 ✅ PR #24
├── #26 Trusted Schema Snapshot           ⏭ NEXT
├── #25 Core Tenant Baseline              depends #26
├── #20 New Tenant Provisioning           depends #25
└── #21 Existing Tenant Baseline/Validate depends #25
```

```text
#8 Backend CI ✅
       ↓
#18 ✅
       ↓
#19 ✅
       ↓
#26
       ↓
#25
      ├─→ #20
      └─→ #21
       ↓
#12 complete
       ↓
#13 Workbench Platform Foundation
```

---

## 17. Historical Documents

以下文件保留为 Historical Design Input：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

冲突优先级：

> ADR-0004 > 本文 > 已合并实现 > 历史文档。

每个 #19/#26/#25/#20/#21 PR 都必须同步更新本文和 `doc/PROJECT_MASTER.md`。
