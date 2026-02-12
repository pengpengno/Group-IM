# Group IM Server 测试套件

## 目录结构

```
tests/
├── package-info.java          # 测试包说明文档
├── README.md                  # 本文件
├── service/                   # 服务层测试
│   ├── UserServiceSpec.groovy    # 用户服务测试
│   └── [其他服务测试]
├── controller/                # 控制器层测试
│   ├── UserControllerTest.groovy # 用户控制器测试
│   └── [其他控制器测试]
├── integration/               # 集成测试
│   └── [集成测试文件]
└── security/                  # 安全测试
    └── [安全相关测试]
```

## 测试策略

### 1. 测试金字塔原则

```
      ▲
      |  UI测试 (少量)
      |
   ┌--+--┐
   |  集成测试  |  (适量)
   └--+--┘
      |
   ┌--+--┐
   | 单元测试  |  (大量)
   └-------┘
```

### 2. 测试层次划分

| 层次 | 类型 | 数量 | 执行频率 | 目标 |
|------|------|------|----------|------|
| 单元测试 | 白盒测试 | 大量 | 每次提交 | 快速反馈 |
| 集成测试 | 灰盒测试 | 适量 | 每日构建 | 验证集成 |
| 端到端测试 | 黑盒测试 | 少量 | 发布前 | 验证完整流程 |

## 测试环境配置

### 开发环境测试

```properties
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
spring.redis.host=localhost
spring.redis.port=6379
```

### CI/CD环境测试

```yaml
# .github/workflows/test.yml 片段
jobs:
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:13
        env:
          POSTGRES_PASSWORD: testpass
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
```

## 测试数据管理

### 1. 测试数据工厂模式

```groovy
class TestDataFactory {
    static User createUser(Map overrides = [:]) {
        def defaults = [
            username: "testuser${System.currentTimeMillis()}",
            email: "test@example.com",
            password: "password123"
        ]
        return new User(defaults + overrides)
    }
    
    static Company createCompany(Map overrides = [:]) {
        def defaults = [
            name: "Test Company",
            code: "TEST_COMPANY"
        ]
        return new Company(defaults + overrides)
    }
}
```

### 2. 测试数据清理

```groovy
def cleanup() {
    // 清理安全上下文
    SecurityContextHolder.clearContext()
    
    // 重置Mock对象状态
    reset(userRepository, userService)
    
    // 清理测试数据
    testDataService.cleanup()
}
```

## 测试最佳实践

### 1. AAA模式 (Arrange-Act-Assert)

```groovy
def "测试用户注册成功"() {
    // Arrange - 准备阶段
    given: "有效的注册数据"
    def request = new RegistrationRequest("testuser", "test@example.com", "password123", "password123")
    
    // Act - 执行阶段  
    when: "执行注册操作"
    def result = userService.registerUser(request)
    
    // Assert - 验证阶段
    then: "验证注册成功"
    result.isPresent()
    result.get().username == "testuser"
}
```

### 2. 参数化测试

```groovy
def "测试密码强度验证"(String password, boolean expected) {
    given:
    def validator = new PasswordValidator()
    
    when:
    def result = validator.isValid(password)
    
    then:
    result == expected
    
    where:
    password       | expected
    "123"          | false
    "password123"  | false  
    "Password123!" | true
    "MySecurePass1"| true
}
```

### 3. 异常测试

```groovy
def "测试用户不存在时抛出异常"() {
    given:
    def userId = 999L
    
    when:
    userService.getUserById(userId)
    
    then:
    def ex = thrown(UserNotFoundException)
    ex.message.contains("User not found")
    ex.userId == userId
}
```

## 测试覆盖率监控

### 1. 实时覆盖率查看

```bash
# 生成详细覆盖率报告
mvn clean test jacoco:report

# 在浏览器中查看
open target/site/jacoco/index.html
```

### 2. 覆盖率门禁设置

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <configuration>
        <rules>
            <rule>
                <element>BUNDLE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</plugin>
```

## 持续集成集成

### GitHub Actions 配置示例

```yaml
name: Test Suite
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java-version: [21]
    
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK ${{ matrix.java-version }}
      uses: actions/setup-java@v3
      with:
        java-version: ${{ matrix.java-version }}
        distribution: 'temurin'
    
    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
        restore-keys: ${{ runner.os }}-m2
    
    - name: Run tests
      run: mvn clean test jacoco:report
    
    - name: Upload coverage reports
      uses: codecov/codecov-action@v3
      with:
        file: ./target/site/jacoco/jacoco.xml
```

## 故障排除

### 常见问题及解决方案

1. **Mock交互验证失败**
   ```bash
   # 问题: Too many invocations
   # 解决: 检查Mock调用次数预期是否正确
   
   # 问题: No interactions wanted
   # 解决: 确认是否真的不需要调用或调整测试逻辑
   ```

2. **测试数据污染**
   ```bash
   # 使用@Transactional回滚测试数据
   @Transactional
   def "测试方法"() { /* ... */ }
   
   # 或使用独立的测试数据库
   @ActiveProfiles("test")
   ```

3. **并发测试问题**
   ```bash
   # 使用随机数据避免冲突
   def username = "testuser_${UUID.randomUUID()}"
   
   # 或使用@TestPropertySource隔离配置
   @TestPropertySource(properties = ["app.test.mode=true"])
   ```

## 贡献指南

### 新增测试要求

1. **命名规范**: 遵循"测试[功能]_[场景]_[结果]"格式
2. **注释要求**: 每个测试方法必须有中文注释说明
3. **覆盖要求**: 新功能至少80%代码覆盖率
4. **执行要求**: 所有测试必须通过才能合并

### 代码审查检查点

- [ ] 测试名称清晰表达了测试意图
- [ ] 测试数据具有代表性和边界情况
- [ ] Mock使用合理，不过度Mock
- [ ] 断言准确，验证了关键业务逻辑
- [ ] 测试独立运行，无副作用
- [ ] 注释完整，符合团队规范

---
*最后更新: 2026年2月*