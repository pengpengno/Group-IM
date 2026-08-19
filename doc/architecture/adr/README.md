# Architecture Decision Records

ADR 用于记录长期有效、影响范围较大、未来需要解释“为什么这样设计”的架构决策。

## 文件命名

```text
ADR-0001-master-as-single-trunk.md
ADR-0002-tenant-schema-strategy.md
ADR-0003-workbench-modular-monolith.md
```

编号递增，不复用旧编号。

## 状态

推荐状态：

- Proposed
- Accepted
- Superseded
- Deprecated
- Rejected

## 模板

```markdown
# ADR-XXXX: 标题

- Status: Proposed
- Date: YYYY-MM-DD
- Related Issue: #123

## Context

为什么需要做这个决定？有哪些约束？

## Decision

最终选择什么方案？

## Consequences

带来哪些正面/负面影响？

## Alternatives Considered

考虑过哪些方案，为什么没有选择？

## Follow-ups

需要哪些实现 Issue / PR？
```

## 规则

- ADR 必须通过 PR 进入 `master`；
- ADR 所在 PR 必须更新 `doc/PROJECT_MASTER.md`；
- 已 Accepted 的 ADR 不直接重写历史结论；如设计发生重大变化，应新增 ADR 并把旧 ADR 标记为 Superseded；
- PR 评论中的架构讨论不能替代 ADR。
