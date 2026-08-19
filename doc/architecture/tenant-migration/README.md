# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> 架构决策：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> 当前阶段：#19 Migration Runtime

本文是 Group-IM **当前租户数据库迁移设计入口**。如果历史文档、PR 评论或旧实现蓝图与本文或 ADR-0004 冲突，以 ADR-0004 和本文为准。

---

## 1. 当前代码事实

### Tenant boundary

Group-IM 使用 PostgreSQL schema 作为公司租户边界。

HTTP 请求通常通过当前登录用户/公司解析 schema，但数据库迁移属于后台/控制面能力，不依赖 HTTP `TenantContextFilter` 或 ThreadLocal 决定目标 schema。

### New company provisioning

当前 `CompanyService.save()` 仍然：

```text
save company
  -> SELECT public.create_or_sync_company_schema(schemaName, companyId)
```

数据库函数会创建 schema 并以 public 当前表结构作为模板建立/同步 tenant。

**状态：LEGACY / COMPATIBILITY，等待 #20 接管。**

### Safe Schema Sync

`SafeTenantSchemaSyncService` 当前可以：

- 检测结构差异；
- 返回 `SYNCED / OUTDATED / CONFLICT / ERROR`；
- 自动补 nullable 且无 default 的缺失字段。

它不会：

- 创建缺失整表；
- 修改冲突字段；
- 表达版本化数据迁移；
- 管理索引/复杂约束生命周期。

**状态：TRANSITIONAL SAFETY TOOL。**

### Hibernate

当前：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

**状态：TECHNICAL DEBT / NOT MIGRATION SOURCE OF TRUTH。**

在 #21 完成 tenant coverage 前不直接切生产 `validate`。

### Backend CI

PR #16 已建立 Java 21 Maven compile + test 门禁。#19 使用 Testcontainers PostgreSQL 执行真实 migration integration test。

---

## 2. Current Runtime Architecture

#19 第一版 Runtime：

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
3. Spring Boot Flyway auto migration 明确关闭：`spring.flyway.enabled=false`；
4. 不在普通应用启动时自动迁移全部 tenant；
5. 后台 migration 显式指定 schema；
6. 同 tenant 使用 advisory lock；
7. 失败 tenant 停止自身执行，不回滚其他已成功 tenant；
8. migration 文件发布后不可修改，变化使用新版本。

---

## 3. Migration Resource Layout

当前目录：

```text
server/src/main/resources/db/migration/
├── public/
│   └── V2026081901__create_schema_migration_control_plane.sql
└── tenant/
    └── V2026081901__create_tenant_schema_metadata.sql
```

### public scope

当前首个 public migration 建立：

```text
schema_migration_run
schema_migration_run_item
tenant_schema_state
```

public bootstrap 是**显式管理员操作**，不是应用启动副作用。

现有 public schema 本来就非空，因此首次 bootstrap 明确创建 `2026081900` baseline，再执行 `2026081901` control-plane migration。这个显式 public baseline 规则不能复制到 tenant。

### tenant scope

当前首个 tenant migration 只建立 migration 基础 metadata：

```text
tenant_schema_metadata
```

它用于证明/验证 runtime 能从空 tenant schema 建立版本化 history。**没有创建任何 Workbench/OA 业务表。**

后续 Workbench 表必须使用新的 migration version，不修改已经发布的 `V2026081901`。

---

## 4. Control Plane

### `schema_migration_run`

一次管理员请求：

```text
PLAN
APPLY
```

状态：

```text
QUEUED
RUNNING
SUCCEEDED
PARTIAL_FAILED
FAILED
```

记录 requested_by、开始/结束时间和 success/failed 汇总。

### `schema_migration_run_item`

每个 tenant 一个 item：

- company/schema；
- from_version；
- target_version；
- pending_count；
- status；
- duration；
- safe error。

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

## 5. Runtime Modes

### PLAN

只读：

- 检查 schema 是否存在；
- 检查是否有业务表 / Flyway history；
- 读取 Flyway info；
- 列出 pending migrations；
- 不执行 tenant DDL；
- 不创建 tenant `flyway_schema_history`。

如果 tenant **非空且没有 Flyway history**：

```text
blocked = true
state = DRIFTED
reason = requires explicit baseline/preflight (#21)
```

PLAN 本身仍然成功，因为它成功发现并报告了风险。

### APPLY

- 只允许真实 tenant，不允许 `public`；
- 获取 tenant advisory lock；
- 空 schema 或已有可信 Flyway history 才允许执行；
- 执行 pending migrations；
- 写入真实 from/target version；
- 失败 tenant 记录 FAILED。

非空/no-history tenant：**直接拒绝 APPLY**，不自动 baseline。

### RETRY

`POST /runs/{runId}/retry` 只允许重试失败的 APPLY run，只重新选择失败 companyId，不重跑已成功 tenant。

### BASELINE

由 #21 实现。

必须在结构 preflight 通过后，由明确授权管理员执行。禁止 blind `baselineOnMigrate(true)`。

---

## 6. Target Resolution

Migration Runtime 不使用客户端传入 schema name 作为目标来源。

目标来自 fully-qualified：

```sql
public.company
```

两种 scope：

```json
{ "mode": "PLAN", "companyIds": [1, 2], "allActive": false }
```

