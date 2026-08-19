# Workbench / OA 实施 Roadmap

> 状态：ACTIVE PLAN  
> 设计 Issue：#10  
> 规则：每个阶段必须 `Issue -> Branch -> PR -> CI -> PROJECT_MASTER -> Merge`

本文把 2026-08-06 v0.1 草稿中的 PR-0 ~ PR-10 重新整理为当前仓库的 Issue/PR 驱动路线。Workbench Web/Android Shell、数据库安全同步和仓库治理已经存在，因此旧编号不再直接沿用。

---

## 1. 总体策略

Workbench V1 按以下顺序推进：

```text
#10 Formal Design
   ↓
#12 Versioned Tenant Migration
   ↓
#13 Workbench Platform Foundation
   ↓
Overview
   ↓
Task Backend
   ↓
Task Web/Electron
   ↓
#14 Structured OA Card / ClientEvent Protocol
   ↓
Task Realtime / Push / Card
   ↓
Approval Backend
   ↓
Approval Web/Electron
   ↓
Calendar + Announcement
   ↓
Android OA capabilities
```

核心原则：每一步必须可独立验收，避免一个 PR 同时决定数据库、后端业务、Electron、Android 和消息协议。

---

## 2. 已建立的前置 Issue

| Issue | 状态 | 目的 | 是否阻塞业务实现 |
| --- | --- | --- | --- |
| #10 | IN_PROGRESS | 正式化 Workbench/OA Feature Design | 是，设计基线 |
| #12 | OPEN | 建立可创建新表的版本化 tenant migration | 是，阻塞 Task/Approval 新表 |
| #13 | OPEN | 建立 Workbench common/context/permission/audit/adapters | 是，阻塞领域实现规范化 |
| #14 | OPEN | 固定 OA Card / ClientEvent / Push / Deep Link 协议 | 阻塞 Task/Approval 卡片通知，不阻塞纯 Backend |

依赖关系：

```text
#10
 ├── #12 database migration
 ├── #13 platform foundation (depends on #12 for DB-backed foundation)
 └── #14 structured notification protocol
```

---

## 3. Phase 0 — 正式设计基线（#10）

### 交付

- Workbench Feature Design；
- Task 设计；
- Approval 设计；
- 平台集成设计；
- 本 Roadmap；
- PROJECT_MASTER 同步；
- ADR-0002 / ADR-0003。

### 验收

- 不修改业务代码；
- Governance CI 通过；
- 旧 v0.1 过时假设已移除；
- 后续开发有唯一正式设计入口；
- 实现前置项已转为真实 Issue，而不是文档 TODO。

---

## 4. Phase 1A — Versioned Tenant Migration（#12）

### 目标

为 Workbench 以及后续业务模块建立唯一、可审计的 tenant 新表迁移路径。

### 范围

- public/global 与 tenant migration 的目录/版本策略；
- 新 tenant 创建后迁移到 target version；
- 已有 tenant 批量 migration；
- migration history / version tracking；
- Safe Sync 与正式 migration 的职责边界；
- drift/conflict preflight；
- 执行审计与失败恢复；
- 多 tenant integration test；
- Hibernate `ddl-auto=update -> validate` 的分阶段路线。

### 关键约束

当前 `SafeTenantSchemaSyncService` **不自动创建缺失表**，所以不能依赖它生成：

```text
wb_task
wb_approval_*
wb_schedule
wb_announcement
```

### 验收

- 能安全创建新表/索引/约束；
- 新旧 tenant 可达到同一 target version；
- 冲突 tenant 不执行危险 DDL；
- 迁移结果可追踪。

---

## 5. Phase 1B — Workbench Platform Foundation（#13）

依赖：#12 的正式 migration 路径已明确到可供 Workbench 使用的程度。

### 范围

- `workbench/common` 模块边界；
- `CurrentWorkContext`；
- Workbench exception/error mapping；
- 最小 PermissionService 框架；
- Audit 基础；
- Organization Adapter；
- File Adapter / resource authorization boundary；
- 后台 Job 的 explicit TenantExecutor/SchemaSwitcher 规则。

### 不做

- 完整 Task；
- Approval；
- 大规模 Workbench UI；
- OA Card 协议。

