# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文档描述 Group-IM 当前是什么、已经实现什么、正在实现什么、接下来做什么。所有代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 Pull Request 中同步更新本文档。

- 文档状态：ACTIVE
- 基线日期：2026-08-19
- 唯一开发主线：`master`
- 当前基线提交：`c0a61180e63af437042b81ce65d49da599fe04cb`
- 仓库：`pengpengno/Group-IM`
- 治理规则：`doc/development/REPOSITORY_GOVERNANCE.md`
- 贡献指南：`CONTRIBUTING.md`

---

## 1. 项目目标

Group-IM 是面向组织协作的多端 IM 与办公平台。即时通信是协作主链路，工作台承载结构化办公能力，AI 与自动化嵌入会话和工作流程。

长期原则：

1. 消息是协作主链路；
2. Workbench 承载 Task / Approval / Schedule / Announcement / Report；
3. AI/Automation 不建立与业务服务冲突的第二份真相；
4. Web/Electron 与 Android 保持核心业务语义一致；
5. 多租户、权限、审计、迁移、文件和通知作为平台能力复用。

---

## 2. 仓库治理基线

### 唯一主线

- `master` 是唯一开发主线和 PR 合并目标；
- `main` 是历史遗留分支，与当前 `master` 不属于同一提交历史，不参与开发和发布；
- 所有代码、配置、数据库和文档变更必须通过 PR；
- Bug 必须先有 Issue；新功能、架构、数据库和重要重构原则上也必须先有 Issue；
- 每个 PR 必须关联 Issue，并同步更新本文件；
- 默认使用 Squash Merge。

### 当前治理状态

PR #5 已建立 Issue-driven PR、`PROJECT_MASTER.md`、贡献规范、PR/Issue 模板、Governance CI 和 ADR 机制。

当前治理工作：

- #6：保护 `master` 并设置 required checks；
- #7：部署 workflow 移除历史 `main` trigger；
- #8：Backend PR CI，PR #16 正在自验证；
- #9：Electron/Web PR CI；
- #22：清理 `server/pom.xml` 重复 dependency 声明。

当前 `master` Branch Protection 仍未启用，因此 #6 为 P0。

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

### 多租户与数据库

请求通过当前登录用户/公司上下文选择 tenant schema。

当前 `SafeTenantSchemaSyncService`：

- 可检测 `SYNCED / OUTDATED / CONFLICT / ERROR`；
- 只自动补安全的 nullable、无 default 缺失字段；
- 不删除或修改已有字段；
- **不自动创建缺失表**。

`CompanyService.save()` 当前仍调用 `public.create_or_sync_company_schema(schemaName, companyId)`，从 `public` 现有表结构初始化新 tenant。该路径属于待退役的兼容机制，不再作为未来数据库版本真相。

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
| Workbench | IN_PROGRESS | Web/Electron + Android Shell 已存在，正式 Feature Design 已合并 | #12/#13 后进入 Overview |
| OA Task | PLANNED | 正式领域设计已形成 | #12 + #13 后进入 Backend |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 平台闭环后实现 |
| OA Calendar | PLANNED | Meeting/Task 聚合方向已形成 | 后续迭代 |
| OA Announcement | PLANNED | Target/Receipt/通知方向已形成 | 后续迭代 |
| OA Report | PLANNED | Roadmap 能力 | Workbench V1 后评估 |
| 数据库迁移治理 | IN_PROGRESS | #12 已拆为 #18/#19/#20/#21 | 先 #18，再 #19 |
| Backend PR CI | IN_PROGRESS | PR #16；compile 已通过，测试隔离修复 #17 正在自验证 | 全绿后合并并纳入 #6 |
| 仓库工程治理 | IN_PROGRESS | PR #5 已建立治理基线 | #6/#7/#8/#9/#22 |

---

## 5. Workbench / OA 工作台

### 当前状态

`IN_PROGRESS / Shell Ready, OA Domains Planned`

### 已实现

- Electron/Web Workbench Shell；
- Android Workbench Shell；
- 会话、会议、通讯录、设置入口；
- Electron 自动化入口。

目前 Workbench 仍主要是“已有协作能力聚合页”，Task/Approval 等结构化 OA 写模型尚未落地。

### 正式 Feature Design

Issue #10 / PR #11 已完成并合入 `master`：

- `doc/features/workbench/README.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- ADR-0002：Workbench 模块化单体 + tenant domains；
- ADR-0003：Task-first + lightweight Approval。

### 实现前置

- #12：Tenant Versioned Migration Epic；
- #13：Workbench Platform Foundation；
- #14：Structured OA Card / ClientEvent Protocol。

#12 已正式拆解：

```text
#8 Backend CI
       ↓
