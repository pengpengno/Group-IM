# Workbench Platform Foundation

> 状态：IMPLEMENTATION — Issue #13 / PR #38  
> 依赖：#12 Tenant Versioned Migration — COMPLETED  
> 非范围：Task / Approval 业务实现、OA Card emission、全局 Security 重写

本文固定 Workbench V1 服务端平台基础。后续 `overview`、`task`、`approval`、`calendar`、`announcement` 必须复用这些边界，而不是各自重新读取 SecurityContext、判断 admin、切 schema 或直接耦合旧模块 Repository。

---

## 1. Package Boundary

```text
com.github.im.server.workbench
├── common
│   ├── context
│   ├── error
│   ├── permission
│   ├── audit
│   ├── integration
│   └── tenant
├── overview
├── task
├── approval
├── calendar
└── announcement
```

规则：domain 可以依赖 `workbench.common`，`common` 不反向依赖 Task/Approval 等领域。

---

## 2. CurrentWorkContext

请求级 Workbench tenant identity 只来自认证用户：

```text
SecurityContext authenticated User
→ User.currentCompany
→ companyId / schemaName / active
→ CurrentWorkContext
```

客户端 `companyId` 不作为数据库路由输入。

`SecurityCurrentWorkContextProvider` 验证：

- authentication 存在且 principal 是 `User`；
- `userId` 存在；
- current company 存在；
- company active；
- schema 非空且不是 `public`；
- 如果 HTTP filter 已绑定 `SchemaContext`，必须与认证公司 schema 完全一致。

Mismatch 不是自动修复，而是 `WORKBENCH_TENANT_CONTEXT_MISMATCH`，避免请求在错误 tenant 上继续执行。

---

## 3. Error Contract

Workbench 不新增第三/第四种 HTTP error envelope。

```text
WorkbenchException
→ BusinessException
→ GlobalExceptionHandler
→ ApiResponse / ProblemDetail
```

稳定错误码前缀：`WORKBENCH_*`。

当前基础错误包括：

- authentication required；
- current company required；
- company inactive；
- invalid tenant scope；
- tenant context mismatch；
- permission denied / policy missing；
- member not found；
- file not available。

---

## 4. Permission Foundation

禁止：

```java
username.equals("admin")
```

最小权限入口：`WorkbenchPermissionService`。

当前 company-member baseline 支持：

- `VIEW_WORKBENCH`；
- `TASK_CREATE`；
- `TASK_UPDATE`；
- `APPROVAL_CREATE`；
- `APPROVAL_ACT`。

这只是 Feature Permission 的最低门槛，不等价于资源权限。Task/Approval 仍必须在领域层继续判断 creator/assignee/approver/department 等数据权限。

高权限动作（当前如 `ANNOUNCEMENT_PUBLISH`）如果没有明确 policy，**fail closed**，返回 `WORKBENCH_PERMISSION_POLICY_MISSING`。

---

## 5. Organization Adapter

Workbench 领域不直接散落调用旧组织 Repository。

```text
OrganizationAdapter
├── isActiveMember(companyId, userId)
└── requireActiveMember(companyId, userId)
```

当前实现通过 tenant-local `company_user` / `users` 视图校验 active membership。

后续可以在 adapter 内升级为批量用户/部门解析、离职状态、头像等，而不改变 Task/Approval 领域接口。

---

## 6. File Adapter

Workbench 不重写存储系统。

当前 adapter boundary：

```text
FileAdapter
├── isAvailable(fileId)
└── requireAvailable(fileId)
```

它只证明当前 tenant 下文件存在且状态为 `NORMAL`。

**它不等价于完整资源授权。** 后续 Task/Approval attachment 必须继续验证：当前用户能访问业务资源，且该 file 与该资源存在合法 attachment relation。知道 fileId 不能直接绕过资源授权。

---

## 7. Audit Foundation

```text
WorkbenchAuditEvent
→ WorkbenchAuditSink
```

事件字段包含：

- eventId；
- companyId / schemaName；
- actorUserId；
- category/action；
- resourceType/resourceId；
- before/after state；
- occurredAt；
- metadata。

当前 sink 是 structured application log，先建立统一调用边界。后续若落 `wb_audit_log` 或 outbox，只替换/扩展 sink，不让每个领域发明独立审计格式。

`WorkbenchAuditService` 同时支持 request current actor 与后台 explicit actor。

---

## 8. Explicit Tenant Execution for Jobs

后台 Job 没有 HTTP `TenantContextFilter`，禁止假设线程已经带 tenant。

```text
WorkbenchTenantScope(companyId, schemaName)
→ WorkbenchTenantExecutor.execute(scope, operation)
→ validate non-public tenant schema
→ SchemaSwitcher.executeInSchema
→ finally restore / clear previous SchemaContext
```

规则：

1. Job 必须显式提供 companyId + schemaName；
2. `public` 不是 Workbench business tenant；
3. Repository/transaction 调用必须在 executor operation 内开始；
4. executor 不负责扫描 company；调度器先获取有效 company，再逐 tenant 执行；
5. 不允许依赖 HTTP Filter 或请求 SecurityContext。

---

## 9. Tests

#13 基础单元测试覆盖：

- authenticated user/company/schema resolve；
- tenant `SchemaContext` mismatch；
- inactive current company；
- missing authentication；
- explicit tenant bind + previous context restoration；
- no previous tenant 时 finally clear；
- `public` rejection；
- company-member baseline permission；
- privileged permission fail-closed；
- non-member denied。

这些测试不依赖生产或测试 tenant 数据。

---

## 10. Follow-up Usage

### Overview

只调用 `CurrentWorkContextProvider` / permission foundation，不接受 client companyId 路由。

### Task

```text
CurrentWorkContext
→ WorkbenchPermissionService
→ OrganizationAdapter (assignee validation)
→ FileAdapter (attachment availability)
→ Task domain permission/resource checks
→ transaction
→ WorkbenchAuditService
```

### Approval

复用同一基础，但 approver/current-node/sensitive-field 权限由 Approval domain 自己实现。

### Scheduled Jobs

```text
active companies
→ WorkbenchTenantExecutor
→ domain service
→ explicit/system audit actor
```

---

## 11. Definition of Done for #13

- Workbench common package established；
- current user/company/schema unified and validated；
- no new Workbench command uses username-based admin bypass；
- Workbench errors use stable project-standard mapping；
- permission service foundation is fail-closed；
- audit primitives available；
- Organization/File adapters defined and wired；
- Job tenant execution independent from HTTP filter；
- context/tenant/permission tests green；
- `PROJECT_MASTER` and Workbench design updated；
- no Task/Approval business implementation mixed into this PR。
