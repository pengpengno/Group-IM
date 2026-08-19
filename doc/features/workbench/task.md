# Workbench Task / 任务领域设计

> 状态：PLANNED  
> 所属 Feature：Workbench / OA  
> 入口：`doc/features/workbench/README.md`  
> 跟踪 Issue：#10

Task 是 Workbench V1 的第一个完整业务闭环。它用于验证 tenant 隔离、权限、通知、附件、审计、多端状态和 Overview 聚合是否可以共同稳定工作。

---

## 1. 业务目标

V1 必须支持：

- 创建任务；
- 指定主负责人；
- 添加协作人/关注人；
- 设置优先级、开始时间、截止时间；
- 从 Chat / Meeting / Approval / AI 等来源创建或关联任务；
- 开始、阻塞、恢复、完成、重新打开、取消；
- 评论；
- 活动时间线；
- Overview / Todo 聚合；
- Realtime / Push / 可选 IM Card；
- 附件；
- tenant、功能权限与数据权限；
- 乐观锁和审计。

V1 不实现：

- 子任务树；
- 甘特图；
- 看板自定义流程；
- 工时核算；
- 跨租户协作任务；
- 自定义状态工作流。

---

## 2. 聚合根与实体

### `WorkTask`

任务聚合根。

核心字段：

| 字段 | 说明 |
| --- | --- |
| `task_id` | 主键 |
| `title` | 标题，建议最大 200 |
| `description` | 描述 |
| `status` | 状态 |
| `priority` | 优先级 |
| `creator_id` | 创建人 |
| `owner_id` | 主负责人，可空 |
| `department_id` | 业务归属部门，可空 |
| `source_type` | MANUAL / CHAT / MEETING / APPROVAL / AI |
| `source_id` | 来源业务 ID |
| `conversation_id` | 关联会话 |
| `start_at` | 计划开始 |
| `due_at` | 截止 |
| `completed_at` | 完成时间 |
| `progress` | 0-100 |
| `version` | 乐观锁 |
| `created_at` | 创建时间 |
| `updated_at` | 更新时间 |
| `deleted` | 逻辑删除 |

任务 Entity 优先存储用户/部门 ID，而不是为所有关系建立 `@ManyToOne User`：

- 降低模块耦合；
- 避免隐式 N+1；
- 用户离职/删除后历史任务仍可保留；
- 展示信息通过 Organization Adapter 批量解析。

### `WorkTaskAssignee`

用于表示除单一 `owner_id` 外的参与关系。

角色：

```text
OWNER
COLLABORATOR
WATCHER
```

约束：

```text
unique(task_id, user_id, role)
```

如果 `OWNER` 同时保留在主表和关系表，两者必须由领域服务同事务维护一致；实现阶段也可以选择只在关系表保存 OWNER，但必须在 ADR/实现 PR 中明确。

### `WorkTaskComment`

建议字段：

```text
comment_id
task_id
author_id
content
reply_to_id
created_at
deleted
```

### `WorkTaskActivity`

面向用户的业务时间线，不等同于安全审计日志。

动作：

```text
CREATE
ASSIGN
UNASSIGN
START
BLOCK
RESUME
COMPLETE
REOPEN
CANCEL
UPDATE_DUE_AT
UPDATE_PRIORITY
UPDATE_PROGRESS
COMMENT
ATTACH_FILE
```

建议保存：

```text
action
actor_id
before_json
after_json
created_at
```

---

## 3. 数据表

目标 tenant tables：

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

这些表位于**当前 company tenant schema**，不在每行依赖客户端传入 `company_id` 做隔离。

推荐索引：

```text
idx_wb_task_owner_status(owner_id, status)
idx_wb_task_creator_created(creator_id, created_at desc)
idx_wb_task_due_status(due_at, status)
idx_wb_task_department_status(department_id, status)
idx_wb_task_conversation(conversation_id)
```

具体 DDL、约束类型和迁移版本必须由实现 PR 根据当时 migration baseline 确定。本设计不预设 `V2`、`V3` 等版本号。

