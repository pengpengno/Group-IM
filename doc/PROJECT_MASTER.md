# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文档描述 Group-IM 当前是什么、已经实现什么、正在实现什么、接下来做什么。所有代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 Pull Request 中同步更新本文档。

- 文档状态：ACTIVE
- 基线日期：2026-08-19
- 唯一开发主线：`master`
- 当前基线提交：`297eb89582d950db6d64a1ae074aa5c37c26d399`
- 仓库：`pengpengno/Group-IM`
- 治理规则：`doc/development/REPOSITORY_GOVERNANCE.md`
- 贡献指南：`CONTRIBUTING.md`

---

## 1. 项目目标

Group-IM 是面向组织协作的多端 IM 与办公平台。即时通信是协作主链路，Workbench 承载结构化办公能力，AI 与自动化嵌入会话和工作流程。

长期原则：

1. 消息是协作主链路；
2. Workbench 承载 Task / Approval / Schedule / Announcement / Report；
3. AI / Automation 不建立与业务服务冲突的第二份真相；
4. Web/Electron 与 Android 保持核心业务语义一致；
5. 多租户、权限、审计、迁移、文件和通知作为平台能力复用。

---

## 2. 仓库治理基线

- `master` 是唯一开发主线和 PR 合并目标；
- `main` 是历史遗留分支，不参与开发和发布；
- 所有代码、配置、数据库和文档变更必须通过 PR；
- Bug 必须先有 Issue；新功能、架构、数据库和重要重构原则上也必须先有 Issue；
- 每个 PR 必须关联 Issue，并同步更新本文件；
- 默认使用 Squash Merge。

### 当前治理状态

已实现：

- PR #5：Issue-driven PR、PROJECT_MASTER、贡献规范、模板、Governance CI、ADR；
- PR #16：Backend PR Validation，Java 21 Maven compile + test；
- #17：HealthCheckTest 基础设施隔离修复；
- PR #23 / #18：ADR-0004 与 canonical tenant migration architecture。

待完成：

- #6：保护 `master` 并设置 required checks；
- #7：部署 workflow 移除历史 `main` trigger；
- #9：Electron/Web PR CI；
- #22：清理 `server/pom.xml` 重复 dependency 声明。

当前 GitHub 插件未暴露 Branch Protection / Ruleset 写接口，因此 #6 仍需仓库管理员在 GitHub Settings 完成。

---

## 3. 技术架构概览

### Server

- Java 21 / Spring Boot 3.x；
- Maven 多模块：`common`、`entity`、`server`；
- Spring Security + JWT；
- Spring Data JPA；
- PostgreSQL Schema 多租户；
- Redis；
- WebSocket / 实时事件；
- Spring AI / AI Provider。

### Client

- Electron / React / TypeScript；
- Kotlin Multiplatform + Compose Android。

### 多租户与数据库当前事实

- HTTP 请求通过当前登录用户/公司上下文选择 tenant schema；
- `spring.jpa.hibernate.ddl-auto` 仍为 `update`；
- `SafeTenantSchemaSyncService` 是 drift/保守兼容工具，**不创建缺失整表**；
- `CompanyService.save()` 当前仍调用 `public.create_or_sync_company_schema(...)` 初始化新 tenant；
- public clone 是待 #20 退役的兼容路径；
- ADR-0004 已固定 Flyway 版本化 migration 为未来唯一新表发布路径。

---

## 4. 模块状态地图

状态：`STABLE` / `IN_PROGRESS` / `PLANNED` / `LEGACY` / `BLOCKED`。

| 模块 | 状态 | 当前事实 | 下一阶段 |
| --- | --- | --- | --- |
| 登录 / 鉴权 | STABLE | JWT/Spring Security 主链路存在 | RBAC 细化 |
| 多公司 / 多租户 | IN_PROGRESS | Schema 多租户 + Safe Sync + legacy public clone | #19/#20/#21 |
| 单聊 / 群聊 | STABLE | 核心 IM 主链路存在 | 一致性、搜索、治理 |
| 联系人 / 组织 | STABLE | 公司/部门/员工能力存在 | 权限与批量治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级权限与可靠性 |
| 音视频会议 | IN_PROGRESS | Meeting 服务、通知、多端入口存在 | 协作联动和可靠性 |
| AI 助手 | IN_PROGRESS | 服务端 AI/Bot/持久化能力持续演进 | 工具治理和可观测性 |
| 群自动化 | IN_PROGRESS | 规则、执行、管理入口存在 | 审批、审计、规则模型 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；正式 Feature Design 已合并 | #12/#13 后 Overview |
| OA Task | PLANNED | 正式领域设计已形成 | #12 + #13 后 Backend |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后实现 |
| OA Calendar | PLANNED | Meeting/Task 聚合方向已形成 | 后续迭代 |
| OA Announcement | PLANNED | Target/Receipt/通知方向已形成 | 后续迭代 |
| OA Report | PLANNED | Roadmap 能力 | Workbench V1 后评估 |
| Tenant Migration Architecture | STABLE | ADR-0004 / PR #23 已合并 | 持续随 runtime 更新 |
| Tenant Migration Runtime | IN_PROGRESS | #19 正在实现 PLAN/APPLY/control plane/Flyway/lock | CI 验证后合并 |
| Backend PR CI | STABLE | PR #16 已合并并实际通过 compile + test | #6 required check |
| 仓库工程治理 | IN_PROGRESS | PR #5 + #16 + #23 | #6/#7/#9/#22 |

