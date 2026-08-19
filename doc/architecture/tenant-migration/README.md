# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> ADR：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12  
> 当前阶段：#25 Core Tenant Baseline — IMPLEMENTED / PR #33  
> Canonical core baseline version：`2026081906`  
> 下一阶段：#20 New Tenant Provisioning + #21 Existing Tenant Baseline/Validate

本文是 Group-IM 当前租户数据库迁移设计入口。冲突时以 ADR-0004、本文件和 `doc/PROJECT_MASTER.md` 的 current facts 为准。

---

## 1. 当前事实

- PostgreSQL schema 是 tenant boundary；
- Migration Runtime 已由 #19 / PR #24 建立；
- `spring.flyway.enabled=false`，普通应用启动不自动 migrate 全 tenant；
- `CompanyService.save()` 仍调用 `public.create_or_sync_company_schema(...)`；
- `SafeTenantSchemaSyncService` 仍是 transitional drift/compatibility tool；
- `spring.jpa.hibernate.ddl-auto` 仍是 `update`；
- #26 / PR #27 已提供 trusted schema snapshot/inventory tooling；
- #32 已完成 PostgreSQL 14.13 test tenant schema evidence review，结果为 `READY_FOR_BASELINE_WITH_NORMALIZATION`；
- #25 / PR #33 已把 reviewed contract 固化为 immutable tenant migrations，并通过 PostgreSQL 16 empty-schema Testcontainers 验证。

#25 不切换新公司 provisioning，不 baseline 旧 tenant，也不修改 `ddl-auto`。这些分别属于 #20、#21 和后续 staged rollout。

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
2. PLAN 只读，不创建 history、不执行 DDL；
3. APPLY 只允许空 schema 或已有可信 Flyway history 的 schema；
4. non-empty + no-history tenant 必须走 #21 explicit baseline/preflight；
5. 同 tenant 并发 APPLY 用 advisory lock；
6. migration 发布后不可修改，只能新增版本。

---

## 3. Tenant Migration Versions

```text
server/src/main/resources/db/migration/tenant/

V2026081901__create_tenant_schema_metadata.sql
V2026081902__create_core_automation_and_collaboration_tables.sql
V2026081903__create_core_file_and_meeting_tables.sql
V2026081904__create_core_message_and_user_tables.sql
V2026081905__add_core_tenant_keys_and_relationships.sql
V2026081906__create_tenant_identity_views.sql
```

空 tenant 完整 APPLY 后 current version 必须为：

```text
2026081906
```

---

## 4. Reviewed Core Tenant Contract

#32 的私有 schema-only evidence 与 peer tenant drift review 固定以下 contract：

```text
18 core tenant tables
3 tenant identity views
17 identity sequences
65 core constraints
26 PK/UNIQUE backing indexes
```

18 个 core tables：

```text
approval_requests
automation_executions
automation_rules
conversation_bot_configs
conversation_members
conversations
departments
file_resource
friendships
media_file_resource
meeting_participants
meetings
messages
status_updates
system_config_item
upload_chunk_record
user_departments
user_privacy_settings
```

Migration runtime 自有的 `flyway_schema_history` 和 `tenant_schema_metadata` 不计入 core object counts。

---

## 5. Canonical Normalization

Reviewed tenant 之间存在 drift，因此 baseline 不复制任何一个历史 tenant，而是 normalization：

```text
messages.content
  -> TEXT

meetings.scheduled_at
  -> timestamp(6) without time zone

messages.type CHECK
  -> TEXT / FILE / VOICE / VIDEO / IMAGE / MEDIA / MEETING / BOT_CARD
```

依据：

- active tenant 已存在长度超过 255 的 message payload；
- 当前 `Message` entity 明确使用 TEXT；
- public / peer tenant meeting timestamp 使用显式 precision 6；
- 当前 Java `MessageType` 已有 `BOT_CARD`，旧 snapshot CHECK 已落后于代码。

