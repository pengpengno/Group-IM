# Trusted Tenant Schema Snapshot / Inventory

> 状态：CURRENT DESIGN / Issue #26  
> Parent Epic：#12  
> 上游：#19 Migration Runtime  
> 下游：#25 Core Tenant Baseline Migrations

本文说明如何从一个管理员已经确认“结构健康”的 tenant schema，生成**离线、只读、可审阅**的数据库结构证据，作为 #25 固化 Core Tenant Baseline 的输入。

这不是 migration 执行器，也不是生产管理 API。

---

## 1. 为什么需要 Snapshot

当前 Group-IM 的 tenant 结构不仅来自 JPA Entity。

legacy `create_or_sync_company_schema(...)` 还会处理：

- tenant tables；
- global identity views；
- foreign keys；
- indexes；
- CHECK constraints；
- database defaults / identity；
- 其他 PostgreSQL schema objects。

因此不能只根据 Java Entity 手写一份“看起来完整”的 baseline SQL。

#26 通过两份独立证据降低遗漏风险：

```text
PostgreSQL catalog inventory
        +
pg_dump --schema-only
        ↓
review / normalize / compare
        ↓
#25 immutable Flyway baseline
```

---

## 2. 工具位置

```text
scripts/tenant-schema-snapshot/
├── export_tenant_schema_snapshot.sh
└── tenant_schema_inventory.sql
```

验证：

```text
.github/workflows/tenant-schema-snapshot.yml
server/src/test/java/com/github/im/server/schema/migration/TenantSchemaInventorySqlIntegrationTest.java
```

---

## 3. 只读边界

导出工具必须满足：

- 不执行 CREATE / ALTER / DROP / INSERT / UPDATE / DELETE；
- catalog inventory 在 `REPEATABLE READ READ ONLY` transaction 中执行；
- `pg_dump` 使用 `--schema-only`；
- 不导出业务 row data；
- 不导出 owner / privilege；
- 不把数据库密码写入输出；
- 不允许 target schema 为 `public`；
- 不允许非法 schema name；
- 不允许输出目录位于 Git worktree 内；
- `flyway_schema_history` 和 `tenant_schema_metadata` 从 core baseline evidence 中排除。

生成的 `schema.sql` 是**reference evidence**，不是可直接执行的 migration。

---

## 4. 选择 Trusted Tenant

Snapshot 只能从管理员明确确认健康的 tenant 生成。

建议条件：

1. 当前业务功能正常；
2. 没有已知 schema conflict；
3. Safe Sync / 运维检查没有未处理的结构错误；
4. tenant 能代表当前生产目标结构；
5. snapshot 期间没有并发 DDL 发布。

如果不同健康 tenant 的 inventory 不一致，不应任选其中一个作为 baseline，而应先进入 drift 分析。

---

## 5. PostgreSQL Client 要求

需要：

```text
psql
pg_dump
sha256sum
python3
```

建议 `pg_dump` major version 与 PostgreSQL server 相同或更新且兼容。

优先使用专用只读账号，并通过以下方式提供连接信息：

1. `PGSERVICE` + service file；或
2. `.pgpass`；或
3. PostgreSQL 标准 `PGHOST` / `PGPORT` / `PGDATABASE` / `PGUSER` 环境变量。

不建议把密码直接写在命令参数、脚本或 Git 文件里。

---

## 6. 运行示例

使用标准 PostgreSQL 环境变量：

```bash
export PGHOST=db.example.internal
export PGPORT=5432
export PGDATABASE=group
export PGUSER=group_schema_reader

TENANT_SCHEMA=company_a \
SNAPSHOT_OUTPUT_DIR=/tmp/group-im-tenant-schema-snapshots \
bash scripts/tenant-schema-snapshot/export_tenant_schema_snapshot.sh
```

也可以把 schema 作为第一个参数：

```bash
bash scripts/tenant-schema-snapshot/export_tenant_schema_snapshot.sh company_a
```

默认输出目录：

```text
/tmp/group-im-tenant-schema-snapshots
```

工具会拒绝把 snapshot 输出到当前 Git worktree 内，避免误提交真实数据库结构快照。

---

## 7. 输出结构

每次运行创建独立目录：

