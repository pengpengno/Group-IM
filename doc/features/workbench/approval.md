# Workbench Approval / 审批领域设计

> 状态：PLANNED  
> 所属 Feature：Workbench / OA  
> 入口：`doc/features/workbench/README.md`  
> 跟踪 Issue：#10

Approval 是 Workbench V1 的第二个核心业务域。首版目标是提供可审计、可查询、可跨端处理的**轻量串行审批**，而不是提前建设完整 BPM 平台。

---

## 1. V1 目标

必须支持：

- 固定审批定义；
- 通用申请；
- 串行审批节点；
- 提交；
- 同意；
- 拒绝；
- 退回；
- 重新提交；
- 撤回/取消；
- 转交（可在 V1 后半段启用）；
- 抄送；
- 审批时间线；
- 待我审批/我发起的/我处理过的/抄送我的；
- Realtime / Push / IM Card；
- tenant 隔离；
- 服务端状态机；
- 权限和审计。

V1 明确不做：

- BPMN；
- 可视化流程设计器；
- 条件网关；
- 并行会签；
- 或签；
- 动态脚本；
- 跨公司审批；
- 任意第三方流程插件。

如果业务确实需要上述能力，应先验证轻量模型已不足，再通过 ADR 评估是否升级流程引擎。

---

## 2. 模型

### `ApprovalDefinition`

定义一个可发起的审批类型。

建议字段：

| 字段 | 说明 |
| --- | --- |
| `definition_id` | 主键 |
| `code` | 稳定业务编码，例如 GENERAL / LEAVE / EXPENSE / TRIP |
| `name` | 显示名称 |
| `form_schema_json` | 表单字段定义 |
| `flow_schema_json` | 简单串行流定义 |
| `enabled` | 是否可发起 |
| `version` | 定义版本 |
| `created_by` | 创建人 |
| `created_at` / `updated_at` | 时间 |

定义发生变化后，历史实例不能被“新模板”反向改变。因此提交审批时需要将流程节点实例化。

### `ApprovalInstance`

一次真实申请。

建议字段：

```text
instance_id
definition_id
definition_version
title
applicant_id
department_id
status
form_data_json
current_node_order
submitted_at
completed_at
version
created_at
updated_at
```

### `ApprovalNode`

实例化后的审批节点。

```text
node_id
instance_id
node_order
node_type
assignee_type
assignee_id
status
started_at
completed_at
```

V1 `assignee_type` 可以只支持：

```text
USER
DEPARTMENT_MANAGER
COMPANY_ADMIN
```

如果 `DEPARTMENT_MANAGER` 在实例化时解析成具体 userId，应保存解析结果，保证后续组织变更不会改变已经开始的流程。

### `ApprovalAction`

审批时间线和动作记录：

```text
action_id
instance_id
node_id
operator_id
action
comment
extra_json
created_at
```

动作枚举：

```text
SUBMIT
APPROVE
REJECT
RETURN
TRANSFER
CANCEL
RESUBMIT
COMMENT
```

### `ApprovalCc`

保存抄送人和阅读状态：

```text
id
instance_id
user_id
delivered_at
read_at
created_at
```

---

## 3. 数据表

目标 tenant tables：

```text
wb_approval_definition
wb_approval_instance
wb_approval_node
wb_approval_action
wb_approval_cc
```

建议索引：

```text
idx_wb_approval_instance_applicant_created(applicant_id, created_at desc)
idx_wb_approval_instance_status_created(status, created_at desc)
idx_wb_approval_node_assignee_status(assignee_id, status)
idx_wb_approval_action_instance_created(instance_id, created_at)
idx_wb_approval_cc_user_read(user_id, read_at)
```

所有表必须通过正式 tenant migration 创建。现有 Safe Schema Sync 不负责自动创建缺失表。

---

## 4. 状态机

实例状态：

```text
DRAFT
PENDING
APPROVED
REJECTED
RETURNED
CANCELLED
```

