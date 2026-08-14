# 租户 Schema 迁移代码设计

> 本文是《租户Schema版本与迁移机制方案》的实现蓝图。目标是先完成安全、可观测的最小闭环；不在业务应用启动时执行租户 DDL。

## 1. 技术选型与边界

选用 **Flyway** 执行每个租户 schema 的版本化 SQL；不自研 SQL 分割、checksum 与迁移历史逻辑。Flyway 在每一个租户 schema 内维护 `flyway_schema_history`，这是该租户版本的权威来源。`public` schema 的控制表只管理批量任务、状态投影、审计和全局查询。

迁移模块的职责：

```text
HTTP Controller
  -> MigrationRunService（创建任务、查询状态、授权）
  -> MigrationRunWorker（异步调度）
  -> TenantMigrationExecutor（一个公司/schema 的互斥迁移）
  -> TenantFlywayFactory（为指定 schema 创建 Flyway）
  -> PostgreSQL + Flyway history
```

明确不做：

- 不再调用 `create_or_sync_company_schema` 来复制 `public` 的表结构。
- 不使用 Hibernate `ddl-auto: update` 作为生产迁移渠道。
- 不把一个全局 version 字段当作执行历史。
- 不在请求线程内执行全量迁移，也不在一个跨租户大事务中执行。

## 2. Maven 与配置变更

在 `server/pom.xml` 取消 Flyway 依赖注释（版本由 Spring Boot 的 dependency management 管理）：

```xml
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

生产配置改为：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: false # 禁止 Spring Boot 在启动时只迁移 public

schema-migration:
  enabled: true
  locations: classpath:db/migration/tenant
  control-schema: public
  max-parallel-tenants: 2
  lock-wait-seconds: 0
  validate-on-migrate: true
  out-of-order: false
  task-retention-days: 90
```

开发环境可单独配置 `ddl-auto: update`，但不能把它视为迁移已完成的证据。CI 和生产必须执行 `validate`。

## 3. 数据库迁移文件

### 3.1 public 控制表

先以一次**公共 schema**迁移创建控制表：

`db/migration/public/V202608140001__create_schema_migration_control_tables.sql`

```sql
CREATE TABLE IF NOT EXISTS schema_migration_run (
  id uuid PRIMARY KEY,
  requested_by bigint NOT NULL,
  request_mode varchar(16) NOT NULL CHECK (request_mode IN ('PLAN', 'APPLY', 'RETRY', 'BASELINE')),
  requested_company_ids jsonb,
  target_version varchar(50),
  release_version varchar(100),
  status varchar(24) NOT NULL,
  total_count integer NOT NULL DEFAULT 0,
  success_count integer NOT NULL DEFAULT 0,
  failed_count integer NOT NULL DEFAULT 0,
  skipped_count integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  started_at timestamptz,
  finished_at timestamptz,
  requested_ip inet,
  last_error text
);

CREATE TABLE IF NOT EXISTS schema_migration_run_item (
  id bigserial PRIMARY KEY,
  run_id uuid NOT NULL REFERENCES schema_migration_run(id),
  company_id bigint NOT NULL REFERENCES company(company_id),
  schema_name varchar(63) NOT NULL,
  from_version varchar(50),
  target_version varchar(50),
  status varchar(24) NOT NULL,
  attempt integer NOT NULL DEFAULT 1,
  started_at timestamptz,
  finished_at timestamptz,
  duration_ms bigint,
  error_code varchar(100),
  error_message text,
  UNIQUE (run_id, company_id)
);

CREATE TABLE IF NOT EXISTS tenant_schema_state (
  company_id bigint PRIMARY KEY REFERENCES company(company_id),
  schema_name varchar(63) NOT NULL UNIQUE,
  current_version varchar(50),
  target_version varchar(50),
  status varchar(24) NOT NULL,
  last_success_at timestamptz,
  last_checked_at timestamptz,
  last_error text,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_schema_migration_run_item_run_status
  ON schema_migration_run_item(run_id, status);
CREATE INDEX IF NOT EXISTS idx_tenant_schema_state_status
  ON tenant_schema_state(status, current_version);
```

