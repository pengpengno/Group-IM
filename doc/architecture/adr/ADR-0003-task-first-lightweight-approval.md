# ADR-0003: Workbench V1 先完成 Task 闭环并采用轻量串行 Approval

- Status: Accepted
- Date: 2026-08-19
- Related Issue: #10

## Context

Workbench 计划覆盖 Task、Approval、Calendar、Announcement、Report 等 OA 能力。审批域如果一开始按完整 BPM 平台设计，会同时引入流程设计器、条件分支、会签/或签、动态表单、流程版本兼容等大量复杂度。

Task 的业务模型更直接，却同样能够验证 Workbench 最关键的平台能力：tenant、权限、状态机、通知、附件、审计、多端和 Overview 聚合。

## Decision

1. Workbench V1 优先完成 Task 的完整业务闭环；
2. Task 稳定后再进入 Approval 主实现；
3. Approval V1 使用固定 Definition + 实例化串行 Node；
4. V1 不引入 BPMN、可视化流程设计器、条件网关、并行会签和低代码平台；
5. Task 与 Approval 保持独立写模型，通过 Domain Event/Integration 联动；
6. Overview/Todo 作为读聚合，不成为两者状态机的拥有者。

## Consequences

### Positive

- 更快验证 Workbench 平台基础；
- 降低首版业务和技术风险；
- Task 的通知、tenant、权限和审计基础可被 Approval 复用；
- 审批模型可在真实需求出现后再判断是否需要升级 BPM。

### Negative

- 首版 Approval 不满足复杂企业流程；
- 如果后续确认必须支持会签/复杂条件，可能需要升级流程模型；
- 产品侧需要接受“先可用，再泛化”的交付顺序。

## Alternatives Considered

### Task 与 Approval 同时完整开发

不采用。会把数据库、权限、通知、多端和流程复杂度叠加到同一阶段，PR 难以独立验收。

### 直接引入成熟 BPMN Engine

V1 不采用。当前没有足够需求证明其复杂度和运维成本必要。

### Approval 只用一个 JSON 状态字段，无节点实例

不采用。无法稳定保存当前审批人、历史流程版本和可审计时间线。

## Follow-ups

- Task Backend Issue；
- Task Web/Electron Issue；
- Task Notification/Card Issue；
- Approval Backend Issue；
- Approval Web/Electron Issue；
- 若未来需要复杂 BPM，新增 ADR 替代本决策的 Approval 范围部分。
