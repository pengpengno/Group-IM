# Workbench 平台与多端集成设计

> 状态：ACTIVE DESIGN  
> 所属 Feature：Workbench / OA  
> 跟踪 Issue：#10

本文记录 Workbench 领域之外但必须统一遵守的平台约束：tenant、权限、数据库迁移、IM/实时事件/Push、附件、定时任务、Web/Electron 与 Android 状态一致性。

---

## 1. 多租户边界

当前系统以 PostgreSQL Schema 作为公司租户边界。

请求链路：

```text
Authenticated User
→ current company/schema
→ TenantContextFilter
→ SchemaContext
→ Hibernate / JDBC current tenant
```

Workbench 业务表原则上全部位于 tenant schema：

```text
company_a.wb_task
company_a.wb_task_assignee
company_a.wb_approval_instance
...

company_b.wb_task
company_b.wb_task_assignee
company_b.wb_approval_instance
...
```

### 约束

- API 不信任客户端 `companyId` 来选择数据；
- tenant 来自认证上下文；
- 后台 Job 没有 HTTP Filter，必须显式进入每个 schema；
- 公司切换成功后客户端必须清理 Workbench 缓存/state；
- tenant query 不允许跨 schema join 形成隐式数据泄漏；
- 真正跨租户的数据才进入 public/global 表。

---

## 2. 当前 Schema Sync 现状与 Workbench 约束

当前已有 `SafeTenantSchemaSyncService`。

它的安全策略非常保守：

- 可检测 tenant 与 public managed structure 差异；
- 状态包括 `SYNCED` / `OUTDATED` / `CONFLICT` / `ERROR`；
- 只自动新增“nullable 且没有 default”的缺失字段；
- 不删除字段；
- 不修改现有字段；
- **缺失整张表会报告冲突，不自动创建表**。

因此：

> Workbench 的 `wb_task`、`wb_approval_*`、`wb_schedule`、`wb_announcement` 等新表不能依赖 Safe Sync 自动生成。

新业务表必须通过正式版本化 migration 创建。

Safe Sync 的定位应保留为：

```text
Drift / conservative repair / migration preflight helper
```

而不是取代完整 migration engine。

---

## 3. 正式数据库迁移方向

Workbench 实现 PR 必须服从仓库最终确定的 tenant migration 架构。

目标能力：

1. public/global migration 有明确版本；
2. tenant migration 有明确版本；
3. 新公司创建后自动执行到当前 target version；
4. 老公司可批量检查版本；
5. migration 脚本不可随意修改历史版本；
6. 执行结果可审计；
7. schema drift 可在执行前阻止高风险迁移；
8. Hibernate 最终从 `ddl-auto=update` 逐步转为 `validate`。

### 重要规则

- 本设计不预设 Flyway 具体版本号；
- 不允许在 Task PR 中顺手创造一套与现有迁移治理平行的新 migration runner；
- 应先由数据库治理 Issue/PR 固定迁移框架，再让 Workbench 新表接入；
- Safe Sync 与正式 migration 的职责必须在实现前明确，避免双写 DDL 体系。

### 对 Workbench 的迁移顺序建议

```text
migration foundation ready
  ↓
Task tables
  ↓
Approval tables
  ↓
Schedule / Announcement tables
```

---

## 4. CurrentWorkContext

Workbench Service 不应散落重复逻辑来读取用户、公司和 schema。

建议提供 Workbench 范围的上下文适配：

```text
CurrentWorkContext
├── requireUserId()
├── requireCompany()
├── requireSchema()
└── tenant/company membership checks
```

注意：

- 是否允许 `public` 作为有效业务 tenant 不能只按 schema 名硬编码决定；
- 应根据当前 Company 模型和成员关系判断；
- 定时任务不要使用请求级 `CurrentWorkContext`，而是使用显式 SchemaSwitcher/TenantExecutor。

---

## 5. 权限架构

当前系统历史权限模型仍较轻，Workbench 不应扩大历史 `username == admin` 方式。

V1 建议角色：

```text
SYSTEM_ADMIN
COMPANY_ADMIN
DEPARTMENT_MANAGER
APPROVAL_MANAGER
ANNOUNCEMENT_MANAGER
EMPLOYEE
```

建议权限码：

```text
workbench:view
workbench:configure

task:create
task:assign
task:view_department
task:update_any
task:delete_any

approval:submit
approval:process
approval:manage_definition

announcement:publish
announcement:view_statistics

report:view_department
```

### 两层权限

```text
Feature Permission
    +
Resource/Data Permission
```

例如：

```text
task:create = 可以创建任务
≠ 可以编辑所有任务
```

