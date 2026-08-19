# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> 架构决策：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> 当前阶段：#18 Architecture Contract

本文是 Group-IM **当前租户数据库迁移设计入口**。如果历史文档、PR 评论或旧实现蓝图与本文或 ADR-0004 冲突，以 ADR-0004 和本文为准。

---

## 1. 当前代码事实

### Tenant boundary

Group-IM 使用 PostgreSQL schema 作为公司租户边界。

HTTP 请求通常通过当前登录用户/公司解析 schema，但数据库迁移属于后台/控制面能力，不能依赖 HTTP `TenantContextFilter` 或 ThreadLocal。

### New company provisioning

当前 `CompanyService.save()`：

```text
save company
  -> SELECT public.create_or_sync_company_schema(schemaName, companyId)
```

数据库函数会创建 schema 并以 public 当前表结构作为模板建立/同步 tenant。

**状态：LEGACY / COMPATIBILITY**。

它仍是现有运行路径，但不是未来版本化迁移的目标实现。

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

**状态：TRANSITIONAL SAFETY TOOL**。

### Hibernate

当前：

```yaml
spring.jpa.hibernate.ddl-auto: update
```

**状态：TECHNICAL DEBT / NOT MIGRATION SOURCE OF TRUTH**。

在 #21 完成 tenant coverage 前不直接切生产 `validate`。

---

## 2. Target Architecture

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
MigrationRunWorker
        ↓
TenantMigrationExecutor
        ↓
Postgres advisory lock
        ↓
PLAN / APPLY / RETRY
```

核心原则：

1. Flyway history 是每个 schema 的版本事实；
2. public control tables 只是运行/状态/审计投影；
3. 不在普通应用启动时自动迁移全部 tenant；
4. 后台 migration 显式指定 schema；
5. 同 tenant 使用 advisory lock；
6. 失败 tenant 停止后续版本，但不伪装成整体成功；
7. checksum 不一致视为不可自动继续的错误；
8. migration 文件发布后不可修改，变化使用新版本。

---

## 3. Migration Resource Layout

目标目录：

```text
server/src/main/resources/db/migration/
├── public/
│   └── V<version>__<description>.sql
└── tenant/
    └── V<version>__<description>.sql
```

版本号在具体实现 PR 中依据仓库当时已有 migration 历史决定，**禁止设计文档预设 V1/V2/V3 并直接复制到生产**。

### public scope

负责：

- migration control tables；
- 真正全局的 schema metadata；
- 必要的跨 tenant 控制面能力。

### tenant scope

负责：

- conversation/chat 等 tenant 业务表后续演进；
- Workbench/OA 新表；
- tenant 数据 backfill；
- tenant indexes / constraints。

Tenant SQL 不硬编码具体 company schema 名。

---

## 4. Control Plane

最小控制面：

```text
schema_migration_run
schema_migration_run_item
tenant_schema_state
```

### schema_migration_run

代表一次管理员请求：

- PLAN；
- APPLY；
- RETRY；
- 后续 BASELINE。

保存 requested_by、范围、target、release、开始/结束时间和汇总状态。

### schema_migration_run_item

每个 tenant 一个 item：

- company/schema；
- from/target；
- attempt；
- status；
- duration；
- safe error。

### tenant_schema_state

快速投影：

```text
UP_TO_DATE
PENDING
MIGRATING
FAILED
DRIFTED
DISABLED
```

注意：它不是 migration history 的唯一来源。

---

## 5. Runtime Modes

### PLAN

只读：

- 检查 schema；
- 读取 Flyway info；
- 校验 checksum；
- 列出 pending migrations；
- 不执行 DDL。

### APPLY

- 获取 advisory lock；
- 执行 pending migrations；
- 更新 run item / state；
- 失败后停止该 tenant 后续 migration。

### RETRY

只重试失败/未完成 tenant，不重跑已成功版本。

### BASELINE

由 #21 实现。

必须在结构 preflight 通过后，由明确授权的管理员执行。

---

## 6. Concurrency

禁止跨所有 tenant 的大事务。

```text
Run
 ├─ tenant A lock → migrate → record
 ├─ tenant B lock → migrate → record
 └─ tenant C lock → failed → record
