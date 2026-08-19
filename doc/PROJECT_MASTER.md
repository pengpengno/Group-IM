# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文档描述 Group-IM 当前是什么、已经实现什么、正在实现什么、接下来做什么。所有代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 Pull Request 中同步更新本文档。

- 文档状态：ACTIVE
- 基线日期：2026-08-19
- 唯一开发主线：`master`
- 当前基线提交：`cdd6ffed21b562b912a01bc8e7abd04eeb1ac3b2`
- 仓库：`pengpengno/Group-IM`
- 治理规则：`doc/development/REPOSITORY_GOVERNANCE.md`
- 贡献指南：`CONTRIBUTING.md`

---

## 1. 项目目标

Group-IM 是一个面向组织协作的多端 IM 与办公平台。项目以即时通信为基础能力，逐步整合组织通讯录、音视频会议、AI 助手与自动化、文件能力、多租户管理以及 OA 工作台。

目标不是堆叠孤立功能，而是形成统一的组织工作入口：

1. 消息是协作主链路；
2. 工作台承载结构化办公能力；
3. AI 与自动化嵌入会话和工作流程，而不是形成独立数据孤岛；
4. Web/Electron 与 Android 保持核心业务语义一致；
5. 多租户、权限、审计、迁移和通知作为平台级基础设施复用。

---

## 2. 仓库治理基线

### 2.1 唯一主线

- `master` 是唯一开发主线和合并目标；
- `main` 为历史遗留分支，与当前 `master` 不属于同一提交历史，不再用于开发、发布或作为 PR 基线；
- 禁止直接向 `master` 提交业务、修复、配置和文档变更；所有变更必须通过 PR。

### 2.2 工作项规则

- Bug 修复必须先有 Issue；
- 新功能、架构调整、数据库变更、重要重构原则上必须先有 Issue；
- 每个 PR 必须关联至少一个 Issue；
- 修复类 PR 使用 `Fixes #<issue>` / `Closes #<issue>` / `Resolves #<issue>`；
- 一个 PR 尽量只完成一个可独立验收的目标。

### 2.3 文档规则

每个 PR 必须同步更新本文件，并至少说明：

- 实现/修改了什么；
- 对应模块当前状态；
- 仍未完成的部分；
- 风险、兼容性或迁移影响；
- 后续 Issue / Roadmap。

详细设计放在 `doc/features/`、`doc/architecture/` 等专题文档中；本文档只维护当前事实、状态和索引。

### 2.4 当前治理状态

PR #5 已完成第一版仓库治理并合入 `master`。

后续治理 Issue：

- #6：保护 `master` 并设置 required checks；
- #7：部署 workflow 移除历史 `main` trigger；
- #8：补齐 Backend PR CI；
- #9：补齐 Electron/Web PR CI。

当前 `master` GitHub Branch Protection 仍未启用，因此 #6 是 P0 管理动作。

---

## 3. 技术架构概览

### 后端

- Java 21
- Spring Boot 3.x
- Maven 多模块：`common`、`entity`、`server`
- Spring Security + JWT
- Spring Data JPA
- PostgreSQL
- Redis
- WebSocket / 实时事件
- Spring AI / AI Provider 集成

### 客户端

- Electron / React / TypeScript 桌面与 Web 客户端
- Kotlin Multiplatform + Compose Android 客户端

### 多租户

当前核心方向为 PostgreSQL Schema 级多租户。HTTP 请求通过当前登录用户/公司上下文选择 tenant schema。数据库结构正在从历史的动态同步方式逐步迁移到可版本化、可审计、可检测漂移的迁移机制。

当前 `SafeTenantSchemaSyncService` 可检测 `SYNCED / OUTDATED / CONFLICT / ERROR`，并只自动新增安全的 nullable 无 default 缺失字段。**它不会自动创建缺失表**，因此 Workbench 新业务表必须进入正式版本化 migration。

---

## 4. 模块状态地图

状态定义：

- `STABLE`：核心路径已实现，可作为现有能力使用；
- `IN_PROGRESS`：已存在实现，但仍在持续补齐或治理；
- `PLANNED`：已有明确设计方向，尚未进入完整实现；
- `LEGACY`：保留兼容但不再作为新设计基线；
- `BLOCKED`：存在明确阻塞。

