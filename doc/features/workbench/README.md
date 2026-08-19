# Workbench / OA 工作台 Feature Design

> 状态：ACTIVE DESIGN  
> 首次正式化：2026-08-19  
> 当前基线：`master@cdd6ffed21b562b912a01bc8e7abd04eeb1ac3b2`  
> 跟踪 Issue：#10  
> 项目级事实入口：`doc/PROJECT_MASTER.md`

本文档是 Group-IM 工作台/OA 业务的**正式 Feature Design 入口**。项目当前状态仍以 `doc/PROJECT_MASTER.md` 为唯一事实源；本目录负责记录 Workbench 的稳定业务边界、领域模型和实施约束。

本文由 2026-08-06 的《Group-IM 工作台大模块：初步代码设计与实施路线》v0.1 Draft 重新审阅、拆分并升级而来。旧文档基于 `master@41ac69b`，其中大量方向仍有效，但仓库已经出现 Workbench 壳、Android Workbench、服务端 AI/自动化和安全 Schema 同步能力，因此旧文档不再作为开发基线。

---

## 1. 当前事实

截至本设计基线：

### 已经存在

- Web/Electron 已有 `renderer/features/workbench/Workbench.tsx`；
- Android 已有 `ui/workbench/WorkbenchUI.kt`；
- 两端当前 Workbench 都主要承担“既有协作能力聚合入口”；
- Web/Electron 已可从 Workbench 进入会话、会议、通讯录、设置和自动化；
- Android 已可从 Workbench 进入会话、会议、通讯录和设置；
- 后端已存在 Schema 多租户、组织、会议、文件、消息、实时事件和推送策略等可复用平台能力；
- 后端已存在 `SafeTenantSchemaSyncService`，可检测 tenant schema 漂移/冲突，并只自动补充安全的 nullable 无 default 缺失字段；
- `ClientEventType` 当前仍只有 `CHAT_MESSAGE_CREATED` 与 `MEETING_INVITE_CREATED`，Workbench 业务事件尚未加入；
-消息协议已有 `BOT_CARD`，但它语义属于机器人动作卡片，不应直接被 OA 业务泛化复用。

### 尚未形成

- Workbench Overview 聚合 API；
- 我的待办统一模型；
- Task 业务闭环；
- Approval 业务闭环；
- OA 日程与 Meeting/Task 聚合；
- 公告发布/已读；
- Workbench 专用实时事件、Push 以及结构化业务卡片；
- Workbench 完整 RBAC/数据权限；
- Workbench 新表的正式版本化迁移。

因此当前 Workbench 状态定义为：**IN_PROGRESS / Shell Ready, OA Domains Planned**。

---

## 2. 产品定位

Group-IM 的工作台不是另一个独立 OA 产品，也不是把所有业务重新存储一遍的“门户数据库”。

它承担两个职责：

1. **聚合入口**：把消息、会议、组织、自动化等已存在能力组织为工作入口；
2. **结构化办公域**：承载 IM 本身无法可靠表达的 Task、Approval、Schedule、Announcement、Report 等业务对象。

原则：

```text
聊天/会议/联系人等已有业务
        ↓ 复用，不复制
Workbench Overview / Todo
        ↓ 聚合
Task / Approval / Schedule / Announcement
        ↓ 新增结构化 OA 业务
IM Card / Realtime / Push
        ↓ 把 OA 结果带回协作链路
```

---

## 3. 第一阶段业务范围

### 3.1 V1 必须纳入

1. Workbench Overview；
2. Todo 摘要；
3. Task；
4. 轻量 Approval；
5. 今日 Schedule 与现有 Meeting 聚合；
6. Announcement 基础展示和已读；
7. IM/实时事件/Push 联动；
8. 最小 RBAC + 数据权限；
9. 审计；
10. Web/Electron 完整核心流程；
11. Android 至少完成 Overview、Task、Approval 的主要处理动作。

### 3.2 V1 明确不做

- 通用 BPMN 引擎；
- 可视化流程设计器；
- 自定义低代码表单平台；
- 薪资、招聘、绩效；
- 定位打卡；
- 复杂排班；
- 在线协作文档；
- 甘特图/复杂项目管理；
- 跨租户 BI 平台。