#18 Migration ADR
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

#14 可与 #12 并行，但必须在 Task/Approval 卡片与 Push 实现前完成。

### Workbench V1 方向

1. Overview / Todo；
2. Task；
3. Approval；
4. Calendar / Meeting aggregation；
5. Announcement；
6. Android OA 核心能力；
7. Report / AI Office 后置。

### 实施原则

- 模块化单体 + 垂直业务包；
- OA 表进入 tenant schema；
- Task 优先于 Approval；
- Approval V1 不做复杂 BPM；
- Overview 是读聚合，不复制领域写模型；
- 状态由服务端状态机控制；
- 核心动作可审计；
- DB 提交成功后再发布外部通知；
- 文件、组织、会议、消息、Push 全部复用已有平台；
- Web/Electron 先形成完整主流程，Android 分阶段补齐；
- 新表必须走 #12 的正式 migration；
- 新 Workbench 权限禁止延续 `username == admin`。

---

## 6. AI 与自动化

### 当前状态：IN_PROGRESS

方向：服务端机器人身份、持久化会话、自动化执行、工具治理和 Human-in-the-loop。

与 Workbench 的集成原则：

```text
AI / Automation
→ proposal / trigger
→ permission + optional human confirmation
→ Workbench Domain Service
→ Task / Approval
```

AI/Automation 不能绕过 Workbench Service 直接写 OA 表。

现有 `BOT_CARD` 明确属于机器人动作卡片；OA 结构化卡片协议由 #14 单独决定。

---

## 7. 数据库与多租户迁移

### 当前状态：IN_PROGRESS / EPIC DECOMPOSED

当前 Safe Sync 用于 drift 检测和保守字段修复，不是完整的新表 migration engine。旧 `create_or_sync_company_schema` 仍服务于当前新公司初始化和人工同步，但将进入兼容/退役路径。

#12 的阶段：

- **#18 Migration ADR**：固定 Flyway history、control plane、Safe Sync、baseline、provisioning 和 `ddl-auto` 边界；
- **#19 Migration Runtime**：Flyway tenant executor、PLAN/APPLY、advisory lock、run/state、管理 API、多 tenant 测试；
- **#20 New Tenant Provisioning**：`create schema -> migrate -> activate`，停止以 public clone 为新租户主路径；
- **#21 Existing Tenant Baseline/Validate**：只读 preflight、显式 baseline、drift repair plan、staged `ddl-auto=validate`。

禁止：

- `baselineOnMigrate(true)` 盲目标记未知 tenant；
- 启动时无条件迁移全部 tenant；
- Workbench 自建第二套 migration runner；
- 在缺少 tenant coverage 证据时直接把生产 `ddl-auto=update` 切成 `validate`。

---

## 8. Backend CI 与测试状态

### Issue #8 / PR #16

目标：对后端相关 PR 强制执行 Java 21 Maven compile + test。

Workflow：

```text
Backend PR Validation
  → checkout
  → JDK 21
  → mvn -pl server -am -DskipTests compile
  → mvn -pl server -am test
```

当前自验证结果：

- 后端 4-module reactor compile 已成功；
- 首次 test 暴露 #17：`HealthCheckTest` 在禁用 JPA Repository 后仍加载完整业务组件图；
- #17 已改为独立最小 Actuator 测试应用，不扫描 Group-IM Service/Repository/AI；
- 第二次 test 进一步发现 LDAP health contributor 尝试连接 `localhost:389`；测试上下文现已明确排除 LDAP 自动配置；
- Workflow 已升级为 `actions/checkout@v5` / `actions/setup-java@v5`；
- 最新一轮 Backend CI 正在自验证。

### Issue #22

Maven 当前报告 `server/pom.xml` 有重复 dependency：Redis starter、Lettuce、WebSocket starter。它们不阻塞本次编译，但已经单独建 Issue #22，禁止顺手混入 PR #16。

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

协议选择由 #14 决定：独立 `WORKBENCH` MessageType，或经 ADR 泛化为更通用的结构化业务卡片。

必须满足：

- 不破坏 `BOT_CARD` 机器人语义；
- payload 版本化；
- Web/Electron/Android 兼容；
- Push 不包含不必要的敏感审批数据；
- Deep Link 仍需服务端 tenant/resource 权限校验；
- 通知在业务事务提交后执行。

---

## 10. 文档体系