| 模块 | 状态 | 当前事实 | 下一阶段 |
| --- | --- | --- | --- |
| 登录 / 鉴权 | STABLE | JWT/Spring Security 主链路已存在 | 细化 RBAC 与权限模型 |
| 多公司 / 多租户 | IN_PROGRESS | Schema 多租户 + Safe Schema Sync 已存在 | 固定版本化 migration 与 tenant runner |
| 单聊 / 群聊 | STABLE | 核心 IM 主链路已存在 | 持续补充一致性、搜索与治理 |
| 联系人 / 组织 | STABLE | 公司、部门、员工与通讯录能力已存在 | 权限与批量操作治理 |
| 文件 | IN_PROGRESS | 上传、分片等能力存在 | 资源级权限、生命周期与可靠性 |
| 音视频会议 | IN_PROGRESS | Meeting 模型、服务、通知与多端入口存在 | 可靠性、会议协作联动 |
| AI 助手 | IN_PROGRESS | 服务端 AI、Bot 身份与会话能力持续演进 | 权限、工具治理、可观测性 |
| 群自动化 | IN_PROGRESS | 自动化规则、执行、管理入口已存在 | 规则模型、审批和审计完善 |
| 工作台 Workbench | IN_PROGRESS | Web/Electron 与 Android Shell 已存在；正式 Feature Design 建立中 | 平台前置 + Overview |
| OA 任务 Task | PLANNED | 正式领域设计已形成 | 平台前置完成后进入 Backend |
| OA 审批 Approval | PLANNED | 正式轻量串行审批设计已形成 | Task 闭环稳定后实现 |
| OA 日程 Calendar | PLANNED | 与 Meeting/Task 聚合方向已形成 | Task/Approval 后实现 |
| OA 公告 Announcement | PLANNED | Target/Receipt/通知方向已形成 | 后续迭代 |
| OA 工作报告 | PLANNED | Roadmap 能力 | Workbench V1 后评估 |
| 数据库迁移治理 | IN_PROGRESS | Safe Sync/冲突检测已存在 | 正式版本化新表 migration |
| 仓库工程治理 | IN_PROGRESS | PR #5 已建立 Issue/PR/PROJECT_MASTER/ADR/Governance CI | #6/#7/#8/#9 |

---

## 5. Workbench / OA 工作台

### 当前状态：IN_PROGRESS

### 当前实现

- Electron/Web 已有正式 Workbench Shell；
- Android 已有正式 Workbench Shell；
- 会话协作入口；
- 在线会议入口；
- 组织通讯录入口；
- 设置入口；
- Electron 自动化规则入口。

当前 Workbench 仍主要是“已有业务能力聚合导航页”，还没有形成 Task/Approval 等结构化 OA 写模型。

### 正式 Feature Design

Issue #10 正在把 2026-08-06 的 v0.1 Draft 升级为当前仓库正式设计：

