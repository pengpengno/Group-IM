# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文档描述 Group-IM 当前是什么、已经实现什么、正在实现什么、接下来做什么。所有代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 Pull Request 中同步更新本文档。

- 文档状态：ACTIVE
- 基线日期：2026-08-19
- 唯一开发主线：`master`
- 当前基线提交：`e533fac3a42cd74e15ee02a66612b3ed52c78635`
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

- PR #5：Issue-driven PR、PROJECT_MASTER、贡献规范、PR/Issue 模板、Governance CI、ADR；
- PR #16：Backend PR Validation，Java 21 Maven compile + test；
- #17：HealthCheckTest 已隔离 DB/JPA/Redis/LDAP/Security 外部依赖并通过 CI。

待完成：

- #6：保护 `master` 并设置 required checks；
- #7：部署 workflow 移除历史 `main` trigger；
- #9：Electron/Web PR CI；
- #22：清理 `server/pom.xml` 重复 dependency 声明。

当前插件未暴露 Branch Protection / Ruleset 写接口，因此 #6 仍需要仓库管理员在 GitHub Settings 完成。

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
- `spring.jpa.hibernate.ddl-auto` 当前仍为 `update`；
- `SafeTenantSchemaSyncService` 可检测 `SYNCED / OUTDATED / CONFLICT / ERROR`，只自动补安全的 nullable、无 default 缺失字段，**不创建缺失整表**；
- `CompanyService.save()` 当前仍调用 `public.create_or_sync_company_schema(...)`，以 public 当前结构初始化新 tenant；
- public clone 是当前兼容路径，不是未来数据库版本真相。

Workbench 新业务表必须走 #12 Tenant Versioned Migration Epic。

---

## 4. 模块状态地图

状态：`STABLE` / `IN_PROGRESS` / `PLANNED` / `LEGACY` / `BLOCKED`。

| 模块 | 状态 | 当前事实 | 下一阶段 |
| --- | --- | --- | --- |
| 登录 / 鉴权 | STABLE | JWT/Spring Security 主链路存在 | RBAC 细化 |
| 多公司 / 多租户 | IN_PROGRESS | Schema 多租户 + Safe Sync + legacy public clone | #12 Migration Epic |
| 单聊 / 群聊 | STABLE | 核心 IM 主链路存在 | 一致性、搜索、治理 |
| 联系人 / 组织 | STABLE | 公司/部门/员工能力存在 | 权限与批量治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级权限与可靠性 |
| 音视频会议 | IN_PROGRESS | Meeting 服务、通知、多端入口存在 | 协作联动和可靠性 |
| AI 助手 | IN_PROGRESS | 服务端 AI/Bot/持久化能力持续演进 | 工具治理和可观测性 |
| 群自动化 | IN_PROGRESS | 规则、执行、管理入口存在 | 审批、审计、规则模型 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；正式 Feature Design 已合并 | #12/#13 后进入 Overview |
| OA Task | PLANNED | 正式领域设计已形成 | #12 + #13 后进入 Backend |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 平台闭环后实现 |
| OA Calendar | PLANNED | Meeting/Task 聚合方向已形成 | 后续迭代 |
| OA Announcement | PLANNED | Target/Receipt/通知方向已形成 | 后续迭代 |
| OA Report | PLANNED | Roadmap 能力 | Workbench V1 后评估 |
| 数据库迁移治理 | IN_PROGRESS | #12 已拆为 #18/#19/#20/#21；ADR-0004 正在 #18 固化 | #19 Runtime |
| Backend PR CI | STABLE | PR #16 已合并，compile + test 已在 Actions 实际通过 | #6 required check |
| 仓库工程治理 | IN_PROGRESS | PR #5 + PR #16 已建立主要门禁 | #6/#7/#9/#22 |

---

## 5. Workbench / OA 工作台

### 当前状态

`IN_PROGRESS / Shell Ready, OA Domains Planned`

### 已实现

- Electron/Web Workbench Shell；
- Android Workbench Shell；
- 会话、会议、通讯录、设置入口；
- Electron 自动化入口。

### 正式 Feature Design

Issue #10 / PR #11 已合并：

- `doc/features/workbench/README.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval。

### 实现前置

```text
#12 Tenant Migration Epic
#13 Workbench Platform Foundation
#14 Structured OA Card / ClientEvent Protocol
```

#12 当前依赖：

```text
#8 Backend CI (implemented)
       ↓