```text
company_a-20260819T123456Z/
├── inventory.json
├── schema.sql
└── manifest.json
```

### `inventory.json`

确定性 catalog inventory，当前包含：

- tables；
- columns / PostgreSQL data type；
- nullability；
- default expression；
- identity / generated metadata；
- PRIMARY KEY；
- UNIQUE；
- FOREIGN KEY；
- CHECK；
- indexes；
- views + definitions；
- sequences；
- triggers；
- schema-local functions / procedures；
- domains；
- enum labels。

### `schema.sql`

由 `pg_dump --schema-only` 生成的 SQL reference。

用途：

- 与 inventory 交叉核对；
- 确认对象创建顺序；
- 查看 PostgreSQL 实际 DDL 表达；
- 为 #25 normalization 提供参考。

不要直接把整个文件改名为 Flyway migration 后执行。

### `manifest.json`

记录：

- snapshot format/tool version；
- source schema；
- UTC 生成时间；
- database name 的 SHA-256（不保存明文数据库名）；
- PostgreSQL server / psql / pg_dump version；
- `inventory.json` SHA-256；
- `schema.sql` SHA-256。

---

## 8. 一致性说明

Catalog inventory 自己在一个 `REPEATABLE READ READ ONLY` transaction 内保持一致。

`pg_dump --schema-only` 是第二个独立只读数据库会话，因此 inventory 与 dump **不是同一个 exported snapshot transaction**。

所以正式采集时要求：

> snapshot 运行期间不要进行 schema DDL。

如果采集窗口内发生 DDL，废弃该次输出并重新采集。

#25 会把 inventory 与 SQL reference 交叉审阅，而不是依赖单一文件。

---

## 9. Inventory 排除项

当前从 core schema inventory 排除：

```text
flyway_schema_history
tenant_schema_metadata
```

原因：

- `flyway_schema_history` 是 migration runtime history，不属于业务 baseline object；
- `tenant_schema_metadata` 是 #19 runtime 自身 metadata，不应污染“现有核心业务结构”对比。

后续如果出现更多 migration/runtime-only objects，应通过新的 PR 明确加入排除规则。

---

## 10. 安全审阅流程

推荐：

```text
管理员选择 trusted tenant
        ↓
只读 snapshot
        ↓
人工检查 manifest/hash
        ↓
检查 inventory.json
        ↓
检查 schema.sql
        ↓
与第二个健康 tenant 对比（推荐）
        ↓
#25 normalize 为 immutable baseline migrations
        ↓
Testcontainers empty schema verification
```

Snapshot 本身不会：

- 自动创建 Issue/PR；
- 自动提交输出文件；
- 自动执行 `schema.sql`；
- 自动写 baseline history；
- 自动修改 production tenant。

---

## 11. CI 验证

`Tenant Schema Snapshot Validation` 在以下路径发生变化时运行：

```text
scripts/tenant-schema-snapshot/**
TenantSchemaInventorySqlIntegrationTest.java
.github/workflows/tenant-schema-snapshot.yml
```

CI 包含：

```text
bash -n export_tenant_schema_snapshot.sh
        ↓
PostgreSQL 16 Testcontainers
        ↓
tenant_schema_inventory.sql
        ↓
JSON assertions
```

集成测试会建立具有代表性的：

- enum；
- domain；
- sequence；
- table；
- identity；
- PK/UNIQUE/FK/CHECK；
- index；
- view；
- function；
- trigger。

并验证 migration runtime tables 被排除。

---

## 12. 与 #25 的边界

#26 的输出是**证据**。

#25 才负责：

- 决定 tenant/global object inventory；
- normalize schema qualification；
- 固定 global identity view 策略；
- 固化 immutable Flyway baseline SQL；
- 从真正空 schema 验证完整核心 tenant 结构；
- 与 trusted inventory 做结构对比。

因此：

```text
#26 snapshot != #25 migration
```

这个边界必须保持。

---

## 13. 后续

完成 #26 后：

```text
#25 Core Tenant Baseline
  ↓
#20 New Tenant Provisioning
+
#21 Existing Tenant Baseline / Validate
```

每个阶段继续使用 Issue → Branch → PR → CI → PROJECT_MASTER → Merge。