---

## 6. Global Identity / Tenant Views

Global identity authority：

```text
public.company
public.company_user
public.users
```

Tenant 只建立 projections：

```text
company      VIEW
company_user VIEW
users        VIEW
```

禁止把 snapshot 中捕获的 companyId 或 source schema name 写进 migration。

`V2026081906` 在 Flyway 当前 tenant schema 中创建 views，并通过 `public.company.schema_name = tenant schema name` 固定 tenant binding。`company_user` 从 public membership 投影；`users` 只暴露属于当前 tenant 的 global users。

---

## 7. Keys / Relationships

`V2026081905` 保留 reviewed legacy constraint names，便于 #21 做 deterministic drift compare。

包含：

- tenant → tenant FKs，例如 messages → conversations；
- tenant → public users FKs；
- PK / UNIQUE constraints；
- enum-like CHECK constraints；
- reviewed PK/UNIQUE backing index contract。

#25 不复制任何业务 row data。

---

## 8. Testcontainers Gate

`CoreTenantBaselineIntegrationTest` 已在 PostgreSQL 16 验证：

```text
minimal public global identity contract
        ↓
CREATE truly empty tenant schema
        ↓
Flyway migrate 2026081901..2026081906
        ↓
assert current version = 2026081906
```

精确断言：

- 18 core table names；
- 3 identity view names；
- 17 core identity sequences；
- 65 core constraints；
- 26 core indexes；
- `messages.content=TEXT`；
- `meetings.scheduled_at=timestamp(6)`；
- `messages_type_check` 包含 `BOT_CARD`；
- identity views 不泄漏另一 tenant 的 company/user。

`MigrationRuntimeIntegrationTest` 同步验证：

- empty tenant PLAN pending count = 6；
- APPLY target version = `2026081906`；
- repeat APPLY 幂等；
- legacy non-empty/no-history tenant 继续被拒绝；
- advisory lock 行为不变。

Hibernate 不参与 baseline test 后补结构。

---

## 9. #20 New Tenant Provisioning

#25 已合并，因此 #20 现在可以把新公司生命周期切换为：

```text
create company metadata as unavailable
        ↓
CREATE SCHEMA
        ↓
Flyway migrate to required version
        ↓
verify core schema
        ↓
activate company
```

#20 合并前，legacy `create_or_sync_company_schema` 继续作为兼容路径。

---

## 10. #21 Existing Tenant Baseline / Validate

#25 已合并，因此 #21 可以使用 `2026081906` contract：

```text
read-only inspect existing tenant
        ↓
compare with canonical core contract
        ↓
BASELINE_READY / DRIFTED / CONFLICT / ERROR
        ↓
explicit authorized baseline
        ↓
normal Flyway migration
```

禁止 blind `baselineOnMigrate(true)`。

---

## 11. Safe Sync / Hibernate Rollout

```text
#19 runtime ✅
  ↓
#26 snapshot tooling ✅
  ↓
#32 reviewed evidence ✅
  ↓
#25 core baseline 2026081906 ✅
  ↓
#20 new tenant migration provisioning
  +
#21 existing tenant baseline/validate
  ↓
all active tenants covered
  ↓
staging ddl-auto=validate
  ↓
production ddl-auto=validate
```

当前仍保持 `ddl-auto=update`。

---

## 12. Roadmap

```text
#18 Architecture ✅ PR #23
#19 Runtime ✅ PR #24
#26 Snapshot Tooling ✅ PR #27
#32 Snapshot Review ✅
#25 Core Tenant Baseline ✅ PR #33
├─ #20 New Tenant Provisioning ← NEXT
└─ #21 Existing Tenant Baseline/Validate ← NEXT
```

#20 与 #21 完成后才能考虑退役 legacy public clone 与 Safe Sync write path。

---

## 13. Historical Inputs

历史文档继续保留：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

它们是 Historical Design Input，不覆盖 ADR-0004 或本 canonical README。
