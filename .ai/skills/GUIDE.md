# AI Skills Guide

This directory contains skill definitions for AI agents working with this project.

## 🤖 For AI Agents

### Available Skills

1. **Documentation Writer** (`documentation-writer.yaml`)
   - Expert in creating technical documentation
   - Follows MECE principle and user-oriented design
   - Ensures accuracy, completeness, and readability

2. **CI/CD Expert** (`cicd-expert.yaml`)
   - Designs and implements CI/CD pipelines
   - Configures GitHub Actions workflows
   - Integrates Docker and cloud deployment

3. **Deployment Assistant** (`deployment-assistant.yaml`)
   - Guides server setup and configuration
   - Manages Docker orchestration
   - Handles production deployment

### How to Use These Skills

1. **Read the skill file**: Load the YAML file to understand capabilities
2. **Check triggers**: Identify when to activate this skill
3. **Follow constraints**: Adhere to all requirements and prohibitions
4. **Use output format**: Generate content in specified format
5. **Apply best practices**: Follow recommended approaches
6. **Meet evaluation criteria**: Ensure output meets quality standards

### Skill Format

Each skill is defined in YAML format with:
```yaml
skill:
  name: "Skill Name"
  version: "1.0.0"
  description: "What this skill does"
  
  capabilities: [...]
  triggers: [...]
  constraints: [...]
  output_format: {...}
  examples: [...]
  tools: [...]
  best_practices: [...]
  evaluation_criteria: {...}
```

## 👥 For Developers

### Understanding AI Skills

Skills define what AI agents can do and how they should behave:

- **Capabilities**: What the AI can accomplish
- **Triggers**: When to use this skill
- **Constraints**: Rules the AI must follow
- **Output Format**: How results should be structured
- **Examples**: Sample use cases

### Creating Custom Skills

1. Copy `template.yaml`
2. Define your skill's capabilities
3. Set appropriate triggers
4. Specify constraints and requirements
5. Provide clear examples
6. Save as `<skill-name>.yaml`

### Compatible AI Agents

These skills are designed to work with:
- ✅ Claude (Anthropic)
- ✅ GPT-4 / GPT-4o (OpenAI)
- ✅ GitHub Copilot
- ✅ Cursor AI
- ✅ Codeium
- ✅ Any AI that understands YAML configurations

## 📚 Skill Files

| File | Purpose | Audience |
|------|---------|----------|
| `template.yaml` | Standard skill template | All AI agents |
| `documentation-writer.yaml` | Technical writing guidelines | Documentation AI |
| `cicd-expert.yaml` | CI/CD configuration guide | DevOps AI |
| `deployment-assistant.yaml` | Server deployment guide | Deployment AI |
| `README.md` | Human-readable guide | Humans & AI |

## 🔧 Maintenance

### Updating Skills

When updating a skill:
1. Increment version number (semver)
2. Update `created_at` date
3. Document changes in version history
4. Test with AI agents
5. Gather feedback and iterate

### Best Practices

- Keep skills focused and specific
- Use clear, unambiguous language
- Provide concrete examples
- Include both positive and negative constraints
- Regular review and updates

## 📝 Example Usage

### Scenario: Create Deployment Documentation

**User Request**: "Help me deploy to Alibaba Cloud using Baota panel"

**AI Agent Actions**:
1. Activate `deployment-assistant.yaml` skill
2. Follow capability: `cloud_server_setup`
3. Execute triggers: match user request
4. Apply constraints: security first, verify each step
5. Use output format: markdown guide
6. Reference examples: similar deployment scenarios

**Expected Output**:
- Complete deployment guide
- Step-by-step instructions
- Configuration files
- Troubleshooting tips
- Security recommendations

## 🎯 Quality Standards

All skills must ensure:
- ✅ Accuracy: Verified information
- ✅ Completeness: No missing steps
- ✅ Readability: Clear structure
- ✅ Safety: Security best practices
- ✅ Maintainability: Easy to update

---

**Maintained by**: Group IM AI Team  
**Last Updated**: 2025-03-12  
**Version**: v1.0

## 📖 Related Documentation

- Human-readable guide: `README.md`
- Skill template: `template.yaml`
- Project docs: `../doc/`
