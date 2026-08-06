# DeepSeek 配置

服务端现在可以通过 OpenAI 兼容接口调用 DeepSeek，并在同时配置多个提供商时优先使用 DeepSeek。

## GitHub Secret

在仓库 Settings → Secrets and variables → Actions 中增加：

- `DEEPSEEK_API_KEY`：DeepSeek API Key

部署工作流需要将 Secret 注入服务端运行环境：

```yaml
env:
  SPRING_AI_ENABLED: "true"
  DEEPSEEK_API_KEY: ${{ secrets.DEEPSEEK_API_KEY }}
```

也可以使用 Spring 标准环境变量：

```yaml
env:
  SPRING_AI_ENABLED: "true"
  SPRING_AI_DEEPSEEK_API_KEY: ${{ secrets.DEEPSEEK_API_KEY }}
```

可选配置：

```yaml
env:
  DEEPSEEK_BASE_URL: "https://api.deepseek.com"
```

不要把 API Key 写入 `application.yml`、Dockerfile、日志或客户端代码。API Key 只应存在于服务端部署环境。

## 本地运行

```bash
export SPRING_AI_ENABLED=true
export DEEPSEEK_API_KEY=your-key
./gradlew :server:bootRun
```

如果项目使用 Maven，请按照当前服务端启动方式运行，并保留相同环境变量。

## 提供商优先级

当多个 Key 同时存在时，默认顺序为：

1. DeepSeek
2. Groq
3. OpenAI

当 `SPRING_AI_ENABLED=true` 但没有任何有效 Key 时，应用会在启动阶段明确报错，而不是创建一个使用占位 Key 的客户端。
