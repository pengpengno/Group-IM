# Workbench / OA 实施 Roadmap

> 状态：ACTIVE PLAN  
> 设计 Issue：#10  
> 规则：每个阶段必须 `Issue -> Branch -> PR -> CI -> PROJECT_MASTER -> Merge`

本文把旧 v0.1 文档中的 PR-0 ~ PR-10 重新整理为当前仓库的 Issue/PR 驱动路线。由于 Workbench 壳、Android Workbench、数据库安全同步和仓库治理已经存在，旧编号不再直接沿用。

---

## 1. 总体策略

WorkBench V1 按“平台前置 -> Task 闭环 -> Approval -> 扩展域 -> Android 补齐”的顺序推进。

```text
Design Baseline
   ↓
Migration / Permission / Context Foundation
   ↓
Overview
   ↓
Task Backend
   ↓
Task Web/Electron
   ↓
Task Notification/Card
   ↓
Approval Backend
   ↓
Approval Web/Electron
   ↓
Calendar + Announcement
   ↓
Android OA capabilities
```

每一步必须可独立验收，避免一次 PR 同时修改数据库、后端、Electron、Android 和协议全部内容。

---

## 2. Phase 0 — 正式设计基线

### 当前 Issue

`#10 docs(workbench): formalize OA business design and implementation roadmap`

### 交付

- Workbench Feature Design；
- Task 设计；
- Approval 设计；
- 平台集成设计；
- 实施 Roadmap；
- PROJECT_MASTER 同步；
- 关键 ADR。

### 验收

- 不改业务代码；
- Governance CI 通过；
- 旧 v0.1 的过时假设已移除；
- 后续开发有唯一设计入口。

---

## 3. Phase 1 — Workbench 平台前置

建议建立一个独立 Feature/Technical Issue。

### 范围

- 明确 tenant migration 正式路径；
- 不允许依赖 Safe Sync 创建新表；
- 建立/确认 Workbench `CurrentWorkContext`；
- 建立 Workbench exception/error 映射；
- 建立最小 PermissionService 基础；
- 建立 Audit 基础；
- 确认 explicit tenant executor / SchemaSwitcher 用于 Job；
- 确认附件资源权限适配接口。

### 不做

- Task 业务表；
- Approval；
- Workbench UI 大改。

### 验收

- 能在测试中显式验证当前公司/tenant；
- 迁移框架能为后续新表提供可靠路径；
- 不破坏现有 IM/Meeting/Organization。

---

## 4. Phase 2 — Overview 基础

### 范围

后端：

```http
GET /api/workbench/overview
```

初期可以聚合已有 Meeting + 空 Task/Approval/Announcement projection，但 API 契约需要稳定。

Web/Electron：

- 在现有 Workbench 中增加 Overview 布局；
- Current Company；
- Todo Summary skeleton；
- Quick Apps；
- Meeting/协作入口继续保留；
- Loading / Empty / Error；
- 公司切换 reset/refresh。

Android：

- 可在本阶段或后续 Task 阶段接入 Overview；
- 不要求同时完成全部 OA 卡片。

### 验收

- 两个公司切换后 Overview 无数据串租户；
- Workbench 当前壳能力不回归。

---

## 5. Phase 3 — Task Backend

### 数据

创建：

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

### 代码

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

### API

- Create；
- Query；
- Detail；
- Edit metadata；
- Assignee；
- Start/Block/Resume/Complete/Reopen/Cancel；
- Comment；
- Activity。

### 测试

- 状态机；
- 权限；
- tenant isolation；
- optimistic lock；
- Controller 401/403/409；
- migration。

### 验收

不借助 UI，通过 API 可完整完成：

```text
create -> assign -> start -> complete -> query history
```

---

## 6. Phase 4 — Task Web/Electron

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

### 状态管理

Task 使用独立 slice/store，不把完整 Task 放进 Workbench Overview state。

### 验收

```text
用户 A Web 创建任务
→ 指派 B
→ B Web 可查看并完成
→ A 列表和 Overview 更新
```