转换：

```text
DRAFT -> PENDING       submit
DRAFT -> CANCELLED     discard/cancel

PENDING -> APPROVED    final approve
PENDING -> REJECTED    reject
PENDING -> RETURNED    return
PENDING -> CANCELLED   applicant cancel（受规则限制）

RETURNED -> PENDING    resubmit
RETURNED -> CANCELLED  applicant cancel
```

约束：

- 禁止通用 Patch 修改 `status`；
- 只有动作 API 可以改变流程；
- `APPROVED` / `REJECTED` / `CANCELLED` 为终态；
- 已完成实例不允许修改原始 `form_data_json`；
- `RETURNED` 后重新提交可以允许修改表单，但必须保留历史 Action；
- 每个动作必须校验“当前节点 + 当前处理人 + 当前实例状态”；
- 并发操作必须使用 version/条件更新避免两个人同时推进同一节点。

---

## 5. 节点流转

### Submit

```text
校验 definition enabled
→ 校验表单
→ 根据 definition version 实例化 node 列表
→ status=PENDING
→ 激活第一个节点
→ 写 SUBMIT action
→ commit
→ 通知当前审批人
```

### Approve

```text
校验当前用户是 active node assignee
→ 当前 node APPROVED
→ 写 APPROVE action
→ 若有下一节点：激活下一节点
→ 否则 instance=APPROVED
→ commit
→ 通知下一审批人或申请人
```

### Reject

```text
当前 node REJECTED
→ instance=REJECTED
→ 写 REJECT action
→ commit
→ 通知申请人
```

### Return

V1 默认退回到申请人，不先支持“退回任意历史节点”。

```text
当前 node RETURNED
→ instance=RETURNED
→ 写 RETURN action
→ commit
→ 通知申请人修改/重提
```

### Transfer

如 V1 开启：

- 只有当前审批人可转交；
- 保存原 assignee 与新 assignee；
- 写 TRANSFER action；
- 不改变节点顺序；
- 必须明确审计链。

---

## 6. 权限

建议功能权限：

```text
approval:submit
approval:process
approval:manage_definition
```

数据权限：

- 申请人：查看自己的申请；
- 当前审批人：查看并处理当前节点；
- 已处理审批人：查看自己处理过的实例；
- 抄送人：只读；
- Approval Manager / Company Admin：按组织策略查看和管理；
- 普通用户不能仅凭 instanceId 查看无关审批。

审批详情通常包含敏感信息，因此权限校验必须在服务端查询入口完成。

---

## 7. API

### Definitions

```http
GET /api/workbench/approval-definitions
```

管理端后续可增加：

```http
POST  /api/workbench/admin/approval-definitions
PATCH /api/workbench/admin/approval-definitions/{id}
```

### Instances

```http
POST /api/workbench/approvals
GET  /api/workbench/approvals
GET  /api/workbench/approvals/{instanceId}
```

### Actions

```http
POST /api/workbench/approvals/{instanceId}/actions/submit
POST /api/workbench/approvals/{instanceId}/actions/approve
POST /api/workbench/approvals/{instanceId}/actions/reject
POST /api/workbench/approvals/{instanceId}/actions/return
POST /api/workbench/approvals/{instanceId}/actions/resubmit
POST /api/workbench/approvals/{instanceId}/actions/transfer
POST /api/workbench/approvals/{instanceId}/actions/cancel
POST /api/workbench/approvals/{instanceId}/comments
```

查询视图：

```text
MY_SUBMITTED
PENDING_FOR_ME
PROCESSED_BY_ME
CC_TO_ME
```

---

## 8. DTO

建议：

```text
ApprovalDefinitionSummaryDTO
ApprovalSubmitRequest
ApprovalActionRequest
ApprovalInstanceDTO
ApprovalSummaryDTO
ApprovalTaskDTO
ApprovalTimelineDTO
ApprovalNodeDTO
```

