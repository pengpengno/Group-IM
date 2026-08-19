# ADR-0001: `master` 作为唯一开发主线

- Status: Accepted
- Date: 2026-08-19
- Related Issue: #4

## Context

Group-IM 当前同时存在 `master` 与 `main`。检查发现两者没有共同祖先：

- `master` 承载当前实际开发历史；
- `main` 仅保留早期 Initial Commit 历史；
- 仓库默认分支为 `master`；
- 现有部署 workflow 同时监听 `main` 与 `master`，容易造成语义混乱。

随着项目进入多模块并行开发阶段，继续保留两个“看起来都像主分支”的入口会增加 PR 基线错误、部署误触发、文档引用混乱和维护成本。

## Decision

1. `master` 是 Group-IM 唯一开发主线、PR 合并目标和后续生产构建基线；
2. `main` 定义为历史遗留分支，不再用于开发、发布或新 PR；
3. 不尝试直接合并 `main` 与 `master`，因为二者没有共同祖先；
4. 所有变更必须通过分支 + Pull Request 进入 `master`；
5. 治理机制落地后，为 `master` 启用 Branch Protection / Ruleset；
6. 后续通过独立 Issue/PR 将部署 workflow 中的 `main` trigger 移除；
7. 是否最终删除 `main` 由后续治理 Issue 单独决定，在确认无历史引用依赖前不贸然删除。

## Consequences

### Positive

- PR 基线唯一；
- 发布/部署语义清晰；
- 文档与自动化规则可以统一围绕 `master`；
- 降低误从 `main` 开分支或误部署的风险。

### Negative

- 部分旧链接/脚本若仍引用 `main`，需要逐步修复；
- `main` 在正式归档/删除前仍会在分支列表中出现，需通过文档和流程避免误用。

## Alternatives Considered

### 将 `master` 迁移为 `main`

不采用。当前 `master` 已承载真实开发历史，而现有 `main` 与其无共同祖先。强行迁移会增加历史和部署风险，没有足够收益。

### 保留两个长期主线

不采用。项目规模和多端协作已不适合维护双主线语义。

## Follow-ups

- Issue #4：落地治理文档、模板和 Governance CI；
- 后续 Issue：启用 `master` 分支保护；
- 后续 Issue：部署 workflow 移除 `main` trigger；
- 后续评估：是否归档/删除历史 `main`。
