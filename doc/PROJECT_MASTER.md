# Group-IM 项目主设计与状态文档

> **Single Source of Truth / 项目唯一事实入口**

- 文档状态：ACTIVE
- 基线日期：2026-08-20
- 唯一开发主线：`master`
- 最近完成：#45 / PR #46 Task Backend；#9 / PR #48 Electron/Web CI；#47 / PR #49 Task Web/Electron
- 当前交付：#28 Workbench structured-card / ClientEvent protocol foundation
- 当前 emission blockers：#29 Web/Electron card/deep-link；#30 Android card/deep-link；#50 WORKBENCH message storage/core evolution
- 仓库：`pengpengno/Group-IM`

---

## 1. 项目原则与治理

Group-IM 是多租户组织协作 IM/OA 平台。消息是协作主链路，Workbench 承载结构化办公，AI/Automation 不建立第二份业务真相。

- `master` 是唯一开发主线；`main` 是 legacy；
- 所有变更通过 Issue/PR；每个 PR 更新本文；默认 Squash Merge；
- Workbench 权限禁止 `username == admin`；
- feature 文件更新必须明确目标 branch；
- merge gate：Repository Governance + applicable Backend + applicable Electron/Web + KMP + no unresolved review threads。

仍待治理：#6 master protection、#7 legacy `main` deploy trigger、#22 Maven duplicate dependencies。

---

## 2. Tenant Migration

```text
core business baseline = 2026081906
managed current target = 2026082002
```

当前事实：

- #12 versioned tenant migration foundation 完成；
- #43 将 immutable core baseline fingerprint 与 later managed object inventory 分离；
- no-history tenant extra object 仍 `CONFLICT`；
- Flyway-managed later tables/sequences 不污染 core baseline hash；
- new tenant：inactive → empty schema → Flyway current target → verify → active；
- existing reviewed tenant：preflight → explicit baseline 1906 → migrate current target → validate/audit；
- `2026082002` 增加 `wb_task / wb_task_assignee / wb_task_comment / wb_task_activity`；
- core baseline 1906 不随 managed target 前移；
- test tenant 数据可清理重建，不阻塞 Workbench 开发。

### WORKBENCH message storage gate — #50

`2026081906` 的 `messages_type_check` 当前允许：

```text
TEXT / FILE / VOICE / VIDEO / IMAGE / MEDIA / MEETING / BOT_CARD
```

该 CHECK 属于 pinned core constraint fingerprint。#28 不直接 ALTER 它，因为直接加入 WORKBENCH 会让合法 managed tenant 被现有 baseline fingerprint 识别为 core drift。

#50 负责建立 versioned managed-core evolution 机制，并在 actual WORKBENCH Message persistence/emission 前通过后续 immutable migration 扩展 CHECK。

因此当前明确：

```text
MessageType.WORKBENCH protocol recognition != WORKBENCH persistence enabled
```

---

## 3. Workbench Platform / Overview

### #13 Platform Foundation — COMPLETED

提供：

- `CurrentWorkContext`；
- stable Workbench errors；
- fail-closed permission service；
- Organization/File adapters；
- Workbench Audit；
- explicit tenant executor。

### #39 Overview — COMPLETED

`GET /api/workbench/overview`：

- tenant 只来自 authenticated current company；
- response 不暴露 schemaName；
- today Meeting 使用轻量 projection；
- Task 上线后接入真实 assigned/overdue/recent Task；
- 无 Overview write model/cache。

---

## 4. Task — Backend + Web/Electron

### #45 / PR #46 Task Backend — COMPLETED

Merge：`6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

能力：

- Task 四表 migration `2026082002`；
- create/update/list/detail；
- start/block/resume/complete/reopen/cancel；
- assignee / comments / activity；
- resource permission；
- Activity + Workbench Audit；
- Overview Task projection。

状态机：

```text
TODO -> IN_PROGRESS / COMPLETED / CANCELLED
IN_PROGRESS -> BLOCKED / COMPLETED / CANCELLED
BLOCKED -> IN_PROGRESS / CANCELLED
COMPLETED -> IN_PROGRESS
CANCELLED -> terminal
```

### #47 / PR #49 Task Web/Electron — COMPLETED

Merge：`b590bbd6a18737b504d0c3876a8d12831e3efe15`。

Workbench 现在读取真实 Overview，并在 Workbench shell 内提供 Task Center：

```text
Overview
→ create / list / detail
→ start / block / resume / complete / reopen / cancel
→ comment / activity
→ back + refresh Overview
```

Task 是 Workbench 子视图，不扩散为 Dashboard 顶级 tab。客户端不维护 Task 第二份持久真相；服务端仍是权限/状态权威。

---

## 5. Electron/Web CI — #9 COMPLETED

PR #48 merge：`d5570e0145a549a4869118b957a059a857e0d6ee`。

`.github/workflows/electron-web.yml` 对 Electron/Web 相关 PR 执行：

```text
npm ci
production desktop env
npm run app:build
npm run web:build
```

首次自验证以及 #47 Task Web/Electron 均已通过 Electron main/renderer + Web production bundle。

---

## 6. Workbench Notification Protocol — #28 CURRENT

正式文档：

- `doc/features/workbench/notification-protocol.md`
- `doc/architecture/adr/ADR-0005-workbench-structured-card-and-event-protocol.md`

#28 建立协议基础，**不启用任何 Task/Approval domain emission**。

### MessageType

Java enum append：

```text
WORKBENCH
```

Proto append-only wire contract：

```text
TEXT      = 0
FILE      = 1
VIDEO     = 3
VOICE     = 4
IMAGE     = 6
MEETING   = 7
BOT_CARD  = 8
WORKBENCH = 9
```

旧编号禁止重排。

Java ↔ Proto mapping 同步覆盖 MEETING / BOT_CARD / WORKBENCH；`MEDIA` 继续沿用既有 TEXT fallback，直到有独立 wire contract。

### Envelope

已建立：

```text
WorkbenchCategory
WorkbenchTarget
WorkbenchCardEnvelope
WorkbenchEventEnvelope
WorkbenchProtocol
WorkbenchCardSerializer
WorkbenchEnvelopeValidator
WorkbenchDeepLinkFactory
WorkbenchNotificationPolicyKey
```

V1 category：

```text
TASK / APPROVAL / ANNOUNCEMENT / SCHEDULE / REPORT
```

`companyId` 仅是 routing hint；schemaName 不进入 envelope；Deep Link 不能作为授权凭证。

### ClientEvent

新增 coarse type：

```text
WORKBENCH_RESOURCE_EVENT
```

不为每个 Task/Approval action 新增顶层 event type。

### Rollout gate

```text
#28 protocol
→ #29 Web/Electron renderer/deep-link
→ #30 Android renderer/deep-link
→ #50 message storage/managed-core evolution
→ supported-client/min-version rollout gate
→ actual server WORKBENCH emission
```

在最后一步前，server 不得持久化或发射 `MessageType.WORKBENCH`，也不做 TEXT + WORKBENCH 双写。

---

## 7. Client-first Follow-ups

### #29 Web/Electron — OPEN

必须实现：

- V1 card parser/renderer；
- unknown version/category/action fallback；
- malformed JSON fallback；
- tenant-aware `group://workbench/...?...companyId=`；
- authenticated company switch；
- switch 后重新 fetch current resource；
- no direct approve/complete from untrusted card payload；
- BOT_CARD / MEETING regression。

