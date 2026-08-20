# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**
>
> 本文描述 Group-IM 当前是什么、已经实现什么、正在交付什么、下一步是什么。代码、配置、数据库、协议、CI/CD、架构或产品能力变更，必须在同一个 PR 同步更新本文。

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：Issue #12 — Tenant Versioned Migration Epic
- 当前交付：Issue #13 / PR #38 — Workbench Platform Foundation
- 下一业务阶段：Workbench Overview → Task Backend
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目目标

Group-IM 是面向组织协作的多端 IM 与办公平台。即时通信是协作主链路，Workbench 承载结构化办公能力，AI / Automation 嵌入会话和业务流程。

长期原则：

1. 消息是协作主链路；
2. Workbench 承载 Task / Approval / Schedule / Announcement / Report；
3. AI / Automation 不建立与业务服务冲突的第二份真相；
4. Web/Electron 与 Android 保持核心业务语义一致；
5. 多租户、权限、审计、迁移、文件和通知作为平台能力复用。

---

## 2. 仓库治理

- `master` 是唯一开发主线；
- `main` 是历史遗留分支，不参与正常开发与发布；
- 所有代码、数据库、配置和文档变更必须通过 PR；
- Bug 必须有 Issue；重要功能、架构、数据库变更原则上也必须先有 Issue；
- 每个 PR 必须更新本文件；
- 默认 Squash Merge。

已实现：

- PR #5：Issue-driven PR、PROJECT_MASTER、模板、Governance CI、ADR；
- PR #16：Backend PR Validation；
- PR #23 / #18：ADR-0004 Tenant Migration Architecture；
- PR #24 / #19：Tenant Migration Runtime；
- PR #27 / #26：Trusted Tenant Schema Snapshot Tooling；
- #32：Trusted Snapshot Review；
- PR #33 / #25：Core Tenant Baseline Migrations；
- PR #35 / #20：New Tenant Provisioning；
- PR #36 / #21：Existing Tenant Baseline / Validate；
- Issue #12：Tenant Versioned Migration Epic — COMPLETED。

仍待治理：#6 master protection、#7 legacy `main` deploy trigger、#9 Electron/Web PR CI、#22 Maven duplicate dependencies。

---

## 3. 技术架构概览

### Server

Java 21 / Spring Boot 3.x / Maven multi-module / Spring Security + JWT / Spring Data JPA / PostgreSQL schema multi-tenancy / Redis / WebSocket / Spring AI。

### Client

Electron + React + TypeScript；Kotlin Multiplatform + Compose Android。

### Tenant database current facts

- PostgreSQL schema 是 company tenant boundary；
- `spring.flyway.enabled=false`，migration 由显式 runtime 驱动；
- core business baseline = `2026081906`；
- managed current target = `2026082001`；
- 新公司：inactive reservation → CREATE SCHEMA → Flyway → verify → active；
- 既有 tenant：read-only preflight → explicit baseline → migrate/validate → audit/state；
- default `spring.jpa.hibernate.ddl-auto=update`；
- `application-schema-validate.yml` 仅供 staged rollout；
- legacy public clone / Safe Sync 是 deprecated transitional compatibility，不是 migration authority。

测试 tenant 数据不作为产品事实；需要时可清理重建，不阻塞功能模块实现。

---

## 4. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| 登录/鉴权 | STABLE | JWT/Spring Security；inactive company 不可进入 | RBAC 细化 |
| 多公司/多租户 | STABLE | #12 migration foundation 完成 | 运营观测 / validate rollout |
| 单聊/群聊 | STABLE | 核心 IM 主链路存在 | 搜索/治理/一致性 |
| 联系人/组织 | STABLE | 公司/部门/员工能力存在 | Workbench adapter + 权限治理 |
| 文件 | IN_PROGRESS | 上传/分片存在 | 资源级授权持续加强 |
| 会议 | IN_PROGRESS | Meeting 服务与多端入口存在 | Workbench 聚合 |
| AI 助手 | IN_PROGRESS | AI/Bot 能力持续演进 | 工具治理 |
| 群自动化 | IN_PROGRESS | 规则/执行/管理存在 | 审批/审计 |
| Workbench | IN_PROGRESS | Web/Electron + Android Shell；#13 platform foundation 正在交付 | Overview |
| OA Task | PLANNED | 正式领域设计已形成 | #13 后 Backend |
| OA Approval | PLANNED | 轻量串行审批设计已形成 | Task 闭环后 |
| Tenant Migration Foundation | STABLE | #12 完成；baseline 1906 / target 2001 | 支撑 OA 新 migration |
| Backend PR CI | STABLE | Java 21 compile + tests | #6 required check |

---

## 5. Workbench / OA

正式设计：

- `doc/features/workbench/README.md`
- `doc/features/workbench/platform-foundation.md`
- `doc/features/workbench/platform-integration.md`
- `doc/features/workbench/task.md`
- `doc/features/workbench/approval.md`
- `doc/features/workbench/implementation-roadmap.md`

关键 ADR：

- ADR-0002：Workbench modular monolith + tenant domains；
- ADR-0003：Task-first + lightweight Approval；
- ADR-0005：Workbench structured card / client event protocol。

### #13 Platform Foundation — PR #38

目标：在 Task/Approval 之前统一所有 Workbench 服务端领域都会重复需要的平台能力。

当前实现边界：

```text
com.github.im.server.workbench.common
├── context
├── error
├── permission
├── audit
├── integration
└── tenant
```

已实现于 PR #38：