---

## 4. 状态机

状态：

```text
TODO
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

允许转换：

```text
TODO -> IN_PROGRESS      start
TODO -> COMPLETED        complete-directly（可选保留）
TODO -> CANCELLED        cancel

IN_PROGRESS -> BLOCKED   block
IN_PROGRESS -> COMPLETED complete
IN_PROGRESS -> CANCELLED cancel

BLOCKED -> IN_PROGRESS   resume
BLOCKED -> CANCELLED     cancel

COMPLETED -> IN_PROGRESS reopen
```

原则：

- 前端不能通过通用 Patch 任意修改 `status`；
- 所有状态变化使用动作 API；
- 服务端领域对象/状态机进行二次校验；
- `COMPLETED` 时 `progress=100` 且设置 `completed_at`；
- `REOPEN` 清理 `completed_at`；
- `CANCELLED` 默认不可重新开始，若未来需要恢复必须单独产品决策；
- 状态变化同时写 Activity、Audit，并发布领域事件。

实现示意：

```java
void transitionTo(WorkTaskStatus target) {
    if (!status.canTransitionTo(target)) {
        throw new InvalidTaskTransitionException(status, target);
    }
    status = target;
}
```

示例仅用于表达领域约束，不是未经验证即可复制的最终代码。

---

## 5. 权限模型

### 功能权限

建议权限码：

```text
task:create
task:assign
task:view_department
task:update_any
task:delete_any
```

### 数据权限

功能权限不能替代资源权限。

默认规则：

- 创建人可编辑未完成任务的基础信息；
- 主负责人可 Start / Block / Resume / Complete；
- 协作人可查看、评论，并在未来允许更新其负责范围；
- Watcher 只读 + 评论权限由产品规则决定；
- 部门经理可查看本部门任务，但不自动拥有任意状态操作权；
- 公司管理员可拥有全公司查看/管理权限；
- 非相关用户访问任务详情应返回 404 或 403，最终策略需统一，避免资源枚举。

新 Workbench 代码禁止继续通过 `username == admin` 判断权限。

---

## 6. API 设计

Base：

```text
/api/workbench/tasks
```

### Command

```http
POST   /api/workbench/tasks
PATCH  /api/workbench/tasks/{taskId}
POST   /api/workbench/tasks/{taskId}/actions/start
POST   /api/workbench/tasks/{taskId}/actions/block
POST   /api/workbench/tasks/{taskId}/actions/resume
POST   /api/workbench/tasks/{taskId}/actions/complete
POST   /api/workbench/tasks/{taskId}/actions/reopen
POST   /api/workbench/tasks/{taskId}/actions/cancel
POST   /api/workbench/tasks/{taskId}/assignees
DELETE /api/workbench/tasks/{taskId}/assignees/{userId}
POST   /api/workbench/tasks/{taskId}/comments
```

### Query

```http
GET /api/workbench/tasks
GET /api/workbench/tasks/{taskId}
GET /api/workbench/tasks/{taskId}/activities
```

建议视图：

```text
ASSIGNED_TO_ME
CREATED_BY_ME
WATCHING
DEPARTMENT
ALL_COMPANY    # 仅授权用户
```

查询示例：

```http
GET /api/workbench/tasks?view=ASSIGNED_TO_ME&status=TODO,IN_PROGRESS&page=0&size=20
```

创建请求示例：

```json
{
  "title": "完成工作台任务列表接口",
  "description": "实现分页、状态过滤和租户隔离",
  "priority": "HIGH",
  "ownerId": 12,
  "collaboratorIds": [15, 18],
  "dueAt": "2026-08-28T18:00:00+08:00",
  "conversationId": 310,
  "sourceType": "MANUAL"
}
```

请求中不包含用于决定租户的数据路由 `companyId`。

---

## 7. Query / DTO

DTO 建议：

```text
TaskCreateRequest
TaskUpdateRequest
TaskActionRequest
TaskQueryRequest
TaskDTO
TaskSummaryDTO
TaskCommentDTO
TaskActivityDTO
```

列表返回 Summary，不加载评论和完整 Activity。

详情可以包含：

```text
basic task
participants
attachment summaries
latest comments / comment count
activity timeline page
resolved user snapshots
permissions/actions available for current user
```

建议服务端直接返回 `availableActions`，避免客户端复制复杂权限逻辑；服务端仍必须再次校验实际动作。

---

## 8. 领域事件

建议事件：

```text
TaskCreatedEvent
TaskAssignedEvent
TaskAssigneeChangedEvent
TaskStatusChangedEvent
TaskDueSoonEvent
TaskCommentAddedEvent
```

发布规则：

```text
事务内：写 Task / Activity / Audit
事务提交
  ↓
