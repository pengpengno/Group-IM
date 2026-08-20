# Workbench Task Backend V1

> 状态：IMPLEMENTATION — Issue #45  
> 依赖：#13 Platform Foundation、#39 Overview、#43 Baseline Fingerprint Scope  
> Tenant target：`2026082002`

---

## 1. Data Model

Tenant migration `V2026082002__create_workbench_task_tables.sql` adds:

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

Tenant schema is the company boundary; rows do not repeat company_id. User/department/conversation are stored as IDs to avoid cross-module JPA coupling and to preserve historical references.

`wb_task` uses optimistic `@Version` and CHECK constraints for status/priority/progress.

---

## 2. State Machine

```text
TODO -> IN_PROGRESS / COMPLETED / CANCELLED
IN_PROGRESS -> BLOCKED / COMPLETED / CANCELLED
BLOCKED -> IN_PROGRESS / CANCELLED
COMPLETED -> IN_PROGRESS (reopen)
CANCELLED -> terminal
```

Status is not patchable through generic update. All status changes use action endpoints and are validated by `TaskStateMachine`.

`COMPLETE` sets progress=100 and completedAt. `REOPEN` clears completedAt. First transition into IN_PROGRESS sets startAt if absent.

---

## 3. API

Base: `/api/workbench/tasks`

```text
POST   /
PATCH  /{taskId}
GET    /
GET    /{taskId}
POST   /{taskId}/actions/{start|block|resume|complete|reopen|cancel}
POST   /{taskId}/assignees
DELETE /{taskId}/assignees/{userId}
POST   /{taskId}/comments
GET    /{taskId}/activities
```

Request never accepts companyId as tenant routing input.

---

## 4. Permission Model

Feature gate uses #13 `WorkbenchPermissionService`.

Resource permission:

- creator / owner: manage task, assignees, cancel, reopen;
- OWNER/COLLABORATOR assignee: work actions;
- WATCHER: view/comment only;
- creator/owner/any assignee: view;
- list query only returns creator/owner/assignee-visible tasks.

Owner/assignee must be an active member according to `OrganizationAdapter`.

No username-based admin bypass exists.

---

## 5. Activity + Audit

Every create/update/workflow/assignment/comment writes `wb_task_activity` and records a normalized Workbench audit event.

Notifications, realtime events and structured WORKBENCH cards are intentionally not emitted in this PR; they belong to the later after-commit delivery phase.

---

## 6. Overview Integration

`GET /api/workbench/overview` now reads Task truth:

- assignedTaskCount: OWNER/COLLABORATOR open tasks;
- overdueTaskCount: assigned open tasks with dueAt before now;
- recentTasks: most recently updated tasks visible to current user;
- Quick Apps includes `TASK`.

Approval/Announcement remain zero/empty until their domains are implemented.

---

## 7. Migration Compatibility

Core business baseline remains `2026081906`.

Managed tenant target advances:

```text
2026082001 -> 2026082002
```

#43 ensures `wb_task*` and their identity sequences are full-inventory later managed objects and do not alter the pinned core baseline fingerprint.

Existing no-history tenant with unknown extra objects remains ineligible for blind baseline.

---

## 8. Validation

Required tests:

- Task state-machine valid/invalid transitions;
- creator/owner/collaborator/watcher resource policy;
- Task service create/action behavior;
- Overview uses real Task projection;
- PostgreSQL migration creates four tables and validates CHECK constraints;
- new tenant provisioning reaches 2026082002 and contains wb_task;
- core baseline hashes stay unchanged after Task migration;
- migration runtime target/pending counts updated;
- Governance / Backend / KMP green.
