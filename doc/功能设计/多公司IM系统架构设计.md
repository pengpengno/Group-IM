# 多公司 IM 系统架构设计

## 1. 多租户架构设计

### 1.1 概念定义
- **集团公司**：拥有对所有子公司数据访问权限的中央管理实体
- **子公司**：独立运营的业务单位，其数据与其他子公司隔离
- **Schema隔离**：在PostgreSQL中，每个子公司对应一个独立的schema，实现数据物理隔离

### 1.2 多租户模型选择
采用 **独立数据库 Schema** 模式：
- 每个子公司拥有自己独立的数据库 schema
- 集团公司使用特殊的 schema 或通过权限控制访问所有子公司数据
- 优势：数据完全隔离，安全性高，易于扩展

## 2. 数据库设计

### 2.1 Schema 结构
```
- public                  # 公共 schema，存放全局配置
- group_company           # 集团公司 schema，可访问所有数据
- company_a_schema        # 子公司 A 的独立 schema
- company_b_schema        # 子公司 B 的独立 schema
```

### 2.2 核心表结构（每个 schema 内）

#### 用户表 (users)
```sql
CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE,
    company_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 消息表 (messages)
```sql
CREATE TABLE messages (
    message_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT,
    message_type VARCHAR(20),
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 会话表 (conversations)
```sql
CREATE TABLE conversations (
    conversation_id BIGSERIAL PRIMARY KEY,
    conversation_type VARCHAR(20), -- SINGLE, GROUP
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 会话参与者表 (conversation_participants)
```sql
CREATE TABLE conversation_participants (
    participant_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 3. 应用层设计

### 3.1 多租户上下文管理
```java
@Component
public class TenantContext {
    private static final ThreadLocal<String> tenantSchema = new ThreadLocal<>();
    
    public static void setCurrentTenant(String schema) {
        tenantSchema.set(schema);
    }
    
    public static String getCurrentTenant() {
        return tenantSchema.get();
    }
    
    public static void clear() {
        tenantSchema.remove();
    }
}
```

### 3.2 数据源配置
```java
@Configuration
public class MultiTenantDataSourceConfig {
    
    @Bean
    public DataSource dataSource() {
        // 配置多租户数据源
        return new MultiTenantDataSource();
    }
    
    @Bean
    public HibernateInterceptor hibernateInterceptor() {
        return new MultiTenantInterceptor();
    }
}
```

### 3.3 Hibernate 多租户拦截器
```java
public class MultiTenantInterceptor extends EmptyInterceptor {
    @Override
    public String onPrepareStatement(String sql) {
        String tenantSchema = TenantContext.getCurrentTenant();
        if (tenantSchema != null) {
            // 替换默认 schema
            return sql.replace("public.", tenantSchema + ".");
        }
        return super.onPrepareStatement(sql);
    }
}
```

## 4. 权限与访问控制

### 4.1 集团公司特殊权限
- 集团公司可以切换到任意子公司的 schema 查看数据
- 集团公司拥有全局统计和报表功能
- 集团公司可以跨公司查询和管理用户

### 4.2 子公司数据隔离
- 每个子公司只能访问自己的 schema
- 通过应用层和数据库层双重验证确保数据隔离
- 用户登录时自动识别所属公司并设置相应 schema

## 5. API 设计

### 5.1 认证与授权
```http
POST /auth/login
{
  "username": "user@example.com",
  "password": "password",
  "companyId": "company_a"
}
```

### 5.2 消息相关接口
```http
GET /api/messages?conversationId=123
POST /api/messages
{
  "conversationId": 123,
  "content": "Hello World",
  "recipientIds": [456, 789]
}
```

### 5.3 集团公司特有接口
```http
GET /api/admin/companies/stats
GET /api/admin/companies/{companyId}/users
```

## 6. 安全考虑

### 6.1 数据安全
- 所有敏感数据传输使用 HTTPS 加密
- 数据库存储密码使用 bcrypt 加密
- 定期审计数据库访问日志

### 6.2 访问控制
- 实施严格的 RBAC 权限模型
- 集团公司管理员具有超级权限但需二次验证
- 子公司用户只能访问本公司数据

## 7. 部署架构

### 7.1 数据库部署
- 使用 PostgreSQL 12+ 版本以获得更好的分区和性能
- 为每个重要子公司建立主从复制提高可用性
- 定期备份所有 schema 数据

### 7.2 应用部署
- 微服务架构，每个核心功能模块独立部署
- 使用 Kubernetes 编排容器化服务
- 负载均衡器分发请求到不同实例

## 8. 监控与运维

### 8.1 性能监控
- 监控各 schema 的查询性能
- 跟踪慢查询并优化
- 设置资源使用阈值告警

### 8.2 日志管理
- 统一收集所有服务的日志
- 区分不同公司的操作日志
- 实施实时异常检测