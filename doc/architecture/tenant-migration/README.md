# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> 架构决策：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> 当前阶段：#19 Migration Runtime — READY / CI GREEN  
> 下一阶段：#25 Core Tenant Baseline Migrations

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

`create_or_sync_company_schema(...)` 会：

- 创建 schema；
- 从 public 克隆全部非系统业务表；
- 创建 company/company_user/users tenant views；
- 同步 foreign keys；
- 同步 indexes；
- 同步 CHECK constraints。

**状态：LEGACY / COMPATIBILITY，等待 #25 + #20 接管。**

#19 的首个 tenant migration 只建立 migration metadata，不能替代上述完整 tenant core schema。

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

PR #16 已建立 Java 21 Maven compile + test 门禁。PR #24 已在该门禁下通过真实 PostgreSQL Testcontainers migration integration test。

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

当前首个 tenant migration 只建立：

```text
tenant_schema_metadata
```

它用于证明 runtime 能从空 tenant schema 建立 Flyway history 和执行 versioned DDL。**没有创建任何 Workbench/OA 表，也没有创建现有 Chat/Meeting/File 等核心 tenant 表。**

后续 migration 文件必须使用新版本，不修改已经发布的 `V2026081901`。

---

## 4. Control Plane

### `schema_migration_run`

一次管理员请求：`PLAN` 或 `APPLY`。

状态：

```text
QUEUED
RUNNING
SUCCEEDED
PARTIAL_FAILED
FAILED
```

### `schema_migration_run_item`

每个 tenant 一个 item，记录：

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

- 检查 schema；
- 检查业务表 / Flyway history；
- 读取 Flyway info；
- 列出 pending migrations；
- **不创建 tenant Flyway history**；
- **不执行 tenant DDL**。

如果 tenant 非空且没有 Flyway history：

```text
blocked = true
state = DRIFTED
reason = requires explicit baseline/preflight (#21)
```

PLAN 本身仍可成功，因为它正确发现并报告了风险。

### APPLY

- 只允许真实 tenant，不允许 `public`；
- 获取 tenant advisory lock；
- 空 schema 或已有可信 Flyway history 才允许执行；
- 执行 pending migrations；
- 记录真实 pre-apply `from_version` 与最终 `target_version`；
- 失败 tenant 记录 FAILED。

非空/no-history tenant：**直接拒绝 APPLY**，不自动 baseline。

### RETRY

`POST /runs/{runId}/retry` 只允许重试失败的 APPLY run，只重新选择失败 companyId，不重跑已成功 tenant。

### BASELINE

由 #21 实现。必须以 #25 固定的 core tenant baseline contract 为 expected structure，并在只读结构 preflight 通过后显式执行。

禁止 blind `baselineOnMigrate(true)`。

---

## 6. Target Resolution

Migration Runtime 不接受 schemaName 作为客户端迁移目标事实。

目标来自 fully-qualified：

```sql
public.company
```

请求 scope：

```json
{ "mode": "PLAN", "companyIds": [1, 2], "allActive": false }
```

或：

```json
{ "mode": "PLAN", "companyIds": [], "allActive": true }
```

约束：

- `companyIds` 与 `allActive=true` 不能同时使用；
- 两者都没有时拒绝；
- inactive / unknown company 被拒绝；
- `schema_name=public` 永远不进入 tenant migration scope。

---

## 7. Concurrency

`MigrationRunWorker` 使用 bounded `ThreadPoolTaskExecutor`。

默认：

```yaml
group.schema-migration.max-concurrency: 2
```

配置被限制在 1–8。

同 tenant 使用 session-level PostgreSQL advisory lock：

```text
pg_try_advisory_lock(hashtext(namespace), hashtext(schemaName))
```

第二执行者会得到：

