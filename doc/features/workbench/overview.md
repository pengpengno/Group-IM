# Workbench Overview API

> 状态：IMPLEMENTATION — Issue #39 / PR #40  
> 依赖：#13 Workbench Platform Foundation — COMPLETED  
> Endpoint：`GET /api/workbench/overview`

Overview 是 Workbench 首页的轻量聚合读模型，不是独立业务写模型，也不是复制 Task/Approval/Meeting 真相的第二套数据库。

---

## 1. Response Contract

```json
{
  "currentCompany": {
    "companyId": 7,
    "name": "Acme"
  },
  "todoSummary": {
    "assignedTaskCount": 0,
    "overdueTaskCount": 0,
    "pendingApprovalCount": 0,
    "unreadAnnouncementCount": 0
  },
  "recentTasks": [],
  "pendingApprovals": [],
  "todaySchedules": [
    {
      "type": "MEETING",
      "resourceId": 99,
      "title": "产品同步会",
      "status": "SCHEDULED",
      "startsAt": "2026-08-20T15:00:00",
      "endsAt": null
    }
  ],
  "announcements": [],
  "quickApps": [
    {"key": "MEETING", "title": "会议"},
    {"key": "CONTACTS", "title": "通讯录"},
    {"key": "AUTOMATION", "title": "自动化"},
    {"key": "SETTINGS", "title": "设置"}
  ]
}
```

`schemaName` 不属于客户端契约。Tenant routing 仍由 authenticated current company 决定。

---

## 2. Permission / Tenant Flow

```text
GET /api/workbench/overview
→ WorkbenchPermissionService.require(VIEW_WORKBENCH)
→ CurrentWorkContext
→ current user/company tenant only
→ lightweight aggregate queries
```

客户端不能通过 query/body `companyId` 切租户。

---

## 3. Current Domain Sources

### Current company

来自 #13 `CurrentWorkContext`。

### Todo / Task / Approval / Announcement

对应真实领域尚未实现，因此首版明确：

```text
counts = 0
lists  = []
```

禁止创建临时 Todo 表、假数据或从聊天内容猜测待办。

### Today schedules

首版来源：现有 Meeting。

查询条件：

- 当前 authenticated user 是 participant；
- participant status in `INVITED / JOINED / LEFT`；
- `REJECTED` invitation 明确排除；
- meeting status in `SCHEDULED / ACTIVE`；
- `scheduledAt` 落在今天，或 scheduledAt 为空时 `startedAt` 落在今天；
- order by effective start time, meetingId。

查询通过 JPQL constructor projection 直接选取：

```text
meetingId
title
roomId
status
coalesce(scheduledAt, startedAt)
endedAt
```

`meeting_participants` 已有 `(meeting_id,user_id)` unique constraint，因此不依赖 `DISTINCT` 去重，也避免和 effective-start ORDER BY 产生数据库特定限制。

不加载完整 participants/conversation graph。

---

## 4. Time Semantics

现有 Meeting 使用 `LocalDateTime`，没有 user/company timezone 字段。

因此 #39 首版使用 `Clock.systemDefaultZone()` 形成服务器本地“今天”的 `[dayStart, dayEnd)` 查询窗口。这是对现有语义的显式继承，不是长期时区设计。

未来若引入 company/user timezone：

1. 先固定 timezone source；
2. 增加契约/兼容性测试；
3. 再切换 Overview date window；
4. 禁止静默改变历史 LocalDateTime 解释。

---

## 5. Quick Apps

服务端只返回稳定业务 key + display title，不返回 Web/Electron 路由路径，也不返回 tenant schema。

客户端负责把 `MEETING / CONTACTS / AUTOMATION / SETTINGS` 映射到自己的导航结构。

---

## 6. No Cache / No Write Model

首版不缓存，因为：

- 当前查询很轻；
- Task/Approval 尚未接入；
- company switch cache invalidation 尚未形成统一机制；
- 先保证租户正确性与契约稳定。

后续只有实际性能证据出现后才评估短 TTL cache 或 materialized projection。

---

## 7. Future Extension

当领域上线时：

```text
Task Backend
→ assignedTaskCount / overdueTaskCount / recentTasks

Approval Backend
→ pendingApprovalCount / pendingApprovals

Announcement
→ unreadAnnouncementCount / announcements

Calendar
→ todaySchedules += PERSONAL / TASK_DUE / APPROVAL_REMINDER
```

Overview service 只调用各领域 Query/Adapter，不直接操作完整领域 Entity。

---

## 8. Tests

#39 必须验证：

- `VIEW_WORKBENCH` 权限入口被调用；
- current company summary 由 context 决定；
- future domain projection 为 zero/empty；
- today Meeting 查询使用当前 userId；
- date window 正确；
- Meeting status 只包含 SCHEDULED/ACTIVE；
- Participant status 包含 INVITED/JOINED/LEFT 且排除 REJECTED；
- Meeting 映射为 `type=MEETING`；
- 空标题有安全 fallback；
- Controller 返回项目标准 `ApiResponse<T>`。
