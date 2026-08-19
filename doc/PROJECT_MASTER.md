# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文档描述 Group-IM 当前是什么、已经实现什么、正在实现什么、接下来做什么。所有代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 Pull Request 中同步更新本文档。

- 文档状态：ACTIVE
- 基线日期：2026-08-19
- 唯一开发主线：`master`
- 当前基线提交：`297eb89582d950db6d64a1ae074aa5c37c26d399`
- 当前变更：Issue #19 / PR #24，最终 CI 已通过一轮，最后依赖文档同步后重新验证
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
- legacy public clone 会复制业务 tables、global identity views、foreign keys、indexes 和 CHECK constraints；
- ADR-0004 已固定 Flyway 版本化 migration 为未来唯一新表发布路径；
- PR #24 已实现第一版 Migration Runtime，并通过真实 PostgreSQL Testcontainers CI。

---

## 4. 模块状态地图

状态：`STABLE` / `IN_PROGRESS` / `PLANNED` / `LEGACY` / `BLOCKED`。

| 模块 | 状态 | 当前事实 | 下一阶段 |
| --- | --- | --- | --- |
| 登录 / 鉴权 | STABLE | JWT/Spring Security 主链路存在 | RBAC 细化 |
| 多公司 / 多租户 | IN_PROGRESS | Schema 多租户 + Safe Sync + legacy public clone | #26 → #25 → #20/#21 |
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
| Tenant Migration Runtime | READY | PR #24 已通过 Governance / Backend / KMP 一轮；最终文档提交后复验 | 合并后 #26 |
| Core Tenant Schema Baseline | PLANNED | #25；不能仅靠 JPA Entity 反推完整结构 | 先 #26 trusted snapshot |
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

#12 当前依赖图：

```text
#8 Backend CI ✅
       ↓
#18 Migration Architecture ✅
       ↓
#19 Migration Runtime ✅ CI / PR #24 ready
       ↓
#26 Read-only Trusted Tenant Schema Snapshot
       ↓
#25 Core Tenant Baseline Migrations
      ├────────────────────┐
      ↓                    ↓
#20 New Tenant          #21 Existing Tenant
 Provisioning              Baseline/Validate
      └─────────┬──────────┘
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
8. legacy public clone 在可信 core baseline + provisioning 接管后退役；
9. 旧 tenant 禁止 blind `baselineOnMigrate(true)`；
10. `ddl-auto=update -> validate` 在 tenant coverage 完成后 staged rollout。

### Issue #19 — Migration Runtime

PR #24 已实现并完成一轮 CI 验证：

- `flyway-core` + PostgreSQL Flyway driver；
- `spring.flyway.enabled=false`；
- public / tenant migration resource；
- public 显式 bootstrap；
- run/item/state control plane；
- schema validator / inspector；
- PostgreSQL advisory lock；
- PLAN / APPLY / failed-tenant retry；
- bounded worker；
- `/api/admin/schema-migrations/**`；
- 集中的 `MigrationAdminAuthorizer`；
- PostgreSQL Testcontainers 集成测试。

### #19 安全行为

- public 默认公司不会成为 tenant migration target；
- PLAN 只读，不创建 tenant Flyway history 和业务 metadata；
- 空 tenant 可 APPLY；
- 非空且没有 Flyway history 的老 tenant 被 blocked，APPLY 拒绝；
- 同 tenant 并发 APPLY 使用 advisory lock；
- 一个 tenant 失败不回滚其他 tenant；
- APPLY 记录真实 pre-apply `from_version` 与最终 `target_version`；
- public bootstrap 是显式管理员 API，不在启动时自动执行。

### Issue #26 — Trusted Tenant Schema Snapshot / Inventory

只读工具，用于从可信健康 tenant 产生：

- tables/views inventory；
- columns/types/nullability/defaults；
- PK/UK/FK/CHECK；
- indexes；
- sequences/identity；
- view definitions；
- schema-only SQL reference；
- source schema / timestamp / fingerprint。

为什么必须有 #26：当前 JPA Entity 并不完整表达 legacy provisioning 中的 views、数据库 defaults、约束和索引；仅靠 Entity 生成 baseline 存在静默漏结构风险。

输出只作为 #25 的**审阅输入**，不会未经 normalize/review 自动执行。

### Issue #25 — Core Tenant Baseline Migrations

依赖 #19 + #26。

目标：

> 从真正空 schema 开始，只通过 immutable versioned tenant migrations，得到现有 Group-IM 核心业务要求的完整 tenant schema。

#25 完成前，#20 不得移除 legacy public clone；#21 也没有可信 expected baseline version/structure。

---

## 7. Backend CI 与测试

### Backend PR Validation：STABLE

```text
JDK 21
→ mvn -B -ntp -pl server -am -DskipTests compile
→ mvn -B -ntp -pl server -am test
```

### PR #24 已通过的一轮验证

- Repository Governance：SUCCESS；
- Backend Maven compile：SUCCESS；
- Backend Maven test：SUCCESS；
- PostgreSQL Testcontainers：SUCCESS；
- KMP APK：SUCCESS。

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

最后一次依赖文档更新会再次跑同一门禁；全部绿色后才合并。

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
  - #19 Runtime：READY / PR #24；
  - #26 Trusted Tenant Schema Snapshot：NEXT；
  - #25 Core Tenant Baseline Migrations：depends #26；
  - #20 New Tenant Provisioning：depends #25；
  - #21 Existing Tenant Baseline/Validate：depends #25；
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
3. #26/#25 未完成前，无法证明空 tenant 仅靠 migrations 可以得到完整现有 Group-IM schema；
4. #20 未完成前，新 tenant 仍依赖 public clone；
5. #21 未完成前，老 tenant 不能安全进入统一 Flyway baseline；
6. #22 Maven duplicate dependencies 尚未清理；
7. `ClientEventType` 尚无 OA 事件，`BOT_CARD` 不能直接当 OA 卡片；
8. MigrationAdminAuthorizer 仍是 configured-admin 兼容桥接，未来需真正 SYSTEM_ADMIN/RBAC；
9. company switch 必须清理 Workbench client state；
10. 附件下载必须增加业务资源授权。

---

## 14. 变更记录

### 2026-08-19 — Issue #19 / PR #24 — Tenant Migration Runtime

状态：`READY TO MERGE / FINAL CI RUNNING`

- 建立 Flyway public/tenant runtime；
- 建立显式 public bootstrap；
- 建立 run/item/state control plane；
- 建立 PLAN/APPLY/retry；
- 建立 non-empty/no-history legacy tenant baseline 阻断；
- 建立 PostgreSQL advisory lock；
- 建立 bounded worker 和 admin API；
- 建立真实 PostgreSQL Testcontainers multi-tenant integration test；
- 已通过完整 CI 一轮；
- 发现正式 provisioning 前还需要 #26 trusted schema snapshot 与 #25 core tenant baseline。

### 2026-08-19 — Issue #26 / #25 — Core Tenant Baseline Preparation

状态：`PLANNED / NEXT AFTER #19`

- #26：只读导出可信 tenant schema inventory/schema-only snapshot；
- #25：将该结构固化为 immutable Flyway core tenant baseline；
- #20/#21 均依赖 #25。

### 2026-08-19 — Issue #18 / PR #23 — Tenant Migration Architecture

状态：`IMPLEMENTED`

- ADR-0004 Accepted；
- canonical migration design 建立；
- 历史 migration 文档降级为 Historical Design Input。

### 2026-08-19 — Issue #8 / #17 / PR #16 — Backend PR Validation

状态：`IMPLEMENTED`

- Java 21 Maven compile/test 门禁落地；
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