公共控制表的自身迁移在 CI/CD 的单实例 migration job 中执行。不要把它放进每个租户的 Flyway locations。

### 3.2 租户迁移

租户脚本不带 schema 前缀，Flyway 的 `defaultSchema` 决定目标：

`db/migration/tenant/V202608140010__conversation_member_role.sql`

```sql
ALTER TABLE conversation_members
  ADD COLUMN IF NOT EXISTS role varchar(16);

UPDATE conversation_members cm
SET role = CASE WHEN c.created_by = cm.user_id THEN 'OWNER' ELSE 'MEMBER' END
FROM conversations c
WHERE c.conversation_id = cm.conversation_id
  AND cm.role IS NULL;

UPDATE conversation_members SET role = 'MEMBER' WHERE role IS NULL;

ALTER TABLE conversation_members
  ALTER COLUMN role SET DEFAULT 'MEMBER';
ALTER TABLE conversation_members
  ALTER COLUMN role SET NOT NULL;

ALTER TABLE conversation_members
  ADD CONSTRAINT ck_conversation_members_role
  CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'));
```

说明：`ADD CONSTRAINT` 在已被手动修复的 schema 上可能因为同名约束失败。生产首个版本应先执行 `PLAN` 和 drift 检查；如需同时兼容已修复租户，单独提供一次性 repair migration，不在版本脚本中通过捕获异常静默跳过。

## 4. 推荐代码目录

```text
server/src/main/java/com/github/im/server/schema/migration/
  config/
    TenantSchemaMigrationProperties.java
    SchemaMigrationExecutorConfig.java
  api/
    SchemaMigrationAdminController.java
    CreateMigrationRunRequest.java
    MigrationRunResponse.java
    TenantMigrationStatusResponse.java
  domain/
    MigrationRunStatus.java
    MigrationRunMode.java
    TenantMigrationStatus.java
    MigrationRun.java
    MigrationRunItem.java
    TenantSchemaState.java
  persistence/
    MigrationRunJdbcRepository.java
    TenantSchemaStateJdbcRepository.java
  service/
    SchemaMigrationAuthorization.java
    MigrationRunService.java
    MigrationRunWorker.java
    TenantMigrationExecutor.java
    TenantFlywayFactory.java
    TenantSchemaProvisioner.java
    TenantSchemaDriftChecker.java
  support/
    SchemaNameValidator.java
    PostgresAdvisoryLock.java
```

控制表建议使用 JDBC repository，而不是现有会受到 `TenantContext` 影响的 JPA repository。每次控制面访问从 `DataSource` 取连接并显式 `connection.setSchema("public")`；从而异步 worker 不依赖 HTTP ThreadLocal。

## 5. 核心类型与接口

```java
public enum MigrationRunMode { PLAN, APPLY, RETRY, BASELINE }
public enum MigrationRunStatus { QUEUED, RUNNING, SUCCEEDED, PARTIAL_FAILED, FAILED, CANCELLED }
public enum TenantMigrationStatus {
    PENDING, PLANNED, MIGRATING, UP_TO_DATE, SUCCEEDED, FAILED, SKIPPED, DRIFTED
}

public record CreateMigrationRunRequest(
        MigrationRunMode mode,
        List<Long> companyIds,  // null/empty = all active companies
        String targetVersion,
        boolean includeInactive,
        String changeTicket
) {}

public interface SchemaMigrationAuthorization {
    void requireMigrationAdmin(User user);
}

public interface TenantFlywayFactory {
    Flyway create(String schemaName);
}
```

`TenantFlywayFactory` 的关键实现：