### 项目级

- `doc/PROJECT_MASTER.md`：唯一项目事实与状态入口；
- `CONTRIBUTING.md`：贡献执行规则；
- `doc/development/REPOSITORY_GOVERNANCE.md`：治理细则。

### 架构级

`doc/architecture/adr/`

- ADR-0001：master single trunk；
- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval；
- ADR-0004：预留给 #18 Tenant Migration Runtime Contract（尚未合并）。

### 功能级

```text
doc/features/<feature>/
```

Workbench 是第一套按新规范整理的 Feature Design。历史文档按“触达即整理”渐进迁移。

---

## 11. PR Definition of Done

每个 PR 至少满足：

- [ ] 已关联 Issue；
- [ ] 范围单一、可验收；
- [ ] 实现/配置/文档完成；
- [ ] 有自动化测试或明确人工验证；
- [ ] `PROJECT_MASTER.md` 已同步；
- [ ] 相关 Feature Design 已同步；
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
- #8 backend PR CI（PR #16，IN_PROGRESS）；
- #9 Electron/Web PR CI；
- #17 HealthCheck test isolation（随 PR #16 修复）；
- #22 duplicate Maven dependencies。

### P1 — Workbench Foundation

- #10 Formal Feature Design：IMPLEMENTED / PR #11；
- #12 Tenant Migration Epic：IN_PROGRESS；
  - #18 Migration ADR；
  - #19 Migration Runtime；
  - #20 New Tenant Provisioning；
  - #21 Existing Tenant Baseline/Validate；
- #13 Workbench Platform Foundation；
- #14 Structured OA Card / ClientEvent Protocol。

### P1 — Workbench Business

按 `doc/features/workbench/implementation-roadmap.md`：Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 — OA Expansion

Calendar → Announcement → Android OA 完整对齐 → Report / AI Office。

---

## 13. 当前主要风险

1. #6 未完成前，`master` 仍存在直接 push 风险；
2. #8 未合并前，后端 PR 尚无正式 required compile/test 门禁；
3. #12 未完成前，Workbench 新业务表没有统一可执行 migration 路径；
4. 当前新租户仍依赖 public schema clone；
5. #22 表明 Maven model 有重复 dependency 技术债；
6. `ClientEventType` 尚无 OA 事件，且 `BOT_CARD` 不能直接当 OA 卡片；
7. Workbench 新权限不能延续 `username == admin`；
8. company switch 必须清理 Workbench client state；
9. 附件下载必须增加业务资源授权；
10. Java/TypeScript/Kotlin DTO 长期存在漂移风险。

---

## 14. 变更记录

### 2026-08-19 — Issue #8 / #17 — Backend PR Validation

状态：`IN_PROGRESS / SELF-VALIDATING`

- PR #16 新增独立 Backend PR workflow；
- Java 21 / Maven compile + test；
- 编译已在 GitHub Actions 实际成功；
- CI 暴露并跟踪 #17 HealthCheckTest 隔离问题；
- HealthCheckTest 已改为最小 Actuator 测试上下文并排除 DB/JPA/Redis/LDAP/Security 外部依赖；
- Actions 升级到 Node 24 兼容 v5；
- 额外发现 Maven 重复依赖并创建 #22；
- 全绿后 PR #16 才允许合并，并成为 #6 required-check 候选。

### 2026-08-19 — Issue #12 — Tenant Migration Epic

状态：`IN_PROGRESS / DECOMPOSED`

- 重新核对两份历史 migration 设计与当前 CompanyService/Safe Sync；
- #12 从巨型实现项拆为 #18 ADR、#19 Runtime、#20 New Tenant、#21 Existing Tenant；
- 固定“不启动时全量迁移、不盲目 baseline、不直接切 validate”的阶段原则。

### 2026-08-19 — Issue #10 / PR #11 — Workbench Formal Feature Design

状态：`IMPLEMENTED`

- 完整重新审阅 2026-08-06 v0.1 Draft；
- 建立 Workbench Feature Design、Task、Approval、Platform Integration、Roadmap；
- 增加 ADR-0002、ADR-0003；
- 建立 #12/#13/#14 前置工作；
- PR #11 已合入 `master`。

### 2026-08-19 — Issue #4 / PR #5 — Repository Governance

状态：`IMPLEMENTED / FOLLOW-UPS OPEN`

- 固定 `master` 为唯一开发主线；
- 建立 Issue-driven PR、PROJECT_MASTER、PR/Issue 模板、Governance CI、ADR；
- 后续：#6、#7、#8、#9。