Electron/Web CI 已具备。

### #30 Android/KMP — OPEN

必须实现：

- V1 parser/model；
- Compose Workbench card；
- safe unknown/malformed fallback；
- tenant-aware deep-link router；
- authenticated company switch + resource fetch；
- BOT_CARD / MEETING regression；
- KMP APK CI green。

---

## 8. 模块状态

| 模块 | 状态 | 当前事实 | 下一步 |
| --- | --- | --- | --- |
| Workbench Platform | STABLE | #13 完成 | 领域复用 |
| Workbench Overview | STABLE | #39 + Task projection | events refresh |
| Tenant Migration | STABLE | baseline 1906 / target 2002 | #50 core evolution |
| Task Backend | STABLE | #45 / PR #46 | notification integration after gates |
| Task Web/Electron | STABLE | #47 / PR #49 | #29 card/deep-link |
| Electron/Web PR CI | STABLE | #9 / PR #48 | #6 required check |
| Workbench Protocol | IN_PROGRESS | #28 envelope/proto/event | CI / merge |
| Workbench Web Card | PLANNED | #29 open | after #28 |
| Workbench Android Card | PLANNED | #30 open | after #28 |
| WORKBENCH Message Storage | BLOCKED | #50 open | before emission |
| OA Approval | PLANNED | design complete | Task notification/client gates后 |

---

## 9. CI / Merge Gates

当前 checks：

- Repository Governance；
- Backend PR Validation；
- Electron Web PR Validation（applicable paths）；
- Build KMP APK；
- no unresolved review threads。

#6 仍负责配置 master required checks。

---

## 10. Roadmap

```text
#13 Platform Foundation ✅
→ #39 Overview API ✅
→ #43 Baseline Scope ✅
→ #45 Task Backend ✅
→ #9 Electron/Web CI ✅
→ #47 Task Web/Electron ✅
→ #28 Workbench Protocol ← CURRENT
→ #29 Web/Electron Card + Deep Link
→ #30 Android Card + Deep Link
→ #50 WORKBENCH Message Storage / Managed Core Evolution
→ Task Realtime / Push / optional IM Card
→ Approval Backend / UI
→ Calendar / Announcement / Android OA / Report / AI Office
```

---

## 11. Change Log

### 2026-08-20 — #28 Workbench Protocol

状态：IN_PROGRESS。Java/Proto WORKBENCH enum、V1 envelope、validator、deep link、ClientEvent contract 已进入工作分支；actual domain emission 继续禁用，并由 #29/#30/#50 rollout gates 阻塞。

### 2026-08-20 — #50 WORKBENCH storage gate

状态：OPEN。解决合法 managed core constraint evolution 与 immutable 1906 baseline fingerprint 的兼容问题。

### 2026-08-20 — #47 / PR #49

状态：COMPLETED，merge `b590bbd6a18737b504d0c3876a8d12831e3efe15`。

### 2026-08-20 — #9 / PR #48

状态：COMPLETED，merge `d5570e0145a549a4869118b957a059a857e0d6ee`。

### 2026-08-20 — #45 / PR #46

状态：COMPLETED，merge `6ff1d1f99567e9d203ca1e27e20c47db24551d62`。

### 2026-08-20 — #43 / PR #44

状态：COMPLETED，merge `8384ebd69ef203b661f89db98b2169d35b06ec7e`。

### 2026-08-20 — #39 / PR #40

状态：COMPLETED，merge `979f1f9a0efe407efe4981f3880835d40490f9f5`。

### 2026-08-20 — #13 / PR #38

状态：COMPLETED，merge `4c7f3e29379ec44013e7e78d15edece6d6b1a924`。

> Issue 描述为什么做，PR 描述怎么做，代码描述实际怎么运行，PROJECT_MASTER 描述项目现在是什么。