#18 Migration Architecture / ADR-0004
       ↓
#19 Migration Runtime
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

### Workbench V1 方向

Overview / Todo → Task → Approval → Calendar/Meeting → Announcement → Android OA → Report/AI Office。

---

## 6. Tenant Versioned Migration

### 当前状态：IN_PROGRESS / ARCHITECTURE FREEZING

正式架构入口：

- `doc/architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md`
- `doc/architecture/tenant-migration/README.md`

历史输入：

- `doc/部署运维/租户Schema版本与迁移机制方案.md`
- `doc/部署运维/租户Schema迁移代码设计.md`

上述历史文档保留参考价值，但不再与 ADR-0004 并列作为当前真相。

### ADR-0004 固定的核心边界

1. Flyway 负责版本化 SQL、checksum、ordering 和每 schema history；
2. `<tenant>.flyway_schema_history` 是单 tenant migration 的权威事实；
3. public control plane 保存 run/item/state/audit 投影，不替代 tenant history；
4. 普通应用启动时**不无条件 migrate 全部 tenant**；
5. Migration Runtime 显式选择 public / tenant schema，不依赖 HTTP TenantContext；
6. 同 tenant 使用 PostgreSQL advisory lock；
7. Safe Sync 只负责 drift/保守兼容，不创建 Workbench 新表、不伪造 history；
8. legacy `create_or_sync_company_schema` 按 #19 → #20 → #21 顺序退役；
9. 旧 tenant 禁止 blind `baselineOnMigrate(true)`；
10. `ddl-auto=update -> validate` 必须在 #19/#20/#21 与 tenant coverage 完成后分阶段执行；
11. public/control-plane bootstrap 也是显式 migration operation。

### #12 分阶段 Issues

- #18：Migration Architecture Contract / ADR-0004；
- #19：Migration Runtime；
- #20：New Tenant Provisioning；
- #21：Existing Tenant Baseline / Validate。

Workbench 不得另建一套 migration runner。

---

## 7. Backend CI 与测试

### 状态：IMPLEMENTED

PR #16 已通过并合入 `master`。

Workflow：

```text
Backend PR Validation
  → actions/checkout@v5
  → actions/setup-java@v5 / JDK 21
  → mvn -B -ntp -pl server -am -DskipTests compile
  → mvn -B -ntp -pl server -am test
```

最终 GitHub Actions 验证：

- Maven compile：SUCCESS；
- Maven test：SUCCESS；
- Repository Governance：SUCCESS；
- KMP APK：SUCCESS。

CI 首次运行还发现并修复了 #17 HealthCheckTest 的测试上下文问题。

### 已发现技术债

#22：`server/pom.xml` 重复声明 Redis starter、Lettuce、WebSocket starter。当前不阻塞构建，但需独立 PR 清理。

---

## 8. AI / Automation 与 Workbench

AI/Automation 可以提议或触发 Task/Approval，但不能绕过 Workbench Domain Service 直接写 OA 表。

```text
AI / Automation
→ proposal / trigger
→ permission + optional human confirmation
→ Workbench Domain Service
→ Task / Approval
```

现有 `BOT_CARD` 是机器人动作卡片；OA 卡片协议由 #14 单独决定。

---

## 9. 通知与结构化卡片

当前 `ClientEventType` 只有 Chat/Meeting 事件；Workbench OA 事件尚未实现。

候选事件：

```text
TASK_ASSIGNED
TASK_STATUS_CHANGED
TASK_DUE_SOON
APPROVAL_PENDING
APPROVAL_RESULT
ANNOUNCEMENT_PUBLISHED
SCHEDULE_REMINDER
```

#14 需要决定独立 `WORKBENCH` MessageType 或更通用的结构化业务卡片，并定义版本、Push、Deep Link、敏感数据和老客户端兼容策略。

---

## 10. 文档体系

### 项目级

- `doc/PROJECT_MASTER.md`：项目当前事实唯一入口；
- `CONTRIBUTING.md`；
- `doc/development/REPOSITORY_GOVERNANCE.md`。

### 架构级

- ADR-0001：master single trunk；
- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval；
- ADR-0004：Versioned Tenant Schema Migrations（#18）。