AFTER_COMMIT handler
  ↓
ClientEvent / Push / optional IM Card / projection refresh
```

V1 可用事务后事件；可靠性要求提高时转 Outbox。

---

## 9. 通知策略

建议默认：

| 事件 | Realtime | Push | IM Card |
| --- | --- | --- | --- |
| 新指派 | 是 | 离线/策略允许 | 是/可配置 |
| 协作人新增 | 是 | 可选 | 可选 |
| 普通评论 | 是 | 默认否 | 否 |
| 即将到期 | 是 | 是 | 否 |
| 完成 | 是 | 创建人/关注人按策略 | 可选 |
| 重新打开 | 是 | 负责人按策略 | 可选 |

在线/免打扰/推送策略必须复用现有 Notification Policy，不在 Task 内重新实现。

---

## 10. 来源联动

`source_type`：

```text
MANUAL
CHAT
MEETING
APPROVAL
AI
```

使用场景：

- 从一条消息“转为任务”；
- 从会议纪要创建行动项；
- 审批通过后生成后续任务；
- AI 提议生成任务，用户确认后落库。

任何自动来源都必须明确 `creator_id` / actor，以及是否经过用户确认。

---

## 11. 附件

Task 不复制文件二进制，统一通过平台 `wb_resource_attachment` / 文件适配层关联已有 File 资源。

下载时必须校验：

```text
current tenant
+ task visibility
+ file relation
```

知道 `fileId` 不代表拥有访问权限。

---

## 12. 并发与幂等

- `@Version` / version 字段处理并发修改；
- 状态动作对当前状态做条件校验；
- 重复 Complete 在状态已经 COMPLETED 时返回明确幂等结果或 409，具体统一策略在实现 PR 确定；
- 创建接口若由自动化/消息转任务触发，应支持 source/idempotency key，避免事件重试重复创建；
- 评论创建可使用客户端 request ID 防止网络重试重复提交。

---

## 13. 测试

### Unit

```text
TaskStatusTransitionTest
TaskPermissionServiceTest
TaskServiceTest
TaskOverviewQueryTest
```

至少覆盖：

- 合法/非法状态转换；
- owner/creator/collaborator/无关用户权限；
- completedAt/progress 一致性；
- reopen；
- 乐观锁冲突；
- 事件只在业务成功后产生。

### Integration

推荐 PostgreSQL Testcontainers：

```text
company_a 创建 task
切换 company_b -> 查询不到
切回 company_a -> 可查询
```

同时覆盖表约束、分页、索引查询、附件权限。

### E2E

首条必须通过的用户链路：

```text
A 创建任务并指派 B
→ B 收到提示
→ B 进入 Workbench
→ B Start
→ B Complete
→ A 看到 Completed
→ Overview/Todo 数字同步
```

---

## 14. V1 验收

Task 只有满足以下条件才可标记为 `STABLE`：

- API 完成创建、查询、指派、状态动作、评论；
- Web/Electron 不借助 Postman 可完成闭环；
- Android 可查看并执行核心动作；
- tenant 隔离测试通过；
- 数据权限服务端校验；
- Activity + Audit 可追踪；
- 通知策略正确；
- 附件访问安全；
- Overview/Todo 正确聚合；
- PROJECT_MASTER 与本设计同步。
