# Tenant Schema Migration — Current Design

> 状态：CURRENT / CANONICAL  
> ADR：`ADR-0004-versioned-tenant-schema-migrations.md`  
> Epic：#12 — COMPLETED  
> Core baseline：`2026081906`  
> Managed tenant target：`2026082001`  
> 当前兼容增强：#43 — core baseline fingerprint scope

---

## 1. Authority

- PostgreSQL schema 是 tenant boundary；
- `<tenant>.flyway_schema_history` 是 tenant migration/version authority；
- Git 中 immutable Flyway migrations 是 tenant DDL 发布来源；
- `spring.flyway.enabled=false`，普通应用启动不 migrate-all；
- new tenant 由 #20 migration-backed provisioning 创建；
- non-empty/no-history tenant 由 #21 read-only preflight + explicit baseline 接管；
- legacy public clone / Safe Sync 仅为 deprecated transitional compatibility，不是 migration authority。

---

## 2. Version Contract

```text
core business baseline = 2026081906
managed current target = 2026082001
```

`2026081906` 固定 18 core tables、3 identity views、17 core-owned identity sequences 及其 column/constraint/index contract。

`2026082001` 是 baseline 后第一个非破坏性 managed migration。未来 Workbench/OA table 必须继续追加新的 immutable tenant migrations。

---

## 3. Core Baseline Fingerprint vs Full Inventory — #43

原 #21 fingerprint 在 baseline 建立时覆盖 tenant 当时的全部 table/view/sequence。这在 `1906` 接管阶段安全，但会导致合法后续 Flyway table（例如 `wb_task`）进入 schema 后被错误视为 baseline conflict/drift。

#43 明确分离两个概念：

### Immutable core fingerprint

只参与 pinned SHA-256 的对象：

- `CoreTenantBaselineContract.CORE_TABLES`；
- core tables 的 columns；
- core tables 上的 constraints；
- core tables 的 indexes；
- `company/company_user/users` identity views；
- 由 core table serial/identity columns owned 的 sequences。

同 tenant FK referenced schema 继续归一化为 `<tenant>`；public/global reference 继续保留 `public`。

六组 canonical hashes **不因后续 managed table/view/sequence 增加而改变**。

### Full object inventory

Fingerprint result 同时保留：

- all application tables（排除 Flyway history / tenant metadata）；
- all views；
- all sequences。

它只用于 adoption safety，不进入 pinned core hash。

---

## 4. Existing Tenant Classification

### No Flyway history

这是“准备被 baseline 接管”的 legacy tenant，必须严格：

```text
core object complete
+ core pinned hashes match
+ identity isolation behavior match
+ no extra table/view/sequence
= BASELINE_READY
```

任何 baseline 之外的未知 object 都是 `CONFLICT`；不会被 blind baseline 吞掉。

### Existing Flyway history

这是已经受管的 tenant：

- 仍验证 immutable core fingerprint；
- 合法 later managed table/view/sequence 不参与 core hash，不产生 false conflict；
- 仍禁止重复 baseline；
- 后续使用 normal Flyway APPLY / validate。

因此 baseline preflight 不再承担“当前最新业务 schema 全量 validator”的职责。最新 managed schema 的完整性由 Flyway history/checksum/pending/validate 和各 migration integration test 负责。

---

## 5. Explicit Baseline Safety

```text
read-only preflight
→ BASELINE_READY
→ exact expectedFingerprint
→ tenant advisory lock
→ re-preflight
→ require no history
→ Flyway.baseline(2026081906)
→ migrate current target
→ Flyway.validate
→ post-check immutable core fingerprint
→ audit + tenant state
```

- 禁止 `baselineOnMigrate(true)`；
- drift/conflict 不自动 ALTER/DROP；
- public 当前结构不充当 expected contract；
- baseline fingerprint stale 时拒绝执行；
- existing history 永远不能重复 baseline。

---

## 6. PostgreSQL Test Evidence

`CoreTenantBaselineContractFingerprintTest` 必须证明：

- canonical migrations 产生原有 pinned hashes；
- 添加 later table + identity sequence + view 后，core hashes/fingerprint 完全不变；
- full inventory 能看到 later objects。

`ExistingTenantBaselineIntegrationTest` 必须证明：

- compliant no-history tenant 可 baseline；
- no-history tenant + unknown later-like table => `CONFLICT`；
- core column drift => `DRIFTED`；
- missing core table => `CONFLICT`；
- managed history tenant + later object => core baseline 仍匹配；
- repeat baseline with existing history 仍拒绝；
- audit state 保持可见。

---

## 7. Workbench Migration Gate

Task Backend 的 `wb_task*` migration 只有在 #43 合并、上述 PostgreSQL tests 绿色后才能进入 master。

之后 tenant target 可从 `2026082001` 向新的 Workbench migration version 前进；core business baseline `2026081906` 不随之改变。
