# 部署运维文档导航

> 状态：ACTIVE INDEX  
> 最后更新：2026-08-19

本目录包含 Group-IM 的部署、CI/CD、数据库迁移和日常运维文档。

历史设计文档不会因为保留在仓库中自动成为当前实现真相。涉及当前项目状态时，优先查看 `doc/PROJECT_MASTER.md`；涉及长期架构决策时，优先查看 Accepted ADR。

## 📚 文档列表

### Tenant Schema / 数据库迁移

**当前 canonical design：**

- [ADR-0004：Versioned Tenant Schema Migrations](../architecture/adr/ADR-0004-versioned-tenant-schema-migrations.md)
- [Tenant Schema Migration — Current Design](../architecture/tenant-migration/README.md)

**历史设计输入：**

- [租户Schema版本与迁移机制方案.md](./租户Schema版本与迁移机制方案.md)
- [租户Schema迁移代码设计.md](./租户Schema迁移代码设计.md)

这两份历史文档记录了 Flyway、控制面、管理 API 等早期设计，仍有参考价值，但状态为 **HISTORICAL DESIGN INPUT**。如果内容与 ADR-0004 / Current Design 冲突，以新的 canonical design 为准。

当前 Migration Epic：

- #12 — Tenant Migration Epic
- #18 — Architecture Contract / ADR-0004
- #19 — Migration Runtime
- #20 — New Tenant Provisioning
- #21 — Existing Tenant Baseline / Validate

关键修正规则：

1. 不在普通应用启动时无条件迁移全部 tenant；
2. Safe Sync 不负责创建新业务表；
3. 旧 tenant 禁止 blind `baselineOnMigrate(true)`；
4. #19 不直接把生产 `ddl-auto=update` 切为 `validate`；
5. 新 tenant 的 public clone 在 #20 前属于兼容路径，#20 后由版本化 migration 接管；
6. public/control-plane bootstrap 也是显式 migration operation。

### CI/CD 标准流程

- **[CI/CD 标准流程.md](./CI-CD 标准流程.md)**
  - GitHub Actions 配置
  - Docker 镜像构建
  - 自动化部署策略
  - 回滚方案
  - 监控告警

仓库治理和 Required Checks 的当前状态以 `PROJECT_MASTER.md`、`.github/workflows/` 和对应 Issue 为准。

### 阿里云部署指南

- **[阿里云部署指南.md](./阿里云部署指南.md)**
  - 服务器购买配置
  - 系统初始化
  - 项目部署步骤
  - Nginx 配置
  - 性能优化
  - 故障排查

### 宝塔面板配置手册

- **[宝塔面板配置手册.md](./宝塔面板配置手册.md)**
  - Docker 管理器使用
  - 网站管理配置
  - 数据库管理
  - 监控告警设置
  - 安全管理
  - 常用工具

## 🚀 快速开始

### 新手推荐阅读顺序

1. `doc/PROJECT_MASTER.md` — 先理解当前真实状态；
2. **阿里云部署指南** — 环境和部署架构；
3. **CI/CD 标准流程** — 自动化构建/发布；
4. **宝塔面板配置手册** — 日常运维；
5. 涉及 tenant DDL 时必须先阅读 ADR-0004 和 Tenant Migration Current Design。

### 场景化阅读

#### 首次部署

```text
PROJECT_MASTER
   ↓
阿里云部署指南
   ↓
CI/CD 标准流程
   ↓
运行环境验证
```

#### 修改数据库结构

```text
Issue
  ↓
ADR-0004 / Tenant Migration Current Design
  ↓
Versioned Migration
  ↓
Backend CI + Integration Test
  ↓
PR + PROJECT_MASTER
```

禁止以手工修改单个 tenant schema 作为正常发布流程。

#### 日常运维

```text
宝塔面板配置手册
   ↓
日志 / 监控
   ↓
数据库和租户状态检查
```

#### 故障排查

```text
PROJECT_MASTER / Known Risks
   ↓
对应 Feature / Architecture Design
   ↓
日志与 CI
   ↓
Issue
   ↓
Fix PR
```

## 📋 文档优先级

如果仓库中的说明发生冲突：

```text
PROJECT_MASTER 当前事实
        ↓
Accepted ADR
        ↓
Canonical Feature / Architecture Design
        ↓
已合并代码实现
        ↓
Historical Design Docs
```

历史文档应渐进收敛，不进行无业务价值的大规模一次性搬家。

## 🔗 相关入口

- GitHub Repository：`pengpengno/Group-IM`
- 项目主文档：`doc/PROJECT_MASTER.md`
- 仓库治理：`doc/development/REPOSITORY_GOVERNANCE.md`
- 架构决策：`doc/architecture/adr/`
- Workbench：`doc/features/workbench/`

---

**最后更新**：2026-08-19  
**维护方式**：所有更新通过 Issue / PR，并同步 `PROJECT_MASTER.md`。
