# 系统配置说明

## 配置前缀统一

所有系统相关的配置已统一在 `group` 前缀下，方便管理和查找。

## 配置结构

### 1. 文件存储配置
```yaml
group:
  storage:
    type: local          # 存储类型：local, oss, s3 等
    path: /app/storage   # 文件存储路径
  file:
    upload:
      chunk-temp-path: /tmp/chunk-storage  # 分片上传临时路径
      base-path: uploads                   # 基础路径（相对路径）
```

### 2. 系统初始化配置
```yaml
group:
  system:
    initializer:
      enabled: true                      # 是否启用系统初始化
      default-company:
        name: public                     # 默认公司名称
        schema-name: public              # 数据库 schema 名称
        active: true                     # 是否激活
      admin-user:
        username: admin                  # 管理员用户名
        email: admin@example.com         # 管理员邮箱
        phone-number: 1234567890         # 管理员电话号码
        password: "12345"                # 管理员密码
```

### 3. AI 配置
```yaml
group:
  ai:
    enabled: false                       # 是否启用 AI 功能
    groq:
      api-key: ${GROQ_API_KEY}           # Groq API 密钥
      base-url: https://api.groq.com/openai/v1
    openai:
      api-key: ${OPENAI_API_KEY}         # OpenAI API 密钥
      base-url: https://api.openai.com/v1
```

### 4. 序列号生成配置
```yaml
group:
  sequence:
    mode: redis                          # 序列号生成模式：memory, redis, database
```

## 对应的配置类

### Java 配置类映射

1. **FileUploadProperties** - `group.file.upload`
   - 位置：`com.github.im.server.config.FileUploadProperties`
   - 用途：文件上传配置

2. **SystemInitializerProperties** - `group.system.initializer`
   - 位置：`com.github.im.server.config.sys.SystemInitializerProperties`
   - 用途：系统初始化配置

3. **AiProperties** - `group.ai`
   - 位置：`com.github.im.server.config.ai.AiProperties`
   - 用途：AI 功能配置

## Docker 环境变量配置示例

在 Docker Compose 或 Kubernetes 中使用时，可以通过环境变量覆盖配置：

```yaml
environment:
  # 文件存储配置
  - GROUP_STORAGE_PATH=/app/storage
  - GROUP_FILE_UPLOAD_CHUNK_TEMP_PATH=/tmp/chunk-storage
  - GROUP_FILE_UPLOAD_BASE_PATH=uploads
  
  # 系统初始化配置
  - SYSTEM_INITIALIZER_ENABLED=true
  - DEFAULT_COMPANY_NAME=public
  - DEFAULT_COMPANY_SCHEMA=public
  - DEFAULT_COMPANY_ACTIVE=true
  - ADMIN_USERNAME=admin
  - ADMIN_EMAIL=admin@example.com
  - ADMIN_PHONE_NUMBER=1234567890
  - ADMIN_PASSWORD=12345
  
  # AI 配置
  - SPRING_AI_ENABLED=false
  - GROQ_API_KEY=your-api-key
  - OPENAI_API_KEY=your-api-key
```

## 配置迁移说明

### 旧配置格式（已废弃）
```yaml
system:
  initializer:
    enabled: true
    # ...
    
app:
  ai:
    enabled: false
    # ...
```

### 新配置格式（推荐）
```yaml
group:
  system:
    initializer:
      enabled: true
      # ...
  
  ai:
    enabled: false
    # ...
```

## 优势

1. **统一管理**：所有系统配置都在 `group` 前缀下，易于查找和维护
2. **避免冲突**：减少与其他第三方库的配置前缀冲突
3. **清晰结构**：配置层次清晰，一目了然
4. **便于扩展**：未来新增系统配置时，直接在 `group` 下添加即可
