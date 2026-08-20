# Workbench Task Web/Electron V1

> 状态：IMPLEMENTING  
> Issue：#47  
> Depends on：#45 / PR #46  
> Frontend：`Group-app/Group-Electronjs/renderer/features/workbench`

## 1. 目标

让 Web/Electron 用户在不借助 Postman 的情况下完成 Task 核心闭环：

```text
Workbench Overview
→ Task Center
→ create
→ list / detail
→ start / block / resume / complete / reopen / cancel
→ comment / activity
→ back to refreshed Overview
```

Task 的唯一业务真相仍在 server；客户端只保存当前页面展示所需的短生命周期状态。

## 2. 路由边界

Task 是 Workbench OA 子模块，不升级为 Dashboard 顶级导航 tab。

```text
Dashboard activeTab = workbench
        ↓
Workbench local view
  ├── overview
  └── tasks
```

这样保持 Workbench 作为 OA shell，避免把每一个 OA 领域都扩散到全局导航枚举。

Meeting / Contacts / Automation / Settings 仍使用现有 `onNavigate(ActiveTab)` 进入既有模块。

## 3. HTTP contract

新增独立 typed client：

```text
renderer/services/api/workbenchAPI.ts
```

它复用现有 `BASE_URL` 和 localStorage Bearer token 约定，不接受或注入 tenant companyId。

Workbench 新代码严格使用后端统一响应：

```ts
interface ServerApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}
```

不继续沿用旧 Electron `success/data/error` 形状去猜 Workbench API。

## 4. Overview UI

`GET /api/workbench/overview` 驱动：

- current company；
- assigned task count；
- overdue task count；
- pending approval count；
- unread announcement count；
- recent tasks；
- today schedules；
- quick apps。

Overview 不建立本地缓存或复制业务状态。进入 Task Center 修改后，返回 Overview 时重新请求服务端。

## 5. Task Center

### List

```http
GET /api/workbench/tasks?limit=100
```

只显示后端已经按当前用户资源权限过滤的任务。

### Detail

```http
GET /api/workbench/tasks/{taskId}
GET /api/workbench/tasks/{taskId}/activities
```

详情展示：

- title / description；
- status / priority；
- owner / creator；
- dueAt / progress；
- assignees；
- comments；
- activity timeline。

### Create

首版创建表单只暴露已稳定、无需额外 selector 的字段：

- title；
- description；
- priority；
- ownerId（可空，直接 ID）；
- dueAt。

Department / conversation / attachment selector 不在本 PR 伪造。

### Actions

客户端只按状态机展示语义上可行的动作：

```text
TODO        -> start / complete / cancel
IN_PROGRESS -> complete / block / cancel
BLOCKED     -> resume / cancel
COMPLETED   -> reopen
CANCELLED   -> none
```

这只是 UX 提示。后端 `TaskAccessPolicy + TaskStateMachine` 仍是最终权限和状态权威；服务端拒绝时直接显示 error message。

### Comments

```http
POST /api/workbench/tasks/{taskId}/comments
```

成功后重新读取 detail/activity，不在客户端拼接“假成功”记录。

## 6. 状态与错误

页面显式处理：

- initial loading；
- empty list；
- detail loading；
- mutation busy；
- API error + retry/close；
- create modal validation。

公司切换继续使用现有 Dashboard reload 行为，因此不会跨 tenant 保留 Task 页面状态。

## 7. Visual direction

延续现有 Workbench：

- 浅灰工作区背景；
- 深蓝 `#1e3a5f` header；
- 白色卡片；
- 蓝色主操作；
- status/priority 用轻量 badge；
- desktop 双栏 list/detail；
- 窄屏单栏退化。

不在 #47 做全局 redesign。

## 8. Out of scope

- Realtime / Push / WORKBENCH Card；
- #29 structured card renderer / deep link；
- Android Task UI；
- attachment relation；
- department selector；
- Task Redux/global persistent store；
- backend API semantic changes。

## 9. Validation

#9 / PR #48 已建立 Electron/Web PR CI。#47 merge 前必须通过：

```text
npm ci
production desktop env
npm run app:build
npm run web:build
```

以及 Repository Governance / KMP / no unresolved review threads。
