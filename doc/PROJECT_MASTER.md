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

PR #5 已建立：

- Issue-driven PR；
- `PROJECT_MASTER.md`；
- `CONTRIBUTING.md`；
- PR/Issue Templates；
- Governance CI；
- ADR 机制。

后续治理：

- #6：保护 `master` 并设置 required checks；
- #7：部署 workflow 移除历史 `main` trigger；
- #8：补齐 Backend PR CI，当前实现分支为 `ci/8-backend-pr-validation`；
- #9：补齐 Electron/Web PR CI。

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

因此 Workbench 的新业务表必须走正式版本化 tenant migration。该前置工作由 **Issue #12** 跟踪。

---

## 4. 模块状态地图

状态：`STABLE` / `IN_PROGRESS` / `PLANNED` / `LEGACY` / `BLOCKED`。

| 模块 | 状态 | 当前事实 | 下一阶段 |
| --- | --- | --- | --- |
| 登录 / 鉴权 | STABLE | JWT/Spring Security 主链路存在 | RBAC 细化 |
| 多公司 / 多租户 | IN_PROGRESS | Schema 多租户 + Safe Sync | #12 正式版本化 migration |
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
| 数据库迁移治理 | IN_PROGRESS | Safe Sync/冲突检测存在 | #12 |
| Backend PR CI | IN_PROGRESS | #8 正在新增 Java 21 Maven compile/test 门禁 | 验证后设为 required check |
| 仓库工程治理 | IN_PROGRESS | PR #5 已建立治理基线 | #6/#7/#8/#9 |

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

### 已建立的实现前置 Issue

- **#12** `architecture(database): establish versioned tenant migration path for new business tables`
  - 解决 tenant 新表、版本、执行、漂移和审计；
  - 阻塞 Task/Approval 新业务表落库。
- **#13** `feat(workbench): establish OA platform foundation`
  - 建立 `workbench/common`、CurrentWorkContext、Permission、Audit、Organization/File Adapter、Job tenant execution；
  - 依赖 #12 的迁移路径明确。
- **#14** `architecture(workbench): define structured OA card and client event protocol`
  - 固定 Workbench Card、ClientEvent、Push、Deep Link、兼容策略；
  - 不直接把现有机器人 `BOT_CARD` 泛化为 OA；
  - 阻塞 Task/Approval Card 通知，但不阻塞纯 Backend。

依赖关系：

```text
#10 Formal Design (merged)
 ├── #12 Versioned Tenant Migration
 ├── #13 Workbench Platform Foundation
 └── #14 Structured OA Card / ClientEvent Protocol

#12 + #13
   ↓
Overview / Task Backend

Task Backend + #14
   ↓
Task Realtime / Push / Card
```

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

### 当前状态：IN_PROGRESS

当前安全同步用于 drift 检测与保守修复，不是完整新表 migration engine。

**Issue #12 是 Workbench 新表的正式前置。**

目标：

- public/global 与 tenant migration 有明确版本；
- 新 tenant 自动迁移到 target version；
- 老 tenant 可批量迁移；
- migration history 可审计；
- drift/conflict 在高风险 DDL 前阻断；
- 后台任务显式切换 tenant，不依赖 HTTP Filter；
- 完成 baseline 后逐步从 `ddl-auto=update` 转向 `validate`。

Workbench 实现不得创建与仓库迁移治理平行的第二套 migration runner。

---

## 8. 通知与结构化卡片

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

## 9. 文档体系

### 项目级

- `doc/PROJECT_MASTER.md`：唯一项目事实与状态入口；
- `CONTRIBUTING.md`：贡献执行规则；
- `doc/development/REPOSITORY_GOVERNANCE.md`：治理细则。

### 架构级

`doc/architecture/adr/`

- ADR-0001：master single trunk；
- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval。

重大长期决策必须新增 ADR，不能只留在 PR 评论。

### 功能级

```text
doc/features/<feature>/
```

Workbench 是第一套按新规范正式整理的 Feature Design。历史文档按“触达即整理”渐进迁移，不一次性大规模移动。

---

## 10. PR Definition of Done

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

## 11. 当前重点 Roadmap

### P0 — Repository Governance

- #6 master protection；
- #7 remove `main` deploy trigger；
- #8 backend PR CI（IN_PROGRESS）；
- #9 Electron/Web PR CI。

### P1 — Workbench Design / Foundation

- #10 正式 Feature Design（IMPLEMENTED / PR #11）；
- #12 Versioned Tenant Migration；
- #13 Workbench Platform Foundation；
- #14 Structured OA Card / ClientEvent Protocol。

### P1 — Workbench Business

按 `doc/features/workbench/implementation-roadmap.md`：

1. Overview；
2. Task Backend；
3. Task Web/Electron；
4. Task Realtime/Push/Card；
5. Approval Backend；
6. Approval Web/Electron。

### P2 — OA Expansion

- Calendar；
- Announcement；
- Android OA 完整对齐；
- Report / AI Office。

---

## 12. 当前主要风险

1. #6 未完成前，`master` 仍存在直接 push 风险；
2. #8 正在建立 Backend PR CI，#9 Electron/Web PR CI 尚未开始；
3. #12 未完成前，Workbench 新业务表没有正式可执行的统一迁移路径；
4. 当前 `ClientEventType` 尚无 OA 事件；
5. `BOT_CARD` 是机器人语义，#14 未完成前不能随意复用为 OA 卡片；
6. Workbench 新权限不能延续 `username == admin`；
7. company switch 必须清理 Workbench client state；
8. 附件下载必须增加业务资源授权；
9. Java/TypeScript/Kotlin DTO 长期存在漂移风险，接口稳定后应评估 OpenAPI 代码生成。

---

## 13. 变更记录

### 2026-08-19 — Issue #8 — Backend PR Validation

状态：`IN_PROGRESS`

- 新增独立 Backend PR workflow；
- Java 21 / Maven cache；
- 对 `pom.xml`、`common/**`、`entity/**`、`server/**` 相关 PR 执行；
- 先编译 `server` 及依赖模块，再执行后端测试；
- CI 验证通过后应作为 #6 Branch Protection 的 required check 候选。

### 2026-08-19 — Issue #10 / PR #11 — Workbench Formal Feature Design

状态：`IMPLEMENTED`

- 完整重新审阅 2026-08-06 v0.1 Draft；
- 设计基线已通过 PR #11 合入 `master`；
- Web/Electron 和 Android Workbench Shell 改为“已实现事实”；
- 建立 Workbench Feature Design、Task、Approval、Platform Integration、Roadmap；
- 增加 ADR-0002、ADR-0003；
- 明确 Safe Sync 不负责创建 Workbench 新表；
- 明确现有 `BOT_CARD` 不直接作为 OA 默认卡片协议；
- 建立 #12 tenant migration、#13 platform foundation、#14 card/event protocol 三个实现前置 Issue；
- 后续从旧 PR-0~PR-10 计划转换为真实 Issue 驱动实施。

### 2026-08-19 — Issue #4 / PR #5 — Repository Governance

状态：`IMPLEMENTED / FOLLOW-UPS OPEN`

- 固定 `master` 为唯一开发主线；
- 建立 Issue-driven PR；
- 建立 `PROJECT_MASTER.md`；
- 新增 PR/Issue 模板、Governance CI、ADR 机制；
- 后续：#6、#7、#8、#9。
