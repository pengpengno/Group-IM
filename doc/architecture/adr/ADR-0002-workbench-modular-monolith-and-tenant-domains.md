# ADR-0002: Workbench 采用模块化单体并将 OA 业务数据置于 Tenant Schema

- Status: Accepted
- Date: 2026-08-19
- Related Issue: #10

## Context

Group-IM 已经拥有统一 Spring Boot Server、Schema 级多租户、组织、消息、会议、文件、实时事件和 Push 等平台能力。

Workbench/OA 将新增 Task、Approval、Schedule、Announcement 等领域。这些领域与现有用户、组织、会话、会议、文件高度关联。如果首版直接拆成独立微服务，将引入：

- 跨服务身份/tenant 传播；
- 分布式事务；
- 数据复制；
- 部署和观测成本；
- 尚未证明必要的服务边界。

同时 OA 数据必须保持公司级强隔离。

## Decision

1. Workbench V1 继续部署在现有 `server` 中，采用**模块化单体 + 垂直业务包**；
2. 目标包根为 `com.github.im.server.workbench`；
3. Task、Approval、Schedule、Announcement 等业务表进入当前 Company 的 tenant schema；
4. API 不接受/不信任客户端 `companyId` 来决定租户；租户来自认证用户当前工作上下文；
5. Overview 是聚合读模型，不拥有 Task/Approval 的第二份写数据；
6. Workbench 通过 Adapter/Integration 层复用 Organization、Message、Meeting、File、Notification 等平台能力；
7. 新业务表必须进入正式版本化 migration，不能依赖 Safe Schema Sync 自动创建缺失表。

## Consequences

### Positive

- 与现有架构一致；
- tenant 隔离边界清晰；
- 可直接复用平台能力；
- 减少首版分布式复杂度；
- 领域仍有清晰模块边界，未来具备拆服务可能；
- Overview 不复制业务真相。

### Negative

- Server 模块会继续变大；
- 必须严格执行垂直包边界，否则会重新退化成全局 Controller/Service 平铺；
- tenant migration 必须支持多 Schema；
- Workbench Integration 层需要控制跨领域依赖方向。

## Alternatives Considered

### 独立 OA 微服务

暂不采用。当前领域尚未达到需要独立部署/扩缩容的程度，且会放大 tenant 和事务复杂度。

### Workbench 所有数据放 Public Schema 并带 company_id

不采用。会与现有 Schema 多租户主架构冲突，并增加每个 Query 遗漏 company filter 的泄漏风险。

### Overview 建立完整 Todo/Task 副本

首版不采用。避免双写一致性问题；未来如性能需要，可通过 projection/inbox ADR 单独演进。

## Follow-ups

- 固定 tenant migration 正式路径；
- 建立 Workbench common/context/permission 基础；
- 实现 Overview；
- 实现 Task；
- tenant isolation integration tests。
