# ADR-0004: Versioned Tenant Schema Migrations

- Status: Accepted
- Date: 2026-08-19
- Related Epic: #12
- Related Issue: #18
- Follow-ups: #19, #20, #21

## Context

Group-IM 使用 PostgreSQL Schema 作为公司租户边界。当前生产代码同时存在三种数据库结构来源：

1. Hibernate `ddl-auto=update`；
2. `public.create_or_sync_company_schema(schemaName, companyId)`，以 `public` 当前表结构初始化/同步 tenant；
3. `SafeTenantSchemaSyncService`，检测结构差异并只自动补部分安全字段。

这三者都不能承担未来 Workbench/OA 新业务表的可靠版本发布：

- Hibernate update 没有多 tenant 发布历史、审批和失败审计；
- public clone 表达的是“现在长什么样”，不是“按什么版本演进到这里”；
- Safe Sync 刻意保守，不创建缺失表，也不负责有序数据迁移、索引和复杂约束。

仓库已有两份历史设计文档：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

它们的核心方向成立，但从未完整落地，且部分建议（例如过早切 `ddl-auto=validate`、如何 bootstrap public migration）需要结合当前代码分阶段实施。

Workbench 的 `wb_task`、`wb_approval_*` 等表必须在正式版本化迁移机制存在后才能落库。

## Decision

### 1. Flyway 是版本化 SQL 与单 tenant history 的唯一执行器

新 DDL / DML migration 使用 Flyway，不自研 SQL parser、checksum 或 migration ordering。

每个 tenant schema 自己维护：

```text
<tenant>.flyway_schema_history
```

它是该 tenant 已执行 migration 的权威事实。

public/control plane 可以有自己的 Flyway history，但不能把一个全局 `current_version` 字段当成所有 tenant 的执行真相。

### 2. public control plane 只保存运行与状态投影

public schema 后续维护：

```text
schema_migration_run
schema_migration_run_item
tenant_schema_state
```

它们用于：

- 批量运行审计；
- 快速状态查询；
- 失败原因；
- target/current projection；
- 操作者与运行范围。

真正“某个版本是否已经执行”的事实仍来自目标 schema 的 Flyway history。

### 3. 不在普通应用启动时无条件迁移全部 tenant

禁止以下模式：

```text
Application startup
  -> enumerate all companies
  -> migrate every tenant
```

原因：

- 多实例启动会放大 DDL 并发；
- 某个大 tenant 会拖慢服务启动；
- 无法做发布分批、审批和失败隔离；
- 运维无法选择 PLAN / APPLY / RETRY 范围。

Tenant migration 通过显式 Migration Runtime 执行。

新租户 provisioning 是例外：只迁移“刚创建的一个 tenant”，作为租户可用前置步骤。

### 4. Migration Runtime 不依赖 HTTP TenantContext

Migration Runtime 的控制面 JDBC 必须显式操作 `public` schema。

单 tenant 执行器必须显式使用目标 schema，不依赖：

- `TenantContextFilter`；
- `SchemaContext` ThreadLocal；
- 当前 HTTP request。

后台 worker、管理 API 和 provisioning 都使用相同的显式 schema executor/factory。

### 5. 使用 PostgreSQL advisory lock 保护单 tenant

同一 tenant 同一时刻只能有一个 migration executor。

建议锁键：

```text
tenant-schema:<schemaName>
```

通过 session-level PostgreSQL advisory lock 实现。不同 tenant 可以在受控并发度下并行。

### 6. Safe Sync 与 Migration Runtime 职责严格分离

`SafeTenantSchemaSyncService` 在过渡期保留，但职责限定为：

- drift / difference inspection；
- 兼容期的极保守字段补齐；
- 人工诊断辅助。

它不得：

- 创建 Workbench 新表；
- 伪造 migration history；
- 覆盖 Flyway checksum/history；
- 自动把 DRIFTED tenant 修成“看起来和 public 一样”。

#21 完成后，应进一步评估把 Safe Sync 收敛为只读 drift checker。

### 7. 旧 `create_or_sync_company_schema` 进入兼容退役路径

当前 `CompanyService.save()` 和 `syncSchemas()` 仍调用数据库函数：

```sql
public.create_or_sync_company_schema(...)
```

过渡阶段不在 #19 直接删除，以避免同时改动 migration runtime 与 company lifecycle。

退役顺序：

```text
#19 Migration Runtime
  -> #20 New Tenant Provisioning
  -> #21 Existing Tenant Baseline / Validate
  -> legacy sync API deprecated / removed later
```

#20 完成后，新租户主路径必须是：

```text
save company as unavailable
  -> create empty schema
  -> run all tenant migrations
  -> verify target version
  -> mark company available
```