### 验收

- 能统一读取/校验 current user/company/schema；
- 新 Workbench Command 不使用 `username == admin`；
- Job tenant execution 不依赖 HTTP Filter；
- 后续 Task/Approval 可复用统一权限、审计和适配边界。

---

## 6. Phase 2 — Overview 基础

在 #13 完成后创建独立 Feature Issue。

### 后端

```http
GET /api/workbench/overview
```

初期可以聚合已有 Meeting + 空 Task/Approval/Announcement projection，但 API 契约需要稳定。

### Web/Electron

在现有 Workbench Shell 内加入：

- Current Company；
- Todo Summary；
- Quick Apps；
- Today Schedule/Meeting；
- Recent Tasks placeholder/真实数据逐步接入；
- Pending Approvals placeholder/真实数据逐步接入；
- Loading / Empty / Error；
- 公司切换 reset/refresh。

### Android

可以同步接 Overview，也可以延后到 Task 阶段；不要求同一 PR 完整实现全部卡片。

### 验收

- 两个公司切换无数据串租户；
- 当前 Workbench 入口能力无回归；
- 首页不通过 N 个独立卡片请求拼接完整业务详情。

---

## 7. Phase 3 — Task Backend

创建独立 Feature Issue；依赖 #12 + #13。

### 数据

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

### 代码边界

```text
workbench/task/
├── model
├── repository
├── service
├── query
├── permission
├── controller
├── mapper
└── event
```

### API 范围

- Create / Query / Detail；
- Edit metadata；
- Assignee；
- Start / Block / Resume / Complete / Reopen / Cancel；
- Comment；
- Activity timeline。

### 测试

- 状态机；
- 权限；
- tenant isolation；
- optimistic lock；
- Controller 401/403/409；
- migration。

### 验收

不依赖 UI，通过 API 可完成：

```text
create -> assign -> start -> complete -> query history
```

---

## 8. Phase 4 — Task Web/Electron

### 范围

- Task List；
- Task Detail；
- Create/Edit；
- Assignees；
- Filters/Pagination；
- Comments；
- Activity；
- 状态动作；
- Overview count 联动；
- 公司切换 state reset。

Task 使用独立 slice/store，不把完整 Task 写进 Overview state。

### 验收

```text
A 创建任务并指派 B
→ B 在 Web/Electron 查看
→ B Start / Complete
→ A 查看状态和 Overview 更新
```

---

## 9. Phase 5A — Structured OA Card / ClientEvent Protocol（#14）

#14 可以与 Task Backend 并行设计，但必须在 Task 卡片/Push 正式实现前完成。

### 决策内容

- 独立 `WORKBENCH` MessageType，或泛化 `APP_CARD/STRUCTURED_CARD`；
- 不破坏已有 `BOT_CARD` 机器人语义；
- payload version/category/action/resourceId/status/deepLink；
- ClientEventType 扩展；
- Push payload 最小化；
- Web/Electron + Android 兼容渲染；
- 未知卡片的老客户端降级策略；
- Deep Link tenant/resource 再授权。

### 验收

必须通过 ADR 固定选择，而不是在 Task PR 中临时决定。

---

## 10. Phase 5B — Task Realtime / Push / Card

依赖：Task Backend + #14。

### 范围

- Task ClientEvent；
- Notification Policy；
- Deep Link；
- Task Card renderer；
- AFTER_COMMIT handler；
- online/offline 策略；
- Push payload 安全。

### 验收

- 在线/离线按策略通知；
- Deep Link 到正确 tenant/resource；
- DB rollback 不产生成功通知；
- 老客户端有安全降级。

---

## 11. Phase 6 — Approval Backend

依赖：#12 + #13；Task 主闭环已验证平台基础。

### 数据

```text
wb_approval_definition
wb_approval_instance
wb_approval_node
wb_approval_action
wb_approval_cc
```

### 首版流程

- 固定 Definition；
- 串行 Nodes；
- Submit；
- Approve；
- Reject；
- Return；
- Resubmit；
- Cancel；
- Transfer（如范围允许）；
- CC。

### 测试重点

- current assignee；
- serial node advance；
- terminal state；
- concurrent approve；
- tenant isolation；
- sensitive detail permission。