---

## 5. Workbench / OA 工作台

### 当前状态

`IN_PROGRESS / Shell Ready, OA Domains Planned`

正式设计：

- `doc/features/workbench/README.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval。

前置依赖：

```text
#12 Tenant Migration Epic
#13 Workbench Platform Foundation
#14 Structured OA Card / ClientEvent Protocol
```

#12 当前：

```text
#18 Migration Architecture (implemented / PR #23)
       ↓
#19 Migration Runtime (IN_PROGRESS)
      ├─────────────┐
      ↓             ↓
#20 New Tenant   #21 Existing Tenant
 Provisioning       Baseline/Validate
      └──────┬──────┘
             ↓
      #12 Epic Complete
             ↓
      #13 Workbench Foundation
             ↓
      Overview / Task Backend
```

#14 可并行，但必须在 Task/Approval Card / Push 实现前完成。

---

## 6. Tenant Versioned Migration

### 正式架构

- `doc/architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md`
- `doc/architecture/tenant-migration/README.md`

历史输入保留在 `doc/部署运维/`，但不再与 ADR-0004 并列作为当前真相。

### ADR-0004 核心边界

1. Flyway 管版本化 SQL、checksum、ordering 和每 schema history；
2. `<tenant>.flyway_schema_history` 是单 tenant migration 权威事实；
3. public control plane 只保存 run/item/state/audit 投影；
4. 普通应用启动时不无条件 migrate 全部 tenant；
5. Runtime 显式选择 schema，不依赖 HTTP TenantContext；
6. 同 tenant 使用 PostgreSQL advisory lock；
7. Safe Sync 不创建 Workbench 新表、不伪造 history；
8. legacy public clone 按 #19 → #20 → #21 退役；
9. 旧 tenant 禁止 blind `baselineOnMigrate(true)`；
10. `ddl-auto=update -> validate` 在 tenant coverage 完成后 staged rollout。

### Issue #19 — Migration Runtime 当前实现范围

当前分支：`feature/19-tenant-migration-runtime`

正在实现：

- 启用 `flyway-core` + PostgreSQL Flyway driver；
- `spring.flyway.enabled=false`，禁止 Boot 自动迁移 public；
- `db/migration/public` 与 `db/migration/tenant`；
- public 显式 bootstrap；
- `schema_migration_run` / `schema_migration_run_item` / `tenant_schema_state`；
- `TenantFlywayFactory`；
- `TenantSchemaInspector`；
- PostgreSQL advisory lock；
- PLAN / APPLY / retry；
- bounded worker pool；
- `/api/admin/schema-migrations/**` 管理 API；
- `MigrationAdminAuthorizer` 集中兼容当前 configured admin；
- Testcontainers PostgreSQL 集成测试。

明确不在 #19 做：

- 不修改 `CompanyService` 新租户生命周期；
- 不 baseline 现有老 tenant；
- 不创建 Workbench 业务表；
- 不切 `ddl-auto=validate`；
- 不删除 Safe Sync / legacy sync API。

### #19 安全行为

- public 默认公司不会成为 tenant migration target；
- PLAN 是读操作，不建立 tenant history/table；
- 空 tenant 可 APPLY；
- 非空且没有 Flyway history 的老 tenant 标记 blocked，APPLY 返回失败，等待 #21；
- 同 tenant 并发 APPLY 使用 advisory lock 拒绝第二个执行者；
- 一个 tenant 失败不回滚其他已成功 tenant，run 可进入 `PARTIAL_FAILED`；
- public bootstrap 是显式管理员 API，不在启动时自动执行。

---

## 7. Backend CI 与测试

### 状态：IMPLEMENTED

Backend PR Validation：

```text
JDK 21
→ mvn -B -ntp -pl server -am -DskipTests compile
→ mvn -B -ntp -pl server -am test
```

#19 新增 Testcontainers PostgreSQL 集成测试，目标验证：

- public bootstrap 可重复；
- PLAN read-only；
- company A APPLY 不改 company B；
- repeat APPLY 幂等；
- legacy tenant 无 history 被拒绝；
- advisory lock 生效。

CI 结果未绿前 #19 不允许合并。

#22 仍跟踪 Maven duplicate dependency 技术债，不与 #19 混合处理。

---

## 8. AI / Automation 与 Workbench

AI/Automation 可以提议或触发 Task/Approval，但不能绕过 Workbench Domain Service 直接写 OA 表。

现有 `BOT_CARD` 是机器人动作卡片；OA 卡片协议由 #14 单独决定。

---

## 9. 通知与结构化卡片

当前 `ClientEventType` 只有 Chat/Meeting；Workbench OA 事件尚未实现。

#14 将决定 Workbench/Structured Card、版本、Push、Deep Link、敏感数据和老客户端兼容策略。

---

## 10. 文档体系

项目级：

- `doc/PROJECT_MASTER.md`
- `CONTRIBUTING.md`
- `doc/development/REPOSITORY_GOVERNANCE.md`

架构级：

- ADR-0001：master single trunk；
- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval；
- ADR-0004：Versioned Tenant Schema Migrations。

文档冲突优先级：

```text
PROJECT_MASTER current facts
  -> Accepted ADR
  -> Canonical feature/architecture design
  -> merged implementation
  -> historical design docs
```

---

## 11. PR Definition of Done

- [ ] 关联 Issue；
- [ ] 范围单一且可验收；
- [ ] 代码/配置/文档完成；
- [ ] 自动化测试或明确人工验证；
- [ ] `PROJECT_MASTER.md` 同步；
- [ ] Feature/Architecture Design 同步；
- [ ] 重大架构有 ADR；
- [ ] DB/API/Protocol 兼容性说明；
- [ ] CI 通过；
- [ ] Review conversation 解决；
- [ ] 遗留工作建 Follow-up Issue。

---

## 12. 当前重点 Roadmap

### P0 — Repository / Build Governance

- #6 master protection；
- #7 remove `main` deploy trigger；
- #8 Backend PR CI：IMPLEMENTED / PR #16；
- #9 Electron/Web PR CI；
- #17 HealthCheck isolation：IMPLEMENTED / PR #16；
- #22 duplicate Maven dependencies。

### P1 — Workbench Foundation

- #10 Formal Feature Design：IMPLEMENTED / PR #11；
- #12 Tenant Migration Epic：IN_PROGRESS；
  - #18 Architecture：IMPLEMENTED / PR #23；
  - #19 Runtime：IN_PROGRESS；
  - #20 New Tenant Provisioning；
  - #21 Existing Tenant Baseline/Validate；
- #13 Workbench Platform Foundation；
- #14 Structured OA Card / ClientEvent Protocol。

### P1 — Workbench Business

Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 — OA Expansion

Calendar → Announcement → Android OA → Report / AI Office。

---

## 13. 当前主要风险

1. #6 未完成前，GitHub 尚未强制阻止 direct push `master`；
2. #9 未完成前，Electron/Web 缺独立 PR build gate；
3. #19 未合并前，版本化 Migration Runtime 仍不可用；
4. #20 未完成前，新 tenant 仍依赖 public clone；
5. #21 未完成前，老 tenant 不能安全进入 Flyway history；
6. #22 Maven duplicate dependencies 尚未清理；
7. `ClientEventType` 尚无 OA 事件，`BOT_CARD` 不能直接当 OA 卡片；
8. Workbench 权限不能继续扩散 `username == admin`；
9. company switch 必须清理 Workbench client state；
10. 附件下载必须增加业务资源授权。

---

## 14. 变更记录

### 2026-08-19 — Issue #19 — Tenant Migration Runtime

状态：`IN_PROGRESS / CI PENDING`

- 建立 Flyway public/tenant resource；
- 建立显式 public bootstrap，不在应用启动时自动 migration；
- 建立 run/item/state public control plane；
- 建立 tenant PLAN/APPLY/retry；
- 建立老 tenant baseline 阻断；
- 建立 PostgreSQL advisory lock；
- 建立 bounded worker；
- 建立 admin API 与集中授权桥接；
- 建立 PostgreSQL Testcontainers 集成测试；
- 等待 Backend/Governance/KMP CI 验证。

### 2026-08-19 — Issue #18 / PR #23 — Tenant Migration Architecture

状态：`IMPLEMENTED`

- ADR-0004 Accepted；
- canonical migration design 建立；
- 历史 migration 文档降级为 Historical Design Input。

### 2026-08-19 — Issue #8 / #17 / PR #16 — Backend PR Validation

状态：`IMPLEMENTED`

- Java 21 Maven compile/test 门禁已落地；
- HealthCheckTest 隔离外部基础设施并通过 CI；
- 发现 #22 Maven duplicate dependency 技术债。

### 2026-08-19 — Issue #10 / PR #11 — Workbench Formal Feature Design

状态：`IMPLEMENTED`

- 建立 Workbench/Task/Approval/Platform Integration/Roadmap；
- 增加 ADR-0002、ADR-0003；
- 建立 #12/#13/#14 前置工作。

### 2026-08-19 — Issue #4 / PR #5 — Repository Governance

状态：`IMPLEMENTED / FOLLOW-UPS OPEN`

- 固定 `master` 唯一主线；
- 建立 Issue-driven PR、PROJECT_MASTER、模板、Governance CI、ADR。