- `doc/features/workbench/README.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- `ADR-0002-workbench-modular-monolith-and-tenant-domains.md`
- `ADR-0003-task-first-lightweight-approval.md`

### 已确定的演进方向

Workbench V1/V2 逐步承载：

1. Overview / 今日工作概览；
2. Todo / 待办聚合；
3. Task / 任务中心；
4. Approval / 审批中心；
5. Calendar / 日程；
6. Announcement / 公告；
7. Report / 日报周报等工作内容；
8. AI 办公与自动化入口。

### 实施原则

- 模块化单体 + 垂直业务包；
- 业务数据进入 tenant schema；
- Task 优先于 Approval；
- Approval V1 不做复杂 BPM；
- Overview 是读聚合，不复制领域写模型；
- 复用 IM、通知、组织、文件与会议；
- 状态变化由服务端状态机校验；
- 核心动作可审计；
- 业务事务提交后再发送外部通知；
- Web/Electron 先形成完整主流程，Android 分阶段补齐；
- Workbench 新表必须走正式版本化 migration。

### 下一阶段

正式设计 PR 合并后，按独立 Issue 推进：

1. Workbench 平台前置（migration/context/permission/audit）；
2. Overview；
3. Task Backend；
4. Task Web/Electron；
5. Task Realtime/Push/Card；
6. Approval Backend；
7. Approval Web/Electron；
8. Calendar / Announcement；
9. Android OA 能力对齐。

---

## 6. AI 与自动化

### 当前状态：IN_PROGRESS

当前仓库已经从单纯客户端 AI 交互向服务端持久化、机器人身份、自动化执行与受控工具调用方向演进。

治理重点：

- 认证身份必须来自服务端安全上下文；
- 会话成员权限必须服务端验证；
- 敏感工具采用明确授权/审批；
- 自动化执行需要幂等、审计和失败可追踪；
- AI/机器人消息应走统一消息持久化与广播链路；
- 客户端不应维护与服务端冲突的“本地真相”。

与 Workbench 的未来集成原则：AI/自动化可以**提议或触发** Task/Approval，但不能绕过 Workbench Domain Service 直接写 OA 表。

---

## 7. 数据库与多租户迁移

### 当前状态：IN_PROGRESS

仓库已经引入安全 schema 同步、结构差异/冲突检测和迁移控制台方向。

当前 Safe Sync：

- 可检测 schema 差异；
- 可安全补 nullable 无 default 缺失字段；
- 不删除/修改已有字段；
- 缺失表判定为冲突，不自动创建。

最终目标：

- 每个 tenant schema 有明确结构版本；
- 迁移脚本不可变且可审计；
- 新租户创建自动迁移到目标版本；
- 老租户可检测 OUTDATED / CONFLICT / ERROR；
- 定时/批量迁移不依赖 HTTP TenantContextFilter；
- 数据库结构不再主要依赖 Hibernate `ddl-auto=update`；
- 完成基线后逐步转向 `validate`。

涉及数据库结构的 PR 必须记录 migration 影响，并说明向前/回滚策略。Workbench 实现不得自行创建与现有迁移治理平行的第二套 migration runner。

---

## 8. 文档体系

### 项目级

- `doc/PROJECT_MASTER.md`：唯一项目事实与状态入口；
- `CONTRIBUTING.md`：贡献者执行规则；
- `doc/development/REPOSITORY_GOVERNANCE.md`：仓库治理细则。

### 架构级

- `doc/architecture/adr/`：Architecture Decision Record；
- 重大且长期有效的架构选择必须新增 ADR，而不是只留在 PR 评论。

当前 ADR：

- ADR-0001：`master` 单一开发主线；
- ADR-0002：Workbench 模块化单体 + tenant domains；
- ADR-0003：Task-first + lightweight Approval。

### 功能级

详细功能设计收敛到：

```text
doc/features/<feature>/
```

Workbench 是第一个按新规范正式整理的 Feature Design。

历史 `doc/功能设计`、`doc/架构设计` 等目录暂不一次性迁移，采用“触达即整理”原则。

---

## 9. PR Definition of Done

一个 PR 只有同时满足以下条件才视为完成：

- [ ] 已关联 Issue；
- [ ] 目标范围单一且可验收；
- [ ] 代码/配置/文档实现完成；
- [ ] 有相应自动化测试或明确人工验证记录；
- [ ] `doc/PROJECT_MASTER.md` 已同步；
- [ ] 相关 Feature Design 已同步（如适用）；
- [ ] ADR 已新增/更新（如涉及重大架构决策）；
- [ ] 数据库/API/协议兼容性已说明（如适用）；
- [ ] CI 必须通过；
- [ ] Review conversation 已解决；
- [ ] 合并后无遗留事项，或已创建 Follow-up Issue。

---

## 10. 分支与合并策略

建议分支命名：

```text
feature/<issue>-<name>
fix/<issue>-<name>
refactor/<issue>-<name>
docs/<issue>-<name>
chore/<issue>-<name>
hotfix/<issue>-<name>
```

默认合并方式：**Squash Merge**。

原因：一个 Issue/PR 对应 master 中一个完整逻辑变更，便于回溯、回滚和生成发布记录。

---

## 11. 当前重点 Roadmap

### P0 — 工程治理

- #6：为 `master` 启用 branch protection / required checks；
- #7：停止 `main` 参与部署；
- #8：补齐 Backend PR CI；
- #9：补齐 Electron/Web PR CI。

### P1 — Workbench 正式设计

- #10：正式化 Workbench/OA Feature Design；
- 固定 ADR-0002 / ADR-0003；
- 设计合并后拆分实现 Issue。

### P1 — 数据库迁移治理

- 固定版本化 tenant migration 基线；
- 新建租户自动迁移；
- 迁移控制台与执行审计闭环；
- 最终逐步从 Hibernate update 转向 validate。

### P1 — Workbench OA

- Platform Foundation；
- Overview；
- Task Backend；
- Task Web/Electron；
- Task Realtime/Push/Card；
- Approval Backend；
- Approval Web/Electron。

### P2 — OA 扩展

- Calendar；
- Announcement；
- Report；
- Android 完整对齐。

---

## 12. 已知治理与技术风险

1. `master` 当前尚未启用 branch protection，#6 未完成前仍存在直接 push 风险；
2. `main` 与 `master` 历史无共同祖先，不应直接合并；
3. 当前 CI 对后端和 Electron/Web 的 PR 门禁仍待 #8/#9；
4. 历史文档存在过期、重复和路径不统一问题，采用渐进整理；
5. Workbench 新表不能依赖 Safe Sync 自动创建，正式 migration 是实现前置；
6. 当前 `ClientEventType` 只有 Chat/Meeting，OA 事件仍需实现；
7. 当前已有 `BOT_CARD`，Workbench 卡片协议不能未经评审直接复用机器人语义；
8. Workbench 新权限不能延续 `username == admin` 的历史做法。

---

## 13. 变更记录

### 2026-08-19 — Issue #10 / Workbench Formal Feature Design

状态：`IN_PROGRESS`

- 重新审阅 2026-08-06 v0.1 Draft；
- 以当前 `master@cdd6ffe` 为新设计基线；
- Workbench Web/Android Shell 改为“已实现事实”，不再作为待新增能力；
- 建立 Workbench Feature Design 目录；
- 正式拆出 Task、Approval、平台集成和实施 Roadmap；
- 明确 Safe Sync 不负责创建 Workbench 新表；
- 明确 BOT_CARD 不直接作为 OA 卡片默认协议；
- 增加 ADR-0002、ADR-0003；
- 下一步从“大 PR 编号计划”切换为独立 Issue 驱动开发。

### 2026-08-19 — Issue #4 / Repository Governance

状态：`IMPLEMENTED / FOLLOW-UPS OPEN`

- 固定 `master` 为唯一开发主线；
- 建立 Issue-driven PR 规则；
- 建立 `PROJECT_MASTER.md` 为唯一项目事实入口；
- 每个 PR 强制同步主文档；
- 新增 PR / Issue 模板；
- 新增 PR Governance CI；
- 新增 ADR 机制。

后续：#6、#7、#8、#9。