### 功能/专题级

```text
doc/features/<feature>/
doc/architecture/<topic>/
```

部署运维索引：`doc/部署运维/README.md`。

如果文档冲突，优先级：

```text
PROJECT_MASTER current facts
  -> Accepted ADR
  -> Canonical feature/architecture design
  -> merged implementation
  -> historical design docs
```

---

## 11. PR Definition of Done

每个 PR 至少满足：

- [ ] 已关联 Issue；
- [ ] 范围单一、可验收；
- [ ] 实现/配置/文档完成；
- [ ] 有自动化测试或明确人工验证；
- [ ] `PROJECT_MASTER.md` 已同步；
- [ ] 相关 Feature/Architecture Design 已同步；
- [ ] 重大架构决策有 ADR；
- [ ] DB/API/Protocol 兼容性已说明；
- [ ] CI 通过；
- [ ] Review conversation 已解决；
- [ ] 遗留工作已建 Follow-up Issue。

---

## 12. 当前重点 Roadmap

### P0 — Repository / Build Governance

- #6 master protection；
- #7 remove `main` deploy trigger；
- #8 Backend PR CI：IMPLEMENTED / PR #16；
- #9 Electron/Web PR CI；
- #17 HealthCheck test isolation：IMPLEMENTED / PR #16；
- #22 duplicate Maven dependencies。

### P1 — Workbench Foundation

- #10 Formal Feature Design：IMPLEMENTED / PR #11；
- #12 Tenant Migration Epic：IN_PROGRESS；
  - #18 Migration ADR：IN_PROGRESS；
  - #19 Migration Runtime：NEXT；
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

1. #6 未完成前，GitHub 仍未强制阻止 direct push `master`；
2. #9 未完成前，Electron/Web 仍缺独立 PR build gate；
3. #12 未完成前，Workbench 新业务表没有完整可执行 migration 生命周期；
4. 当前新 tenant 仍依赖 public schema clone；
5. #22 Maven duplicate dependencies 尚未清理；
6. `ClientEventType` 尚无 OA 事件，`BOT_CARD` 不能直接当 OA 卡片；
7. Workbench 新权限不能延续 `username == admin`；
8. company switch 必须清理 Workbench client state；
9. 附件下载必须增加业务资源授权；
10. Java/TypeScript/Kotlin DTO 长期存在漂移风险。

---

## 14. 变更记录

### 2026-08-19 — Issue #18 — Tenant Migration Architecture

状态：`IN_PROGRESS`

- 新增 ADR-0004；
- 新增 `doc/architecture/tenant-migration/README.md` 作为当前 canonical design；
- 历史两份 migration 文档降级为 Historical Design Input；
- 固定 Flyway history / public control plane / advisory lock / Safe Sync / baseline / provisioning / staged validate 边界；
- #18 合并后进入 #19 Migration Runtime。

### 2026-08-19 — Issue #8 / #17 / PR #16 — Backend PR Validation

状态：`IMPLEMENTED`

- 新增 Backend PR Validation；
- Java 21 Maven compile + test 已实际通过；
- 修复 HealthCheckTest 对 DB/JPA/Redis/LDAP/Security 外部依赖；
- Workflow 使用 checkout/setup-java v5；
- 发现并建立 #22 Maven duplicate dependency 技术债。

### 2026-08-19 — Issue #12 — Tenant Migration Epic

状态：`IN_PROGRESS / DECOMPOSED`

- #12 拆为 #18 ADR、#19 Runtime、#20 New Tenant、#21 Existing Tenant；
- 固定“不启动时全量迁移、不盲目 baseline、不直接切 validate”的阶段原则。

### 2026-08-19 — Issue #10 / PR #11 — Workbench Formal Feature Design

状态：`IMPLEMENTED`

- 建立 Workbench Feature Design、Task、Approval、Platform Integration、Roadmap；
- 增加 ADR-0002、ADR-0003；
- 建立 #12/#13/#14 前置工作。

### 2026-08-19 — Issue #4 / PR #5 — Repository Governance

状态：`IMPLEMENTED / FOLLOW-UPS OPEN`

- 固定 `master` 唯一开发主线；
- 建立 Issue-driven PR、PROJECT_MASTER、PR/Issue 模板、Governance CI、ADR；
- 后续：#6、#7、#9。