---

## 12. Phase 7 — Approval Web/Electron + Notification

卡片/通知部分依赖 #14 已完成。

### UI

- 发起；
- 待我审批；
- 我发起的；
- 我处理过的；
- 抄送我的；
- Detail/Timeline；
- Approve/Reject/Return/Cancel；
- Attachment。

### 验收

```text
A submit
→ B pending
→ B approve
→ A result
→ pending count correct
```

---

## 13. Phase 8 — Calendar / Meeting Aggregation

### 范围

- `wb_schedule`；
- participant/reminder；
- personal schedule；
- `MeetingCalendarAdapter`；
- Task due projection；
- Today Schedule；
- reminder Job。

Meeting 的真实参与状态仍由 Meeting 模块维护，不复制成 Workbench 第二份真相。

---

## 14. Phase 9 — Announcement

### 范围

- Announcement；
- Company/Department/User target；
- read receipt；
- publish permission；
- normal/urgent notification policy；
- Overview unread card。

### 验收

- 目标范围外用户不可读取；
- read count 正确；
- urgent Push 正确；
- 发布动作可审计。

---

## 15. Phase 10 — Android OA 完整度

Android Workbench Shell 已存在，本阶段是补足领域能力而不是新建工作台入口。

推荐顺序：

1. Overview；
2. Task list/detail；
3. Task Start/Complete/Comment；
4. Approval pending/detail/actions；
5. Announcement；
6. Deep Link / Push routing；
7. Task/Approval 创建体验；
8. 管理配置最后补。

---

## 16. 后续 Phase — Report / AI Office

Task/Approval 稳定后再评估：

- 日报/周报；
- AI 总结工作内容；
- AI 从会议/消息提议任务；
- 自动化触发 Task/Approval；
- 部门工作汇总。

AI/Automation 创建或修改真实业务对象必须经过权限和必要的人机确认，并调用 Workbench Domain Service，不能直接写 OA 表。

---

## 17. 每个实现 Issue 必须包含

```text
背景
目标
范围
非范围
设计文档链接
依赖 Issue
数据/API/协议影响
验收标准
测试计划
兼容性/迁移风险
```

涉及长期架构决定时，同时建立 ADR。

---

## 18. 每个实现 PR 必须包含

- `Fixes/Closes #issue`；
- 实现与测试；
- `doc/PROJECT_MASTER.md` 更新；
- 本目录对应 Feature Design 更新；
- migration 说明（如有）；
- API/协议兼容性说明（如有）；
- 当前模块状态变化；
- Follow-up Issue。

推荐分支示例：

```text
feature/13-workbench-foundation
feature/<issue>-workbench-overview
feature/<issue>-task-backend
feature/<issue>-task-web
feature/<issue>-task-notification
feature/<issue>-approval-backend
feature/<issue>-approval-web
feature/<issue>-workbench-calendar
feature/<issue>-announcement
feature/<issue>-android-workbench-oa
```

架构类：

```text
feature/12-tenant-migration
feature/14-workbench-card-protocol
```

实际前缀可按 Issue 类型使用 `feature/`、`refactor/` 或 `chore/`，但必须符合 Governance CI。

---

## 19. 风险处理顺序

最先解决的不是 UI，而是：

1. tenant migration（#12）；
2. platform/data permission（#13）；
3. 状态机；
4. 并发；
5. notification transaction boundary；
6. card/event protocol（#14）；
7. attachment authorization；
8. company-switch stale state；
9. 多端 DTO 漂移。

禁止为了“先看到页面”跳过这些基础后直接大规模实现 OA UI。

---

## 20. V1 最终 E2E Gate

### Task

```text
A(company X) create task for B
→ B realtime/push
→ B start/complete
→ A observes completed
→ switch company Y: task unavailable
```

### Approval

```text
A submit
→ B pending notification
→ B approve
→ A result notification
→ Overview pending count changes
→ switch company Y: approval unavailable
```

### Regression

同时验证：

- Chat；
- Meeting；
- Login；
- Company switch；
- Contacts；
- File upload/download；
- AI/BOT_CARD existing rendering。

只有这些 Gate 全部通过，Workbench V1 才可以进入 `STABLE`。
