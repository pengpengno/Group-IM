# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> Core baseline：`2026081906`  
> Managed tenant target：`2026082002`

## Authority

`<tenant>.flyway_schema_history` is the tenant DDL/version authority. Migrations are immutable and normal application startup does not migrate all tenants.

## Baseline vs Managed Target

```text
core baseline 2026081906
    ↓ explicit baseline for reviewed legacy tenant
2026082001 baseline-contract metadata
    ↓
2026082002 Workbench Task V1 tables
```

Core baseline never moves when later business migrations are added.

## Core Fingerprint Scope

#43 separates immutable core fingerprint from full object inventory:

- pinned hashes cover 1906 core tables/columns/constraints/indexes, identity views and core-owned sequences;
- all tenant tables/views/sequences are separately inventoried;
- no-history legacy tenant with extra objects => CONFLICT;
- Flyway-managed tenant may contain legitimate later managed objects without false baseline drift;
- existing history still cannot be baselined again.

## Task Migration — 2026082002

Adds tenant-local:

```text
wb_task
wb_task_assignee
wb_task_comment
wb_task_activity
```

No company_id is stored because schema is the tenant boundary. IDs reference users/departments/conversations logically; domain services validate access through Workbench platform adapters rather than cross-schema foreign keys.

## New Tenant

New company provisioning migrates an empty schema directly to current target `2026082002`, verifies it, then activates the company. A provisioned new tenant therefore includes core schema and Task V1 from day one.

## Existing Tenant

Reviewed no-history tenant:

```text
preflight core + inventory
→ explicit baseline 2026081906
→ migrate 2026082001
→ migrate 2026082002
→ validate
→ audit/state
```

Drift/conflict never receives destructive automatic DDL.

## Future Workbench Tables

Approval/Calendar/Announcement migrations continue after `2026082002`. Each migration must have PostgreSQL integration coverage and must not mutate the immutable core fingerprint contract accidentally.
