# Group-IM 仓库治理规范

本文档定义 Group-IM 的固定工程协作机制。若本文档与临时讨论冲突，以已合并到 `master` 的最新治理文档和 `doc/PROJECT_MASTER.md` 为准。

## 1. 主线策略

- 唯一开发主线：`master`。
- `main` 为历史遗留分支，不再作为开发、PR 或发布基线。
- 所有代码、配置、数据库、工作流和设计文档修改均通过 PR 进入 `master`。
- 禁止直接 push 到 `master`。

## 2. Issue 驱动

### 必须先建 Issue

以下工作必须先有 Issue：

- Bug 修复；
- 新业务功能；
- 架构调整；
- 数据库结构/迁移变化；
- 安全修复；
- 重要重构；
- CI/CD 和仓库治理变更。

纯错别字或极小文档修正也建议 Issue 驱动；如果未来需要设置例外，应通过治理 ADR 明确，而不是临时绕过。

### Issue 分层

建议使用：

- Epic：跨多个 PR 的大型目标；
- Feature：可拆分的业务能力；
- Task：具体实现任务；
- Bug：可复现缺陷；
- Architecture：设计决策或技术债；
- Governance：仓库/流程治理。

大型能力推荐：Epic -> Feature/Task -> PR。

## 3. 分支策略

```text
feature/<issue>-<name>
fix/<issue>-<name>
refactor/<issue>-<name>
docs/<issue>-<name>
chore/<issue>-<name>
hotfix/<issue>-<name>
```

分支必须从最新 `master` 创建。一个分支服务一个清晰工作项，避免长期堆积多个无关目标。

## 4. Pull Request 规则

每个 PR 必须：

1. 目标分支为 `master`；
2. 关联至少一个 Issue；
3. 描述 Why / What / Validation / Risks / Follow-ups；
4. 更新 `doc/PROJECT_MASTER.md`；
5. 运行并通过适用的 CI；
6. 说明数据库/API/协议/多端兼容影响；
7. 对未完成内容创建 Follow-up Issue。

修复类 PR 使用 `Fixes #N`；其他 PR 使用 `Related to #N` 或同等明确关联。

## 5. 主设计文档制度

`doc/PROJECT_MASTER.md` 是项目唯一事实入口。

它回答：

- 项目现在是什么；
- 每个模块现在处于什么状态；
- 当前重点 Roadmap；
- 已知风险；
- 哪些详细文档/ADR 是当前有效设计依据。

它不替代所有详细设计。

详细设计位置：

```text
doc/features/<feature>/
doc/architecture/
doc/architecture/adr/
doc/development/
doc/operations/
```

### 每个 PR 的主文档更新

至少更新：状态、已实现、未实现、风险、Roadmap 或变更记录中的一项。

Governance CI 将阻止未修改 `doc/PROJECT_MASTER.md` 的 PR 合并。

## 6. ADR

以下情况应新增 ADR：

- 影响多个模块；
- 数据模型/租户模型/权限模型等长期基础设计；
- 难以回滚；
- 团队未来很可能问“为什么这样做”。

模板见 `doc/architecture/adr/README.md`。

## 7. 合并策略

默认：Squash Merge。

原因：

- 一个 PR 对应 `master` 一个逻辑变更；
- 历史更清晰；
- 更容易回滚；
- Issue、PR、最终 commit 可一一映射。

除非有明确理由，不使用 merge commit 保留开发分支的零散中间提交。

## 8. Hotfix

生产紧急问题仍必须创建 Issue 和 PR。

允许：

```text
Bug Issue -> hotfix/<issue>-xxx -> 最小修复 PR -> CI -> merge -> deploy
```

“紧急”不意味着直接 push `master`。如确需 break-glass，必须事后创建 Incident/Issue、补 PR 等价审计记录并在 PROJECT_MASTER 记录；该例外应极少使用。

## 9. 推荐 Branch Protection

治理 PR 合并后，在 GitHub Settings -> Branches / Rulesets 为 `master` 启用：

- Require a pull request before merging；
- Require status checks to pass；
- Require conversation resolution；
- Block force pushes；
- Block branch deletion；
- Restrict direct pushes；
- 可选：Require branches to be up to date before merging。

当前以单人/少人开发为主时，可暂不强制 1 个他人 Approval，但必须保留 PR、CI 和可审阅 Diff。

建议 Required Check 至少包括：

- `Repository Governance / governance`
- Android build（涉及 Android 或全量时）
- 后端测试/编译（补齐后）
- Electron/Web build（补齐后）

## 10. Release / Deployment

目标链路：

```text
Issue -> PR CI -> merge master -> master build -> deploy/release
```

`main` 不应继续触发部署。后续单独 Issue 修改部署 workflow，使生产分支只接受 `master`。

## 11. 代码评审关注顺序

1. 正确性；
2. 安全与权限；
3. 多租户隔离；
4. 数据兼容/迁移；
5. API/协议兼容；
6. 测试；
7. 可维护性；
8. 文档和状态同步；
9. 风格。

## 12. 后续治理增强

建议后续分别建立 Issue：

- 补后端 PR CI；
- 补 Electron/Web PR CI；
- 移除 `main` 的 deploy trigger；
- 配置 CODEOWNERS（有稳定多人维护角色后）；
- 引入 label / milestone / release note 规范；
- 自动校验 PR 标题和 Issue 状态；
- 自动生成 PROJECT_MASTER 变更摘要或 release notes。