或显式：

```json
{ "mode": "PLAN", "companyIds": [], "allActive": true }
```

约束：

- `companyIds` 与 `allActive=true` 不能同时使用；
- 默认不允许空 scope；
- inactive / unknown company 被拒绝；
- `schema_name=public` 永远不进入 tenant scope。

---

## 7. Concurrency

`MigrationRunWorker` 使用 bounded `ThreadPoolTaskExecutor`。

默认：

```yaml
group.schema-migration.max-concurrency: 2
```

配置被限制在安全范围内，不允许无限并发。

同一 tenant 使用 session-level PostgreSQL advisory lock：

```text
pg_try_advisory_lock(hashtext(namespace), hashtext(schemaName))
```

如果已经有执行者持锁：

```text
MIGRATION_TENANT_LOCKED
HTTP 409 / item FAILED
```

不同 tenant 可以在低并发度下独立执行。

---

## 8. Public Bootstrap

显式 API：

```http
POST /api/admin/schema-migrations/bootstrap
```

行为：

```text
configured admin authorization
  ↓
check public.flyway_schema_history
  ↓
if absent: explicit baseline 2026081900
  ↓
run db/migration/public
  ↓
verify current public migration version
```

不会扫描或迁移任何 tenant。

在 bootstrap 完成前，创建 run / 查询 run / 查询 tenant state 都会返回 control-plane-not-bootstrapped 错误。

---

## 9. Admin API

当前 API：

```http
POST /api/admin/schema-migrations/bootstrap
POST /api/admin/schema-migrations/runs
GET  /api/admin/schema-migrations/runs/{runId}
POST /api/admin/schema-migrations/runs/{runId}/retry
GET  /api/admin/schema-migrations/tenants
```

### Authorization

仓库目前尚无真实 `SYSTEM_ADMIN` authority。

#19 新增 `MigrationAdminAuthorizer`，把当前 configured-admin 兼容逻辑集中到一个组件里，controller 不散落 username 比较。

这仍是**过渡授权桥接**。未来 RBAC 建立后只替换该 authorizer，不改变 migration controller/service。

---

## 10. New Tenant Lifecycle

由 #20 实现，目标：

```text
Company metadata created as unavailable
        ↓
CREATE SCHEMA
        ↓
Flyway tenant migrations from empty schema
        ↓
verify target version
        ↓
activate company
```

当前 `create_or_sync_company_schema` 在 #20 前继续作为兼容路径。

#19 **不会自动把现有 CompanyService 切到 Flyway**。

---

## 11. Existing Tenant Baseline

由 #21 实现。

```text
read-only structure inspection
        ↓
expected baseline fingerprint
        ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
        ↓
explicit authorized baseline
        ↓
normal Flyway migration
```

#19 的责任只是识别：

```text
non-empty + no flyway history = BLOCKED
```

它不会猜测这个 tenant 应该 baseline 到哪个版本。

---

## 12. Safe Sync Boundary

过渡期：

```text
Flyway Migration Runtime = authoritative versioned change
SafeTenantSchemaSyncService = diagnostic / conservative compatibility
```

#19 不删除、不调用它来伪造 Flyway history。

#21 完成后评估把 Safe Sync 收敛为只读 drift checker。

---

## 13. Hibernate Strategy

阶段门禁：

```text
#19 runtime green
  ↓
#20 new tenants on migrations
  ↓
#21 old tenants baselined / drift resolved
  ↓
all active tenants >= required version
  ↓
staging validate
  ↓
production validate
```

#19 中 `ddl-auto` 保持 `update`。

---

## 14. Testing Gate

`MigrationRuntimeIntegrationTest` 使用真实 PostgreSQL Testcontainers，不依赖 mock database。

当前验证：

1. public bootstrap 首次显式 baseline + migration；
2. public bootstrap 重复执行幂等；
3. PLAN 不创建 tenant history / metadata table；
4. allActive 排除 public company；
5. APPLY company A 不改变 company B；
6. company A 写入 Flyway history；
7. repeat APPLY 幂等；
8. APPLY 记录真实 from/target version；
9. legacy non-empty/no-history tenant 被拒绝；
10. legacy table 保持原样；
11. advisory lock 阻止同 tenant 第二执行者。

Backend PR Validation 是 merge gate：

```text
mvn -pl server -am -DskipTests compile
mvn -pl server -am test
```

---

## 15. Issue Roadmap

```text
#12 Tenant Migration Epic
│
├── #18 Architecture Contract / ADR-0004  ✅
├── #19 Migration Runtime                 🚧
├── #20 New Tenant Provisioning
└── #21 Existing Tenant Baseline / ddl-auto validate
```

依赖：

```text
#8 Backend CI ✅
       ↓
#18 ✅
       ↓
#19 🚧
      ├─→ #20
      └─→ #21
       ↓
#12 complete
       ↓
#13 Workbench Platform Foundation
```

---

## 16. Historical Documents

以下文档保留为问题背景和实现思路来源：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

状态：**HISTORICAL DESIGN INPUT**。

若内容冲突：

> ADR-0004 > 本文 > 已合并实现 > 历史文档。

每个 #19/#20/#21 PR 都必须同步更新本文和 `doc/PROJECT_MASTER.md`。