不再把 public LIKE clone 当作新 tenant 的结构来源。

### 8. 旧 tenant 不允许 blind baseline

禁止：

```text
baselineOnMigrate(true)
```

用于自动吞掉未知历史结构。

旧 tenant 必须：

```text
read-only structural preflight
  -> compare expected baseline fingerprint
  -> BASELINE_READY or DRIFTED/CONFLICT
  -> explicit authorized baseline
  -> audit operator/version/fingerprint
```

只有结构被确认可信，才允许写入 baseline history。

### 9. `ddl-auto=update -> validate` 分阶段完成

#19 不直接修改生产默认值为 `validate`。

阶段顺序：

1. Backend CI 能验证 migration/runtime；
2. Migration Runtime 可用；
3. 新租户 provisioning 切换到 migration；
4. 老租户完成可信 baseline/drift 分类；
5. 所有 active tenant 达到 required version；
6. staging/production 先验证 `validate`；
7. 再把生产默认策略切到 `validate`。

在此之前，`ddl-auto=update` 仍是明确技术债，但不通过一次高风险配置修改解决。

### 10. public migration bootstrap 也是显式操作

`spring.flyway.enabled` 不用于“启动应用顺便迁移 public”。

Migration Runtime 的 public/control tables 通过明确的 public-scope Flyway bootstrap / deployment operation 建立，并使用 public 自己的 `flyway_schema_history`。

该 bootstrap 必须：

- 可重复验证；
- 不扫描 tenant；
- 有明确日志/失败状态；
- 在 tenant runtime 开始前完成。

具体启动入口由 #19 实现时确定，但不得退化为“每个应用实例启动都跑所有 tenant”。

## Runtime Boundary

目标模块边界：

```text
schema/migration/
├── config/
├── api/
├── domain/
├── persistence/
├── service/
│   ├── PublicMigrationBootstrap
│   ├── TenantFlywayFactory
│   ├── MigrationRunService
│   ├── MigrationRunWorker
│   ├── TenantMigrationExecutor
│   ├── TenantSchemaProvisioner   # #20
│   └── TenantSchemaDriftChecker # #21
└── support/
    ├── SchemaNameValidator
    └── PostgresAdvisoryLock
```

控制面优先使用 JDBC repository，避免被 tenant JPA context 影响。

## Delivery Phases

### #18 — Architecture Contract

本 ADR + canonical migration docs。

### #19 — Runtime

- Flyway dependencies/config；
- public bootstrap；
- tenant factory/executor；
- PLAN/APPLY；
- run/item/state；
- advisory lock；
- admin API；
- multi-tenant integration tests。

### #20 — New Tenant Provisioning

- 公司先不可用；
- create schema；
- migrate；
- verify；
- activate；
- legacy public clone 不再是新 tenant 主路径。

### #21 — Existing Tenant Baseline / Validate

- drift/preflight；
- explicit baseline；
- repair plan；
- staged `ddl-auto=validate`；
- legacy sync 进入 deprecated 状态。

## Consequences

### Positive

- Workbench 新表有可靠发布路径；
- tenant 版本可审计；
- migration 失败不会被 HTTP 200 或 public clone 掩盖；
- 新 tenant 与旧 tenant 使用同一 SQL 历史；
- 多实例环境可安全互斥；
- 可以逐步从 Hibernate 自动 DDL 收敛到显式 migration。

### Negative / Cost

- 需要新增 control plane、worker、admin API 和集成测试；
- 老 tenant baseline 需要真实结构审查，不能“一键视为正常”；
- 过渡期会同时存在 Flyway、Safe Sync 和 legacy clone，但职责必须明确；
- company provisioning 需要从当前单事务思路改为有状态流程。

## Alternatives Considered

### 继续使用 public clone + Safe Sync

Rejected。它不能表达版本顺序、checksum、历史数据变更和可靠失败状态。

### Hibernate `ddl-auto=update` 管全部 tenant

Rejected。Hibernate 不是多 tenant 发布编排系统。

### 自研 migration engine

Rejected。SQL 执行、checksum、history、repair 等语义已有成熟 Flyway 能力，自研成本和风险不合理。

### 启动时自动 migrate 全部 tenant

Rejected。影响启动可靠性、分批发布和多实例 DDL 控制。

## Follow-ups

- #19 实现 Migration Runtime；
- #20 接管新 tenant provisioning；
- #21 baseline 老 tenant 并推进 `ddl-auto=validate`；
- #13 在 #12 基础上建立 Workbench Platform Foundation；
- Workbench Task/Approval 新表只有在 migration 路径可用后才能实现。