```java
@Component
@RequiredArgsConstructor
class DefaultTenantFlywayFactory implements TenantFlywayFactory {
  private final DataSource dataSource;
  private final TenantSchemaMigrationProperties properties;

  @Override
  public Flyway create(String schemaName) {
    SchemaNameValidator.requireValid(schemaName);
    return Flyway.configure()
        .dataSource(dataSource)
        .schemas(schemaName)
        .defaultSchema(schemaName)
        .locations(properties.getLocations())
        .createSchemas(false)
        .validateOnMigrate(properties.isValidateOnMigrate())
        .outOfOrder(false)
        .load();
  }
}
```

禁止 `baselineOnMigrate(true)`：它会把未知旧结构误标为已迁移。旧租户 baseline 必须走单独的 `BASELINE` 任务，并先完成结构指纹校验和人工确认。

## 6. 单租户执行算法

`TenantMigrationExecutor` 对一个租户的流程必须保持如下语义：

```java
TenantExecutionResult execute(MigrationRun run, Company company) {
  SchemaNameValidator.requireValid(company.getSchemaName());
  try (Connection lockConnection = dataSource.getConnection()) {
    lockConnection.setSchema("public");
    if (!advisoryLock.tryLock(lockConnection, company.getSchemaName())) {
      return skipped("SCHEMA_LOCKED");
    }
    try {
      stateRepository.markMigrating(company);
      Flyway flyway = flywayFactory.create(company.getSchemaName());
      MigrationInfoService info = flyway.info(); // 读取 from/target、校验 checksum
      if (run.mode() == PLAN) return planned(info.pending());
      flyway.migrate(); // 每个 Flyway SQL 迁移独立提交，历史只在成功后写入
      return succeeded(flyway.info().current());
    } catch (FlywayValidateException ex) {
      return failed("VALIDATION_FAILED", safeMessage(ex));
    } catch (FlywayException | SQLException ex) {
      return failed("MIGRATION_FAILED", safeMessage(ex));
    } finally {
      advisoryLock.unlock(lockConnection, company.getSchemaName());
    }
  }
}
```

`PostgresAdvisoryLock` 以 session 级锁实现：

```sql
SELECT pg_try_advisory_lock(hashtextextended(:schemaName, 0));
SELECT pg_advisory_unlock(hashtextextended(:schemaName, 0));
```

锁连接从获取到释放始终打开。Flyway 使用另一连接不影响互斥性：其他 worker 会先争抢同一 advisory lock 并失败/等待。

外层不能标注一个覆盖所有公司的 `@Transactional`。每个租户的迁移和控制表状态更新使用独立短事务；任务失败后仍要可靠写入失败记录。

## 7. 异步任务与状态更新

`MigrationRunService.createRun` 负责：校验管理员权限、解析公司范围、创建 `QUEUED` 的 run 与 PENDING items、提交后调用 worker。

```java
@Async("schemaMigrationExecutor")
public void execute(UUID runId) {
  repository.markRunRunning(runId);
  for (MigrationRunItem item : repository.lockAndListRunnableItems(runId)) {
    TenantExecutionResult result = executor.execute(run, item.company());
    repository.completeItemAndRefreshState(runId, item.companyId(), result);
  }
  repository.finishRunFromItemCounts(runId);
}
```

线程池使用独立且有界的 `ThreadPoolTaskExecutor`：核心线程 1、最大线程 2、队列 20，拒绝策略为记录任务仍为 `QUEUED`。首期以顺序执行为默认，后续才可在“每 schema 一把锁”的前提下按 `max-parallel-tenants` 并发。

进程重启恢复：应用启动后或定时任务将运行超过阈值（例如 30 分钟）的 `RUNNING/MIGRATING` 标为 `INTERRUPTED`，允许管理员以 `RETRY` 创建新任务。不得假定旧 worker 已完成。

## 8. API 设计

新 controller 为 `SchemaMigrationAdminController`，只挂载管理员路径：