详情返回应包含：

```text
instance
form snapshot
current node
node timeline
actions
cc
attachments
resolved actor snapshots
availableActions
```

`availableActions` 仅用于 UI 辅助，不能替代服务端权限校验。

---

## 9. 表单策略

V1 可以使用简单 JSON Schema-like 定义，但不要过早建设低代码平台。

例如：

```json
{
  "fields": [
    {"key": "reason", "type": "text", "required": true},
    {"key": "amount", "type": "number", "required": false}
  ]
}
```

服务端必须根据 definition version 验证提交数据，而不是只依赖客户端渲染。

对于 LEAVE / EXPENSE 等未来固定类型，如果出现复杂规则，应在 domain service 中显式实现，而不是无限增加 JSON 表达式能力。

---

## 10. 事件与通知

建议领域事件：

```text
ApprovalSubmittedEvent
ApprovalPendingChangedEvent
ApprovalApprovedEvent
ApprovalRejectedEvent
ApprovalReturnedEvent
ApprovalCancelledEvent
ApprovalTransferredEvent
```

默认通知：

| 事件 | Realtime | Push | IM Card |
| --- | --- | --- | --- |
| 待审批 | 是 | 是 | 是 |
| 同意并进入下一节点 | 是 | 下一审批人是 | 可选 |
| 最终通过 | 是 | 申请人是 | 是 |
| 拒绝 | 是 | 申请人是 | 是 |
| 退回 | 是 | 申请人是 | 是 |
| 转交 | 是 | 新审批人是 | 可选 |
| 撤回 | 是 | 已涉及审批人按策略 | 可选 |

通知在事务提交后执行。

---

## 11. 与 Task 的边界

Approval 和 Task 是两个独立写模型。

可以发生联动，但不能互相直接改表：

```text
Approval approved
→ domain event
→ 可选创建 Task
```

例如费用审批通过后生成“财务付款任务”。该规则应由 Integration/Automation 层处理。

Task 也可以以 `source_type=APPROVAL` 关联原审批实例。

---

## 12. 附件

Approval 不存文件二进制。

附件关联必须做资源权限：

```text
current tenant
+ approval visibility
+ attachment relation
```

审批附件属于高敏感数据，不能仅凭 fileId 下载。

---

## 13. 并发和一致性

重点风险是“重复审批”。

必须至少使用：

- instance/node version；
- 当前节点条件更新；
- action transaction；
- 唯一/状态约束；
- 必要时 idempotency key。

典型场景：两个浏览器同时点击 Approve，只允许一个成功推进节点；另一个收到明确的冲突响应并刷新最新状态。

---

## 14. 测试

### Unit

```text
ApprovalStateMachineTest
ApprovalPermissionServiceTest
ApprovalFlowServiceTest
ApprovalDefinitionValidationTest
```

覆盖：

- submit；
- multi-node serial approve；
- reject；
- return/resubmit；
- cancel；
- transfer；
- 非当前审批人 403；
- 完成后不可修改；
- 并发 approve。

### Integration

重点：

```text
company_a 审批实例
切换 company_b -> 不可见
```

以及 node/action 外键、一致性、分页查询。

### E2E

```text
A 发起审批
→ B 收到待审批
→ B Approve
→ 若最后节点：A 收到通过通知
→ Overview pendingApprovalCount 更新
```

至少再覆盖 Reject 和 Return/Resubmit。

---

## 15. V1 验收

Approval 可标记 `STABLE` 的最低条件：

- 至少一个通用审批 definition 可用；
- Submit / Approve / Reject / Return / Resubmit / Cancel 可用；
- 四类查询视图可用；
- tenant/data permission 正确；
- 时间线可审计；
- 并发审批不重复推进；
- Web/Electron 可完成完整流程；
- Android 可处理待审批核心动作；
- Realtime/Push/Deep Link 正确；
- PROJECT_MASTER 与本文档同步。