这些能力必须通过后续独立 Issue/ADR 评估，不能反向阻塞 V1。

---

## 4. 领域拆分

Workbench 在现有 Spring Boot 服务中采用**模块化单体 + 垂直业务包**。

目标服务端结构：

```text
com.github.im.server.workbench
├── common
├── overview
├── task
├── approval
├── calendar
├── announcement
└── integration
```

领域职责：

| 领域 | 职责 | 是否拥有写模型 |
| --- | --- | --- |
| `overview` | 首页聚合、Todo Summary、Quick Apps | 否，主要为读模型 |
| `task` | 任务、负责人、评论、活动时间线 | 是 |
| `approval` | 审批定义、实例、节点、动作、抄送 | 是 |
| `calendar` | 个人日程、提醒、Meeting/Task 映射 | 是（个人日程）+ 聚合 |
| `announcement` | 公告、目标范围、阅读回执 | 是 |
| `integration` | IM Card、ClientEvent、Push、文件、组织适配 | 否 |

禁止把所有 Workbench 类继续平铺到全局 `controller/`、`service/`、`repository/` 中。

---

## 5. 核心设计原则

### P1 — 租户由认证上下文决定

业务接口不接受客户端 `companyId` 作为数据路由依据。

```text
JWT / authenticated User
  ↓
current company / schema
  ↓
TenantContextFilter + SchemaContext
  ↓
当前 tenant schema
```

客户端即使伪造 `companyId` 也不能改变数据访问租户。

### P2 — Task 先形成闭环

首个真实业务闭环：

```text
新建任务
→ 指派同事
→ 对方收到应用内/Push/可选 IM 卡片
→ 开始任务
→ 完成任务
→ Overview/Todo 数字变化
→ 创建人看到状态结果
```

Task 是验证 Workbench 的租户、权限、通知、附件、多端和审计链路的首要领域。

### P3 — Approval 首版保持轻量

V1 使用固定定义 + 实例化串行节点，不先引入 BPMN、会签、复杂条件路由和图形设计器。

### P4 — 首页读模型与领域写模型分离

`WorkbenchOverviewService` 只聚合轻量 Query，不直接操作 Task/Approval 完整 Entity。

### P5 — 业务提交成功后再对外通知

IM Card、Realtime、Push 必须在业务事务成功后触发。V1 可采用 `@TransactionalEventListener(AFTER_COMMIT)`；需要可靠重试后升级 Outbox。

### P6 — 核心动作可审计

Task 状态变化、Approval 动作、公告发布、转交、撤回等必须保留操作者、时间、资源和关键前后状态。

### P7 — 已有平台能力优先复用

- 用户/部门：Organization；
- 文件：现有 File 服务；
- 会议：现有 Meeting；
- 消息：现有 MessageService；
- Push：现有 NotificationPolicyService/Push Gateway；
- 多租户：现有 SchemaContext/Tenant Filter；
- 自动化：现有服务端自动化能力作为未来触发器之一。

Workbench 不重复实现这些平台。

---

## 6. Overview / Todo 设计

### 6.1 聚合接口

```http
GET /api/workbench/overview
```

只返回首页摘要：

```json
{
  "todoSummary": {
    "assignedTaskCount": 5,
    "overdueTaskCount": 1,
    "pendingApprovalCount": 2,
    "unreadAnnouncementCount": 3
  },
  "recentTasks": [],
  "pendingApprovals": [],
  "todaySchedules": [],
  "announcements": [],
  "quickApps": []
}
```

### 6.2 Query 原则

- 不加载每个领域完整详情；
- Count 使用专用 SQL/Repository Query；
- 列表限制条数；
- 首版优先不引入缓存；
- 如果后续加入短 TTL 缓存，Task/Approval 动作、公司切换后必须失效；
- 公司切换后客户端必须清空所有 Workbench 相关状态。

### 6.3 Todo 不是独立写业务

Todo 是聚合视图，不建立“复制所有待办”的第二套业务真相。初期来源：

- Task assigned to me；
- Approval pending for me；
- Schedule reminders；
- Announcement unread/urgent（是否显示为 Todo 由产品规则决定）。

后续若需要高性能统一 inbox，可单独 ADR 评估 materialized inbox/outbox projection。

---

## 7. 多端信息架构

### Web / Electron