```

同一 tenant 的并发任务：只允许一个执行者。

首版默认低并发，优先正确性而不是迁移吞吐。

---

## 7. New Tenant Lifecycle

由 #20 实现，目标：

```text
Company metadata created as unavailable
        ↓
CREATE SCHEMA
        ↓
public/global compatibility setup if required
        ↓
Flyway tenant migrations from empty schema
        ↓
verify target version
        ↓
activate company
```

失败：company 不可进入正常登录/切换路径，并留下 provisioning/migration 状态。

当前 public clone 逻辑在 #20 之前仍保留兼容。

---

## 8. Existing Tenant Baseline

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

禁止：

- blind `baselineOnMigrate(true)`；
- 看到表存在就直接标记成功；
- 用 public 当前结构覆盖 drift 证据。

---

## 9. Safe Sync Boundary

过渡期：

```text
Flyway Migration Runtime = authoritative versioned change
SafeTenantSchemaSyncService = diagnostic / conservative compatibility
```

当 #21 完成后评估：

- 关闭 Safe Sync 自动写字段；
- 保留/迁移为 `TenantSchemaDriftChecker` 只读检查。

---

## 10. Hibernate Strategy

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

不能反向执行。

---

## 11. API Target

#19 最小管理 API：

```http
POST /api/admin/schema-migrations/runs
GET  /api/admin/schema-migrations/runs/{runId}
GET  /api/admin/schema-migrations/tenants
POST /api/admin/schema-migrations/runs/{runId}/retry
```

旧：

```http
POST /api/organization/company/sync-schema
```

过渡策略：先保留；后续改为 compatibility adapter 或 deprecated，不再同步返回“所有迁移已完成”的错觉。

---

## 12. Security and Audit

Migration 管理接口必须走统一 authorization service，不能继续散落：

```text
username == "admin"
```

控制面至少记录：

- user id；
- run id；
- company scope；
- source IP（可获得时）；
- target/release；
- result；
- safe error。

日志/API 不泄露数据库密码、JDBC URL 或敏感 SQL 参数。

---

## 13. Testing Gate

#19 起至少需要：

### Unit

- schema validator；
- run state transition；
- authorization；
- target version resolution。

### PostgreSQL integration

建议 Testcontainers：

```text
public
company_a
company_b
```

验证：

1. PLAN 不改表；
2. APPLY company_a 不改变 company_b；
3. company_a history 写入；
4. 重复 APPLY 幂等；
5. checksum/invalid history 被拒绝；
6. advisory lock 阻止同 tenant 并发；
7. 一 tenant 失败不会被汇总为全成功。

Backend PR CI 是 merge gate。

---

## 14. Issue Roadmap

```text
#12 Tenant Migration Epic
│
├── #18 Architecture Contract / ADR-0004
├── #19 Migration Runtime
├── #20 New Tenant Provisioning
└── #21 Existing Tenant Baseline / ddl-auto validate
```

依赖：

```text
#8 Backend CI (implemented)
       ↓
#18
       ↓
#19
      ├─→ #20
      └─→ #21
       ↓
#12 complete
       ↓
#13 Workbench Platform Foundation
```

---

## 15. Historical Documents

以下文档保留，作为问题背景和实现思路来源：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

它们的状态是 **HISTORICAL DESIGN INPUT**。

若内容与 ADR-0004 / 本文冲突：

> ADR-0004 > 本文 > 后续已合并实现 > 历史文档。

随着 #19/#20/#21 实现推进，真实状态必须持续回写 `doc/PROJECT_MASTER.md` 和本文。