V1 可以先用领域 PermissionService：

```text
TaskPermissionService
ApprovalPermissionService
AnnouncementPermissionService
```

再逐步接入 `@PreAuthorize`。不要为了 Workbench 一次性重写整个旧安全模型，但所有新 Command 必须有服务端权限检查。

---

## 6. 审计

业务时间线与安全审计用途不同。

### 用户业务时间线

例如：

```text
wb_task_activity
wb_approval_action
```

展示给业务用户。

### 平台审计

建议通用：

```text
wb_audit_log
```

字段：

```text
audit_id
resource_type
resource_id
action
actor_id
request_id
ip_address
user_agent
before_json
after_json
created_at
```

审计原则：

- 不记录密码、token、敏感密钥；
- 表单敏感字段需要脱敏策略；
- 审计不能被普通业务删除动作一起逻辑删除；
- 管理员查询审计也需要权限控制。

---

## 7. IM Card 协议

旧设计建议增加 `WORKBENCH` 消息类型。当前仓库已经存在 `BOT_CARD`，用于机器人动作卡片。

### 当前结论

**不建议直接复用 `BOT_CARD` 承载 OA 卡片。**

原因：

- BOT_CARD 有明确机器人语义；
- Task/Approval/Announcement 不是机器人消息；
- 过载会让客户端渲染、权限和统计语义混乱。

V1 实现前应在协议 PR 中二选一：

### 方案 A — `WORKBENCH`

新增明确 OA 消息类型：

```json
{
  "version": 1,
  "category": "TASK",
  "action": "ASSIGNED",
  "resourceId": 901,
  "title": "完成工作台接口",
  "summary": "张三指派给你",
  "status": "TODO",
  "deepLink": "group://workbench/task/901"
}
```

category：

```text
TASK
APPROVAL
ANNOUNCEMENT
SCHEDULE
REPORT
```

### 方案 B — 泛化结构化业务卡片协议

如果未来 Chat 内有更多非机器人业务卡片，可通过 ADR 把 `BOT_CARD` 和 Workbench Card 提炼为更通用的 `APP_CARD/STRUCTURED_CARD`。

该方案影响协议和多端兼容，必须单独 Issue/ADR，不应在 Task PR 中临时完成。

当前 Feature Design 默认按**独立 Workbench Card 语义**推进。

---

## 8. ClientEvent

当前 `ClientEventType` 只有：

```text
CHAT_MESSAGE_CREATED
MEETING_INVITE_CREATED
```

Workbench 需要新增的候选事件：

```text
TASK_ASSIGNED
TASK_STATUS_CHANGED
TASK_DUE_SOON
APPROVAL_PENDING
APPROVAL_RESULT
ANNOUNCEMENT_PUBLISHED
SCHEDULE_REMINDER
```

### Deep Link

```text
group://workbench
group://workbench/task/{taskId}
group://workbench/approval/{instanceId}
group://workbench/announcement/{announcementId}
```

客户端收到事件后：

```text
switch/open Workbench
→ select module
→ load resource detail
```

Deep Link 必须最终走资源权限校验，不能因为来自 Push 就绕过服务端授权。

---

## 9. Push

统一复用现有通知策略组件，不在 Workbench 重新维护“是否在线/是否推送”。

推荐：

| 事件 | 应用内 | Push |
| --- | --- | --- |
| Task assigned | 是 | 离线/策略允许 |
| Task due soon | 是 | 是 |
| Task ordinary comment | 是 | 默认否 |
| Approval pending | 是 | 是 |
| Approval result | 是 | 是 |
| Normal announcement | 是 | 按策略 |
| Urgent announcement | 是 | 是 |
| Schedule reminder | 是 | 是 |

Push payload 只放必要摘要和 deep link，不放完整敏感审批数据。

---

## 10. 事务与领域事件

原则：

```text
DB transaction success
   ↓
Domain Event AFTER_COMMIT
   ↓
Realtime / Push / IM Card
```

避免：

```text
先发 Push
→ DB rollback
→ 用户收到不存在的任务/审批
```