```text
MIGRATION_TENANT_LOCKED
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

在 bootstrap 完成前，创建/查询 migration run 和 tenant state 会拒绝执行。

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

#19 使用 `MigrationAdminAuthorizer` 把 configured-admin 兼容逻辑集中到一个组件里，controller/service 不散落 username 判断。

这是过渡授权桥接；未来 RBAC 建立后替换 authorizer 即可。

---

## 10. Core Tenant Baseline — #25

### Why #25 is required

#19 已证明 migration runtime 可以：

- 对空 tenant 建立 Flyway history；
- 执行 versioned DDL；
- 管理 PLAN/APPLY/lock/state。

但它**没有证明一个全新空 schema 在 migrations 后已经具备完整 Group-IM 业务结构**。

当前 legacy provisioning 动态复制：

- all non-system tenant tables；
- global identity views；
- foreign keys；
- indexes；
- CHECK constraints。

因此 #20 不能直接接管新公司，必须先完成：

```text
#25 Core Tenant Baseline Migrations
```

#25 目标：

> 从真正空 schema 开始，只运行 versioned tenant migrations，就能得到现有 Group-IM 核心业务要求的完整 tenant schema。

如果仅凭仓库无法可靠还原生产 schema，#25 必须先增加只读 schema snapshot/export，由管理员从可信数据库产生待审阅基线输入。

---

## 11. New Tenant Lifecycle — #20

新依赖：

```text
#19 Runtime
  ↓
#25 Core Baseline
  ↓
#20 Provisioning
```

目标：

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

#25 完成前，当前 `create_or_sync_company_schema` 必须继续作为兼容路径，不能提前删除。

---

## 12. Existing Tenant Baseline — #21

依赖：#19 + #25。

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

#19 当前只负责识别：

```text
non-empty + no flyway history = BLOCKED
```

它不会猜测 tenant 应 baseline 到哪个版本。

---

## 13. Safe Sync Boundary

过渡期：

```text
Flyway Migration Runtime = authoritative versioned change
SafeTenantSchemaSyncService = diagnostic / conservative compatibility
```

#21 完成后评估把 Safe Sync 收敛为只读 drift checker。

---

## 14. Hibernate Strategy

阶段门禁：

```text
#19 runtime ✅
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

#19 中 `ddl-auto` 保持 `update`。

---

## 15. Testing Gate

PR #24 已通过：

- Repository Governance；
- Backend Maven compile；
- Backend Maven test；
- PostgreSQL Testcontainers；
- KMP APK。

`MigrationRuntimeIntegrationTest` 验证：

1. public bootstrap 首次 explicit baseline + migration；
2. public bootstrap 幂等；
3. PLAN 不创建 tenant history；
4. PLAN 不执行 tenant DDL；
5. allActive 排除 public company；
6. APPLY company A 不改变 company B；
7. company A history 写入；
8. repeat APPLY 幂等；
9. APPLY 保存真实 from/target version；
10. legacy non-empty/no-history tenant 被拒绝；
11. legacy table 不被修改；
12. advisory lock 阻止同 tenant 第二执行者。

#25 必须增加“空 schema → 完整 core tenant schema”的独立验证，不能把 #19 的 metadata migration 测试当成 core baseline 测试。

---

## 16. Issue Roadmap

```text
#12 Tenant Migration Epic
│
├── #18 Architecture Contract / ADR-0004  ✅
├── #19 Migration Runtime                 ✅ CI / PR #24 ready
├── #25 Core Tenant Baseline              ⏭ NEXT
├── #20 New Tenant Provisioning           depends #25
└── #21 Existing Tenant Baseline/Validate depends #25
```

依赖：

```text
#8 Backend CI ✅
       ↓
#18 ✅
       ↓
#19 ✅ CI
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

以下文档保留为背景和实现思路来源：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

状态：**HISTORICAL DESIGN INPUT**。

若内容冲突：

> ADR-0004 > 本文 > 已合并实现 > 历史文档。

每个 #19/#25/#20/#21 PR 都必须同步更新本文和 `doc/PROJECT_MASTER.md`。
