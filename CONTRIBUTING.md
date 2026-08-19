# Contributing to Group-IM

Group-IM 使用 **Issue-driven Pull Request** 作为固定协作方式。

## 核心原则

1. `master` 是唯一开发主线。
2. 禁止直接向 `master` 提交代码、配置、数据库和文档变更。
3. Bug 修复必须先建立 Issue。
4. 新功能、架构调整、数据库变更和重要重构原则上必须先建立 Issue。
5. 每个 PR 必须关联至少一个 Issue。
6. 每个 PR 必须同步更新 `doc/PROJECT_MASTER.md`。
7. 重大架构决策必须新增或更新 ADR。
8. 默认使用 Squash Merge。

## 推荐流程

```text
Issue
  ↓
feature/fix/chore/docs 分支
  ↓
实现 + 测试 + 文档
  ↓
Pull Request
  ↓
CI / Review
  ↓
Squash Merge -> master
```

## 分支命名

```text
feature/<issue>-<name>
fix/<issue>-<name>
refactor/<issue>-<name>
docs/<issue>-<name>
chore/<issue>-<name>
hotfix/<issue>-<name>
```

示例：

```text
feature/12-workbench-task
fix/27-task-notification
chore/4-repository-governance
```

## PR 标题建议

使用清晰、可搜索的 Conventional Commits 风格：

```text
feat(workbench): add task creation flow
fix(notification): prevent duplicate task push
chore(governance): establish repository workflow
docs(workbench): update approval design
```

## Issue 关联

修复类 PR 使用：

```text
Fixes #123
```

其他实现类 PR 至少使用：

```text
Related to #123
```

如果一个 PR 对应多个 Issue，应在 PR 描述中全部列出，并明确哪个会在合并后关闭。

## PROJECT_MASTER 更新要求

每个 PR 都必须修改 `doc/PROJECT_MASTER.md`。至少更新以下一项：

- 模块状态；
- 已实现功能；
- 未完成范围；
- Roadmap；
- 已知风险；
- 变更记录。

不要把所有详细设计都塞入 PROJECT_MASTER。详细内容应放在对应 Feature Design 或 ADR 中，再由 PROJECT_MASTER 链接过去。

## 测试要求

### Bug Fix

必须满足至少一项：

- 新增自动化回归测试；
- 补充已有测试覆盖；
- 如果当前架构无法自动测试，在 PR 中记录明确的人工复现与验证步骤，并创建后续测试债 Issue（如必要）。

### Feature

至少说明：

- 正常路径；
- 权限/异常路径；
- 多租户影响（如适用）；
- Android / Electron-Web 影响（如适用）。

## 数据库变更

数据库 PR 必须说明：

- schema/table/column/index 变化；
- tenant schema 影响；
- 迁移方式；
- 向前兼容性；
- 回滚/失败策略；
- 是否影响已有 tenant。

禁止依赖“启动后 Hibernate 自动改表”作为长期生产迁移方案。

## API / Protocol 变更

涉及 REST、WebSocket、Protobuf 或客户端共享协议时，PR 必须说明：

- 请求/响应或字段变化；
- 向后兼容性；
- Android 与 Electron/Web 是否同步；
- 老版本客户端行为。

## Architecture Decision Record

长期有效、影响多个模块或难以逆转的技术决策，应添加：

```text
doc/architecture/adr/ADR-xxxx-<title>.md
```

ADR 应说明 Context、Decision、Consequences、Alternatives。

## Definition of Done

- [ ] 已关联 Issue
- [ ] 范围单一且可验收
- [ ] 实现完成
- [ ] 验证/测试完成
- [ ] PROJECT_MASTER 已更新
- [ ] Feature Design 已更新（如适用）
- [ ] ADR 已更新（如适用）
- [ ] 数据库/API/协议影响已说明（如适用）
- [ ] CI 通过
- [ ] Review conversation 已解决
- [ ] 后续工作已有 Issue 或明确不需要

详细规则见：`doc/development/REPOSITORY_GOVERNANCE.md`。