V1：

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
```

后续可靠性要求提升：

```text
business tables + outbox_event in same transaction
→ worker
→ send
→ success / retry / dead-letter
```

Outbox 应作为平台能力，不应 Task 和 Approval 各写一套。

---

## 11. 文件与附件

Workbench 不重新开发存储。

建议通用关联：

```text
wb_resource_attachment
```

字段：

```text
id
resource_type      TASK/APPROVAL/COMMENT/ANNOUNCEMENT
resource_id
file_id
file_name
mime_type
file_size
uploaded_by
created_at
```

文件授权：

```text
当前用户
→ 当前 tenant
→ 是否可访问业务资源
→ resource_attachment 是否关联该 file
→ File download
```

禁止“知道 fileId 就能下载”。

---

## 12. 组织信息

Task/Approval 表优先保存 userId/departmentId。

展示层由：

```text
WorkbenchOrganizationAdapter
```

批量解析：

```text
userId -> name/avatar/status
departmentId -> name/path
```

避免每条 Task 通过 JPA 关系加载 User。

对于离职用户：

- 历史业务对象保留 userId；
- UI 可显示离职/未知用户；
- 不因组织对象删除破坏历史审批和审计。

---

## 13. Calendar / Meeting 聚合

目标表：

```text
wb_schedule
wb_schedule_participant
wb_schedule_reminder
```

类型：

```text
PERSONAL
MEETING
TASK_DUE
APPROVAL_REMINDER
```

Meeting 已经拥有自己的参与和状态模型，因此 Workbench 不复制 Meeting 的真实状态。

建议：

```text
MeetingCalendarAdapter
→ 转为 WorkbenchScheduleDTO
```

Overview todaySchedules 同时聚合个人日程和现有 Meeting。

---

## 14. Announcement

目标表：

```text
wb_announcement
wb_announcement_target
wb_announcement_receipt
```

Target：

```text
COMPANY
DEPARTMENT
USER
```

Receipt：

```text
delivered_at
read_at
```

发布动作必须有权限和审计；紧急公告与普通公告使用不同通知策略。

---

## 15. 定时任务

候选 Job：

```text
TaskDueReminderJob
ApprovalPendingReminderJob
ScheduleReminderJob
AnnouncementPublishJob
```

关键约束：

- Job 没有请求级 TenantContext；
- 必须查询有效 Company，并显式切换 schema；
- finally 清理 tenant context；
- 分批扫描；
- 幂等；
- 多实例需要 DB/Redis 锁或调度平台防重；
- 每次提醒需要 last sent / reminder record；
- Push 失败要可追踪。

伪流程：

```text
active companies
→ for each company schema
→ TenantExecutor.execute(schema)
→ scan due resources
→ write reminder/outbox
→ clear schema
```

---

## 16. Web/Electron 状态

目标目录：

```text
renderer/features/workbench/
├── Workbench.tsx / WorkbenchScreen.tsx
├── components/
├── task/
├── approval/
└── announcement/
```

状态原则：

- Overview 独立轻状态；
- Task 独立 slice；
- Approval 独立 slice；
- Announcement 独立或 query state；
- 不把所有 OA 数据塞入 `workbenchSlice`；
- 公司切换时 reset；
- deep link 打开详情时以服务端重新读取为准。

API 数量增多后，可从 `apiClient.ts` 拆出：

```text
workbenchAPI.ts
taskAPI.ts
approvalAPI.ts
announcementAPI.ts
```

---

## 17. Android 状态

当前 `WorkbenchUI` 已存在，后续层次：

```text
UI
→ WorkbenchViewModel / TaskViewModel / ApprovalViewModel
→ Repository
→ HTTP client
```

建议：

```text
model/workbench/
repository/WorkbenchRepository.kt
repository/TaskRepository.kt
repository/ApprovalRepository.kt
viewmodel/WorkbenchViewModel.kt
viewmodel/TaskViewModel.kt
viewmodel/ApprovalViewModel.kt
ui/workbench/
```

Android 不需要在第一个 Task PR 同时实现所有管理能力；先保证查询和核心动作。

---

## 18. 测试与安全基线

### tenant isolation

```text
company_a create resource
→ company_b cannot read/update
→ company_a can read
```

Task、Approval、Announcement 都必须有此类测试。

### permission

- 401 未登录；
- 403/404 无资源权限；
- 非审批人不可审批；
- 非负责人不可执行受限 Task 动作；
- 管理权限不能只靠前端隐藏按钮。

### company switch

- Web/Android 不显示上个公司的 Overview；
- 旧详情路由切公司后重新授权；
- Push deep link 进入错误公司时不能访问资源。

---

## 19. 平台实施前置条件

在 Workbench Task 真正落库前，至少确认：

1. tenant migration 的正式创建新表路径；
2. `public` 是否可以作为真实业务 tenant；
3. Workbench 最小权限服务策略；
4. 统一 API error 处理方式；
5. Workbench Card 协议方向；
6. ClientEvent 扩展方式；
7. Job 的显式 tenant executor；
8. 附件资源权限适配。

这些事项不能通过前端 UI 临时规避。