此阶段可以先用应用内刷新/Realtime 基础，Push/Card 在下一 Phase 完成。

---

## 7. Phase 5 — Task Realtime / Push / Card

### 必须单独评审协议

当前仓库已有 `BOT_CARD`，不能直接当 Workbench 卡片使用。

本阶段先通过 Issue/ADR 确认：

```text
WORKBENCH message type
or
更通用 STRUCTURED/APP CARD protocol
```

### 范围

- Task ClientEvent；
- Notification policy；
- Deep Link；
- Task Card renderer（若采用）；
- AFTER_COMMIT handler；
- online/offline 策略；
- Push payload 安全。

### 验收

- 在线/离线均按策略收到通知；
- Deep Link 到正确 tenant/resource；
- 数据库事务失败时不发送成功通知。

---

## 8. Phase 6 — Approval Backend

### 数据

```text
wb_approval_definition
wb_approval_instance
wb_approval_node
wb_approval_action
wb_approval_cc
```

### 首版流程

- 固定 definition；
- 串行 nodes；
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

## 9. Phase 7 — Approval Web/Electron + Notification

### UI

- 发起；
- 待我审批；
- 我发起的；
- 我处理过的；
- 抄送我的；
- Detail/Timeline；
- Approve/Reject/Return/Cancel；
- Attachment。

### 通知

- pending；
- result；
- return；
- transfer；
- Deep Link。

### 验收

```text
A submit
→ B pending
→ B approve
→ A result
→ pending count correct
```

---

## 10. Phase 8 — Calendar / Meeting Aggregation

### 范围

- `wb_schedule`；
- participant/reminder；
- personal schedule；
- MeetingCalendarAdapter；
- Task due projection；
- Today Schedule；
- reminder Job。

### 约束

Meeting 的真实参与状态仍由 Meeting 模块维护，不复制到 Workbench 成第二份真相。

---

## 11. Phase 9 — Announcement

### 范围

- Announcement；
- Company/Department/User target；
- read receipt；
- publish permission；
- normal/urgent notification policy；
- Overview unread card。

### 验收

- 指定范围外用户不可读取；
- read count 正确；
- urgent Push 正确；
- 发布动作可审计。

---

## 12. Phase 10 — Android OA 完整度

Android Workbench 入口已经存在，因此本阶段不是“创建工作台”，而是补足领域能力。

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

## 13. 后续 Phase — Report / AI Office

在 Task/Approval 稳定后再评估：

- 日报/周报；
- AI 总结工作内容；
- AI 从会议/消息提议任务；
- 自动化触发 Task/Approval；
- 部门工作汇总。

AI 创建或修改真实业务对象必须经过权限和必要的人机确认，不允许绕过 Workbench Domain Service 直接写表。

---

## 14. 每个实现 Issue 必须包含

```text
背景
目标
范围
非范围
设计文档链接
数据/API/协议影响
验收标准
测试计划
兼容性/迁移风险
```

如果涉及重大架构决定，同时建立 ADR。

---

## 15. 每个实现 PR 必须包含

- `Fixes/Closes #issue`；
- 代码实现；
- 测试；
- `doc/PROJECT_MASTER.md` 更新；
- 本目录对应 Feature Design 更新；
- migration 说明（如有）；
- API/协议兼容性说明（如有）；
- 当前模块状态变化；
- Follow-up Issue。

---

## 16. 推荐分支

示例：

```text
feature/<issue>-workbench-foundation
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

---

## 17. 风险顺序

最先解决的风险不是 UI，而是：

1. tenant migration；
2. data permission；
3. 状态机；
4. 并发；
5. notification transaction boundary；
6. attachment authorization；
7. company-switch stale state；
8. 多端 DTO 漂移。

因此禁止为了“先看到页面”跳过前四项后直接大规模做 OA UI。

---

## 18. V1 最终 E2E Gate

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
- AI/Bot Card existing rendering。

只有这些 Gate 都通过，Workbench V1 才能进入稳定状态。