当前 Workbench 已是正式 Feature。后续不再“新增 Workbench Tab”，而是在现有入口内增加 OA 内容。

目标首页：

```text
Workbench
├── Header / Current Company
├── Todo Summary
├── Quick Apps
├── Recent Tasks
├── Pending Approvals
├── Today Schedule
└── Announcements
```

Task、Approval 等保持独立 slice/feature state，禁止全部塞进一个巨型 `workbenchSlice`。

### Android

当前 Android 已有 `WorkbenchUI`，因此后续是**填充业务能力**而不是新建入口。

优先顺序：

1. Overview；
2. 我的任务列表/详情；
3. Start/Complete/Comment；
4. 待审批/审批动作；
5. 公告；
6. 复杂创建/管理能力后置。

---

## 8. API 通用约定

Workbench 新 API 统一使用项目标准 `ApiResponse<T>` 风格，不再增加第三种响应格式。

推荐错误类别：

- `400` 参数/非法状态转换；
- `401` 未认证；
- `403` 功能或数据权限不足；
- `404` 当前 tenant 下资源不存在；
- `409` 乐观锁/幂等/并发状态冲突；
- `500` 未预期服务端错误。

时间字段使用带时区语义的 ISO-8601。服务端优先 `Instant` / `OffsetDateTime`；如果沿用 `LocalDateTime`，接口契约必须明确公司/用户时区解释。

---

## 9. DTO 与跨端契约

Java DTO 继续放在 `entity` 模块的 Workbench 命名空间：

```text
entity/src/main/java/com/github/im/dto/workbench/
├── overview/
├── task/
├── approval/
├── calendar/
└── announcement/
```

首版可人工保持 Java/TypeScript/Kotlin 字段一致，但这不是长期方案。接口稳定后应建立 OpenAPI 契约和代码生成，减少三端 DTO 漂移。

---

## 10. 详细设计索引

- [Task 任务设计](./task.md)
- [Approval 审批设计](./approval.md)
- [平台与多端集成](./platform-integration.md)
- [实施 Roadmap](./implementation-roadmap.md)

相关 ADR：

- `ADR-0001`：master 唯一开发主线；
- 后续 Workbench 架构关键决定通过本 Issue/后续 PR 正式记录。

---

## 11. 从 v0.1 Draft 到正式设计的修正

| v0.1 假设 | 当前处理 |
| --- | --- |
| Web Workbench 尚未创建 | 已创建，视为现有 Shell |
| Android Workbench 后期才新增 | 已创建，后续填充 OA |
| Flyway 从零引入即可解决新表 | 调整为“先服从现有迁移治理”；新表不能由 Safe Sync 自动创建 |
| `V2__create_workbench_task.sql` 可作为示例 | 取消具体版本假设，实际版本必须由迁移基线决定 |
| Workbench 新消息类型可直接定义 | 保留设计方向，但需与现有 `BOT_CARD`/协议兼容性做单独实现评审 |
| ClientEvent 可直接增加 OA 事件 | 仍是未来工作；当前枚举只有 Chat/Meeting |
| PR-0/PR-1 可直接进入实现 | 先完成仓库治理与正式 Feature Design，再拆 Issue 实施 |

---

## 12. 完成定义

Workbench V1 达成以下条件才可以从 `IN_PROGRESS` 转为 `STABLE`：

### 功能

- Overview 可用；
- Task 可创建、指派、开始、阻塞/恢复、完成、评论；
- Approval 可提交、同意、拒绝、退回、重提、撤回；
- Meeting 能在今日日程中聚合；
- Announcement 可查看并记录已读；
- Task/Approval 有正确 Realtime/Push/Deep Link；
- Android 至少可处理核心 Task/Approval 流程。

### 安全

- tenant schema 严格隔离；
- 服务端状态机；
- 服务端数据权限；
- 附件资源授权；
- 核心动作审计；
- 不使用 `username == admin` 作为 Workbench 权限方案。

### 工程

- 所有新表进入版本化迁移；
- Task/Approval 状态机有单元测试；
- tenant 隔离有集成测试；
- Web 状态在公司切换时清理；
- 现有 IM/Meeting/Auth/Contacts 无回归；
- 每个实现 PR 同步更新 `PROJECT_MASTER.md` 与本 Feature Design。