- `CurrentWorkContext` / `CurrentWorkContextProvider`：从 authenticated `User.currentCompany` 得到 user/company/schema；
- 校验 current company active、tenant schema 合法、`SchemaContext` 与认证公司一致；
- Workbench stable error code，通过现有 `BusinessException` + `GlobalExceptionHandler` 输出；
- `WorkbenchPermissionService`：以 active company membership 为最小基线，未配置的高权限策略 fail-closed；
- 禁止新 Workbench command 使用 `username == admin`；
- `OrganizationAdapter`：隔离 Workbench 与旧组织 Repository/Entity；
- `FileAdapter`：建立 tenant-local attachment availability boundary；
- `WorkbenchAuditEvent` / Sink / Service：支持 request actor 与 explicit job actor；
- `WorkbenchTenantScope(companyId, schemaName)`；
- `WorkbenchTenantExecutor`：后台任务不依赖 HTTP TenantContextFilter，显式 tenant execution，并在 finally 恢复/清理 SchemaContext；
- unit tests 覆盖认证上下文、tenant mismatch、inactive company、tenant restore、public rejection、permission fail-closed。

非范围：Task/Approval model/API、OA 新表、Workbench Card emission、大规模 Spring Security 重写。

### 当前依赖链

```text
#12 Tenant Versioned Migration Epic ✅
        ↓
#13 Workbench Platform Foundation 🚧 PR #38
        ↓
Workbench Overview
        ↓
Task Backend
        ↓
Task Web/Electron
        ↓
Realtime / Push / Structured Card
        ↓
Approval Backend / UI
```

#14 protocol design 已接受；#28/#29/#30 按 client-first rollout gate 推进，server actual WORKBENCH emission 不能早于客户端兼容。

---

## 6. Tenant Versioned Migration — #12 COMPLETED

Canonical docs：

- `doc/architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md`
- `doc/architecture/tenant-migration/README.md`

Runtime rules：

1. `<tenant>.flyway_schema_history` 是 tenant migration authority；
2. PLAN 只读；
3. empty / Flyway-managed tenant 才走 normal APPLY；
4. non-empty + no-history tenant 先 preflight/baseline；
5. 同 tenant advisory lock；
6. migration immutable，只新增版本；
7. 禁止 blind `baselineOnMigrate(true)`；
8. public/current schema 不能动态充当 expected contract。

Core baseline `2026081906` 包含 18 core tables、3 tenant identity views、17 identity sequences、65 constraints、26 PK/UNIQUE backing indexes。

Managed target `2026082001` 记录 baseline contract，不改变 core business schema。

未来 Workbench/OA tenant table 只能在 `2026082001` 之后追加新的 immutable migrations。

---

## 7. New / Existing Tenant Lifecycle

### New tenant — #20 / PR #35

```text
reserve inactive company
→ create empty schema
→ Flyway migrate current target
→ verify
→ activate
```

失败保持 inactive；CompanyCreatedEvent 不承担 schema DDL。

### Existing tenant — #21 / PR #36

```text
read-only semantic preflight
→ BASELINE_READY / DRIFTED / CONFLICT / ERROR
→ exact fingerprint authorization
→ advisory lock + recheck
→ baseline 2026081906
→ migrate/validate 2026082001
→ audit/state
```

Drift/Conflict 不自动 ALTER/DROP。

测试 tenant（如 `dingding`、`pingduoduo`、`yuansheng`）可按需要清理重建；当前不再作为 Workbench 功能实现阻塞项。

---

## 8. CI / Validation

Backend PR Validation：

```text
mvn -B -ntp -pl server -am -DskipTests compile
mvn -B -ntp -pl server -am test
```

数据库 integration tests 持续覆盖 runtime、baseline、provisioning、preflight。

#13 新增纯单元测试，不依赖测试 tenant 数据。

---

## 9. 当前风险

1. #6 未完成，GitHub 尚未强制 master required checks；
2. #9 未完成，Electron/Web 缺独立 PR build gate；
3. 默认 `ddl-auto=update` 尚未切到 validate；
4. #22 Maven duplicate dependency warnings 尚未清理；
5. legacy schema sync write path 仍存在；
6. Workbench protocol actual emission 仍受 client-first gate；
7. `FileResource` 当前缺少完整业务资源级 ownership 语义，#13 只建立 adapter boundary，Task/Approval 必须在资源层继续授权；
8. Workbench permission foundation 当前只提供最小 company-member baseline，管理权限需要后续 RBAC/领域 policy 扩展。

---

## 10. Roadmap

### P0 Governance

#6 / #7 / #9 / #22。

### P1 Workbench Platform

```text
#13 Workbench Platform Foundation — PR #38
        ↓
Overview
        ↓
Task Backend
```

### P1 Workbench Business

Overview → Task Backend → Task Web/Electron → Task Realtime/Push/Card → Approval Backend → Approval Web/Electron。

### P2 OA Expansion

Calendar → Announcement → Android OA → Report / AI Office。

---

## 11. Change Log

### 2026-08-20 — Issue #13 / PR #38

状态：`IN_PROGRESS / IMPLEMENTATION READY FOR CI`。

- Workbench common package boundary；
- authenticated CurrentWorkContext；
- stable Workbench error codes；
- fail-closed permission foundation；
- Organization/File adapters；
- Audit primitives；
- explicit background TenantExecutor；
- context/tenant/permission unit tests；
- 无新数据库 migration，无 Task/Approval 实现。

### 2026-08-20 — Issue #12 — Tenant Versioned Migration Epic

状态：`COMPLETED`。#18/#19/#26/#32/#25/#20/#21 已完成；core baseline `2026081906`，managed target `2026082001`。

---

项目原则：

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