| 方法 | 路径 | 成功返回 |
| --- | --- | --- |
| `POST` | `/api/admin/schema-migrations/runs` | `202 Accepted` 与 `runId`、状态 `QUEUED` |
| `GET` | `/api/admin/schema-migrations/runs/{id}` | 任务计数、时间、逐租户结果 |
| `GET` | `/api/admin/schema-migrations/tenants` | current/target/status/error 分页列表 |
| `POST` | `/api/admin/schema-migrations/runs/{id}/retry` | 新 runId；只复制 FAILED/SKIPPED 项 |
| `POST` | `/api/admin/schema-migrations/tenants/{companyId}/baseline` | 仅通过 drift 校验并带确认参数时可用 |
| `GET` | `/api/admin/schema-migrations/drift` | 只读漂移报告 |

旧接口 `/api/organization/company/sync-schema` 第一阶段保留，内部改为：

```java
var request = new CreateMigrationRunRequest(APPLY, companyIds, null, false, "legacy-sync-schema");
return ResponseEntity.accepted().body(ApiResponse.success(runService.createRun(request, user)));
```

这样不会再同步返回“完成”，而是返回可查询的任务编号。下一个大版本移除该旧路径。

## 9. 新租户创建流程

替换 `CompanyService.save` 和 `CompanyCreatedEventListener` 中对 `create_or_sync_company_schema` 的调用：

1. 保存 `company` 记录（状态为 `PROVISIONING` 或暂不 `active`）。
2. `TenantSchemaProvisioner` 用校验后的标识符执行 `CREATE SCHEMA IF NOT EXISTS`。
3. 执行该 schema 的 Flyway 全量迁移。
4. 创建三张全局表视图（如仍确有跨 schema ORM 兼容需求，则将这部分视图建模为独立、版本化 SQL）。
5. 初始化 `tenant_schema_state` 为最新版本，最后激活公司。

失败时公司保持不可用并记录原因；不可创建“有 company 记录但没有完整 schema”的可登录租户。

## 10. 权限、审计和错误处理

第一期新增 `SchemaMigrationAuthorization`，可临时复用当前系统管理员判定，但所有 controller 必须通过这个统一接口；后续将实现替换为明确的 `SCHEMA_MIGRATION_ADMIN` authority，不再散落 `username.equals("admin")`。

错误记录规则：

- API 响应不返回 JDBC URL、SQL、密码或完整堆栈。
- `schema_migration_run_item.error_message` 保存截断后的安全错误；完整堆栈只入服务日志并关联 `runId`。
- Flyway checksum 不一致、历史版本缺失、结构漂移均为不可自动重试的 `FAILED`。
- 普通 SQL 临时错误（死锁、连接中断）可以 RETRY，但仍创建新的 run item attempt，不覆盖旧证据。

## 11. 测试清单

使用 Testcontainers PostgreSQL（不能只用 H2）覆盖：

1. 空 schema 执行全部迁移，包含 `flyway_schema_history` 和 `role` 正确约束。
2. 旧 schema 执行 `role` 迁移：创建者是 `OWNER`，其他成员是 `MEMBER`，无 null。
3. 迁移文件 checksum 被修改时，`validate` 失败且 state 为 `FAILED`。
4. 两次并发同 schema 的 APPLY：仅一次获得 lock，另一次 `SKIPPED/SCHEMA_LOCKED`。
5. 一个租户失败、另一个成功时，run 为 `PARTIAL_FAILED`，HTTP 任务创建仍为 202。
6. RUNNING 任务模拟进程中断后的超时恢复与 RETRY。
7. `PLAN` 不产生 DDL、不写租户 Flyway history。
8. 新建公司失败时不会被标记 active。

## 12. 分两期交付

**一期（必须先做）**：Flyway 依赖、public 控制表、租户 `role` 脚本、单 schema executor、异步 run、状态/详情 API、旧接口兼容改造、Testcontainers 集成测试、生产 `ddl-auto: validate`。

**二期**：drift checker、baseline 管理、任务恢复、发布门禁、仪表盘/告警、专用迁移数据库账号、替换旧 schema 复制函数与新租户初始化流程。

一期完成后就可以安全处理当前 `pingduoduo` 的问题并准确看到每个租户的升级结果；二期完成后形成长期治理闭环。
