# AI Skills for Group IM Project

这个目录包含 AI Agent 可以理解和使用的技能定义文件。

## 📚 技能列表

### 1. Documentation Writer (文档编写专家)
**文件**: `documentation-writer.yaml`  
**用途**: 帮助编写和整理项目文档  
**能力**:
- 根据项目代码生成 API 文档
- 创建部署指南和操作手册
- 维护文档结构和导航
- 确保文档符合规范

### 2. CI/CD Expert (CI/CD 专家)
**文件**: `cicd-expert.yaml`  
**用途**: 配置和优化 CI/CD 流程  
**能力**:
- 设计 GitHub Actions 工作流
- 配置 Docker 构建和部署
- 制定自动化测试策略
- 实现一键部署到阿里云

### 3. Deployment Assistant (部署助手)
**文件**: `deployment-assistant.yaml`  
**用途**: 指导服务器部署和运维  
**能力**:
- 阿里云 ECS 配置指导
- 宝塔面板操作指南
- Docker Compose 编排
- 监控告警配置

## 🤖 如何使用这些 Skill？

### 对于 AI Agent
1. 读取对应的 YAML/JSON 技能文件
2. 理解技能的目标、能力和约束
3. 按照技能定义执行任务
4. 遵循最佳实践和规范

### 对于开发者
1. 参考技能文件了解 AI 能力
2. 调用 AI 完成特定任务
3. 审查 AI 生成的内容
4. 提供反馈优化技能

## 📋 技能文件格式

每个技能文件包含:
```yaml
skill:
  name: 技能名称
  version: 版本号
  description: 技能描述
  
  capabilities:
    - 能力 1
    - 能力 2
    
  triggers:
    - 触发条件 1
    - 触发条件 2
    
  constraints:
    - 约束条件 1
    - 约束条件 2
    
  examples:
    - 使用示例 1
    - 使用示例 2
```

## 🔧 自定义技能

你可以根据需要创建新的技能文件:
1. 复制现有技能文件作为模板
2. 修改技能名称和描述
3. 定义能力和约束
4. 添加使用示例

---

**维护者**: AI Agent  
**最后更新**: 2025-03-12  
**版本**: v1.0
