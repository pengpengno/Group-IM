# 使用 Spock + Groovy 为 Spring Boot 应用编写单元测试：从入门到实战

## 1. 引言

在 Java 世界中，JUnit 是最常见的测试框架，但 Spock 框架凭借其 Groovy 语言的表达力、清晰的语法和内置的 Mock 功能，成为了测试的强力工具。本文将介绍如何在 Spring Boot 项目中集成 Groovy + Spock，实现易于编写、阅读和维护的测试代码，并通过 JaCoCo 生成测试覆盖率报告。

---

## 2. 搭建环境

确保你的 `pom.xml` 包含以下依赖：

```xml
<dependencies>
    <!-- Spock 核心 -->
    <dependency>
        <groupId>org.spockframework</groupId>
        <artifactId>spock-core</artifactId>
        <version>2.3-groovy-3.0</version>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring Boot 测试支持 -->
    <dependency>
        <groupId>org.spockframework</groupId>
        <artifactId>spock-spring</artifactId>
        <version>2.3-groovy-3.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Groovy -->
    <dependency>
        <groupId>org.codehaus.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>3.0.19</version>
    </dependency>
</dependencies>
```

添加 Maven 插件，支持 `.groovy` 文件测试：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
    <configuration>
        <includes>
            <include>**/*Spec.groovy</include>
        </includes>
    </configuration>
</plugin>
```

---

## 3. 创建第一个 Spock 测试

以测试一个简单的 `CalculatorService` 为例：

```groovy
class CalculatorServiceSpec extends Specification {

    def calculatorService = new CalculatorService()

    def "加法功能测试"() {
        expect:
        calculatorService.add(1, 2) == 3
    }

    def "减法功能测试"() {
        expect:
        calculatorService.subtract(5, 3) == 2
    }
}
```

Groovy 的语法自然优雅，不再需要大量的注解和样板代码。

---

## 4. Spring 服务类的测试（含 Mock）

以一个好友系统的 `FriendshipService` 为例：

```groovy
@SpringBootTest
class FriendshipServiceSpec extends Specification {

    @Autowired
    FriendshipService friendshipService

    @SpringBean
    UserRepository userRepository = Mock()

    @SpringBean
    FriendshipRepository friendshipRepository = Mock()

    @SpringBean
    SendMessageToClientEndPointImpl sendMessageToClientEndPoint = Mock()

    def "测试好友请求发送"() {
        given:
        def user = new User(id: 1L)
        def friend = new User(id: 2L)
        def dto = new FriendRequestDto(userId: 1L, friendId: 2L)

        userRepository.findById(1L) >> Optional.of(user)
        userRepository.findById(2L) >> Optional.of(friend)

        when:
        friendshipService.sendFriendRequest(dto)

        then:
        1 * friendshipRepository.save(_ as Friendship)
        1 * sendMessageToClientEndPoint.sendMessage(dto)
    }

    def "测试好友请求接受"() {
        given:
        def user = new User(id: 1L)
        def friend = new User(id: 2L)
        def friendship = new Friendship(user: friend, friend: user)
        def dto = new FriendRequestDto(userId: 1L, friendId: 2L)

        userRepository.findById(2L) >> Optional.of(friend)
        userRepository.findById(1L) >> Optional.of(user)
        friendshipRepository.findByUserAndFriend(friend, user) >> Optional.of(friendship)

        when:
        friendshipService.acceptFriendRequest(dto)

        then:
        1 * friendshipRepository.save(friendship)
    }
}
```

---

## 5. 表驱动测试示例（Spock 特色）

Spock 强大的 `where:` 表格语法可以简化多组数据的测试：

```groovy
def "加法的多组输入测试"() {
    expect:
    calculatorService.add(a, b) == result

    where:
    a | b || result
    1 | 2 || 3
    2 | 3 || 5
    0 | 0 || 0
}
```

---

## 6. 集成 JaCoCo 生成覆盖率报告

在 `pom.xml` 中配置：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>prepare-package</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

然后执行以下命令：

```bash
mvn clean verify
```

查看生成的覆盖率报告：

```
target/site/jacoco/index.html
```

---

## 7. 常见问题排查

| 问题 | 解决方式 |
|------|----------|
| 无法识别 `.groovy` 文件 | 确保 `maven-surefire-plugin` 配置 includes |
| `jacoco.exec` 文件未生成 | 确保测试代码实际运行（不能空跑），`prepare-agent` 必须在测试执行前触发 |
| Spring 注入失败 | 使用 `@SpringBean` 而不是手动 `Mock` 注入 |
| `@SpringBootTest` 太慢 | 对纯逻辑类可以不用 Spring 容器，提升速度 |

---

## 8. 小结：为什么推荐 Spock

| 优势 | 描述 |
|------|------|
| 语法简洁 | Groovy 的 DSL 很接近自然语言 |
| Mock 简单 | 内置 Mock，无需额外框架 |
| 表格测试 | `where:` 语句支持数据驱动测试 |
| Spring 集成 | `@SpringBean` 自动注入 Mock |
| 可读性高 | 非开发人员也能理解测试内容 |

---

## 9. Mock 异常模拟测试（Spock 内置支持）

当你需要测试服务在某些异常情况下的行为时，Spock 提供了优雅的语法支持：

```groovy
def "当 UserRepository 抛出异常时应处理失败"() {
    given:
    def dto = new FriendRequestDto(userId: 1L, friendId: 2L)
    userRepository.findById(1L) >> { throw new RuntimeException("数据库异常") }

    when:
    friendshipService.sendFriendRequest(dto)

    then:
    def e = thrown(RuntimeException)
    e.message == "数据库异常"
}
```

也可以使用 Mock 的 `.throws()` 方法：

```groovy
userRepository.findById(_) >> { throw new IllegalStateException("非法状态") }
```

---

## 10. 参数校验测试（Bean Validation 场景）

Spring 中你可能使用 `javax.validation.Valid` 或 `jakarta.validation.Valid` 来做参数校验：

### 被测类

```java
public class FriendRequestDto {
    @NotNull
    private Long userId;
    
    @NotNull
    private Long friendId;
    // getters and setters
}
```

### 测试方式

可以直接测试校验逻辑：

```groovy
import javax.validation.Validation
import javax.validation.Validator

class FriendRequestDtoSpec extends Specification {

    Validator validator = Validation.buildDefaultValidatorFactory().validator

    def "userId 为 null 应该校验失败"() {
        given:
        def dto = new FriendRequestDto(userId: null, friendId: 2L)

        when:
        def violations = validator.validate(dto)

        then:
        violations.any { it.propertyPath.toString() == "userId" }
    }
}
```

---

## 11. 多模块项目中的 Spock 配置（父子模块）

你可能有如下结构：

```
my-project/
├── pom.xml (父模块)
├── common/ (包含 DTO/工具类等)
├── service/ (主业务逻辑)
└── tests/ (专门写测试用例)
```

### 父模块 `pom.xml`

```xml
<modules>
    <module>common</module>
    <module>service</module>
    <module>tests</module>
</modules>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.spockframework</groupId>
            <artifactId>spock-core</artifactId>
            <version>2.3-groovy-3.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### `tests` 模块 `pom.xml`

```xml
<dependencies>
    <!-- 引入待测模块 -->
    <dependency>
        <groupId>com.example</groupId>
        <artifactId>service</artifactId>
    </dependency>

    <!-- Spock + Groovy 测试支持 -->
    <dependency>
        <groupId>org.spockframework</groupId>
        <artifactId>spock-spring</artifactId>
        <version>2.3-groovy-3.0</version>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>org.codehaus.groovy</groupId>
        <artifactId>groovy</artifactId>
        <version>3.0.19</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.5</version>
            <configuration>
                <includes>
                    <include>**/*Spec.groovy</include>
                </includes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 启动 Spock 测试

执行：

```bash
mvn clean verify
```

---

## 12. 小结

| 测试类型 | 示例 |
|----------|------|
| 正常逻辑 | `expect: service.add(1,2)==3` |
| 异常逻辑 | `thrown(RuntimeException)` |
| 校验逻辑 | `validator.validate(dto)` |
| 多模块配置 | 父子模块分离，测试集中放在 `tests` 模块 |

---
## 13. 使用 `@WebMvcTest` 进行 Controller 层测试（Spring + Spock）

`@WebMvcTest` 适合只加载 Controller 层，模拟 HTTP 请求，不加载 Service 和 Repository。

### 示例 Controller

```java
@RestController
@RequestMapping("/api/friendship")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<String> sendFriendRequest(@RequestBody @Valid FriendRequestDto request) {
        friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok("请求发送成功");
    }
}
```

### 示例测试（Spock）

```groovy
@WebMvcTest(FriendshipController)
class FriendshipControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    FriendshipService friendshipService = Mock()

    def "发送好友请求应返回200成功"() {
        given:
        def json = '{"userId":1,"friendId":2}'

        when:
        def result = mockMvc.perform(
            post("/api/friendship/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andReturn().response

        then:
        result.status == 200
        result.contentAsString == "请求发送成功"
        1 * friendshipService.sendFriendRequest(_)
    }

    def "参数校验失败时应返回400"() {
        given:
        def json = '{"userId":null,"friendId":2}'

        when:
        def result = mockMvc.perform(
            post("/api/friendship/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
        ).andReturn().response

        then:
        result.status == 400
    }
}
```

---

## 14. 在 GitHub Actions 中运行 Spock 测试并生成覆盖率报告

使用 GitHub Actions 自动运行测试、生成 Jacoco 报告、上传到 Codecov（或 SonarQube）。

### `.github/workflows/test.yml`

```yaml
name: Run Tests

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests with coverage
        run: mvn clean verify

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: target/site/jacoco/jacoco.xml
          fail_ci_if_error: true
          token: ${{ secrets.CODECOV_TOKEN }}
```

### `jacoco-maven-plugin` 配置补充（添加 `report-aggregate`）

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

> 注意：你需要在 Codecov 官网注册项目，并配置 `CODECOV_TOKEN` 到 GitHub Secrets。

---

## 15. 最终目录结构建议

```
project-root/
├── pom.xml
├── common/                 # 公共模块
├── service/                # 业务逻辑
├── controller/             # Controller 层
├── tests/                  # 专门的 Spock 测试模块（推荐）
│   └── FriendshipServiceSpec.groovy
└── .github/
    └── workflows/
        └── test.yml        # CI 流程
```

---

## 16. 补充资料

- Spock 官方文档：https://spockframework.org/spock/docs/
- Jacoco Maven Plugin：https://www.jacoco.org/jacoco/trunk/doc/maven.html
- Codecov GitHub Action：https://github.com/codecov/codecov-action
- Spring Boot @WebMvcTest：https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/test/autoconfigure/web/servlet/WebMvcTest.html

---

---

## 17. 使用 `@SpringBootTest` 进行完整集成测试

当需要加载整个 Spring 容器（含数据库、服务等）时，用 `@SpringBootTest`。

```groovy
@SpringBootTest
class FriendshipIntegrationSpec extends Specification {

    @Autowired
    FriendshipService friendshipService

    @Autowired
    FriendshipRepository friendshipRepository

    def "发送好友请求后应保存记录"() {
        given:
        def request = new FriendRequestDto(userId: 1L, friendId: 2L)

        when:
        friendshipService.sendFriendRequest(request)

        then:
        def saved = friendshipRepository.findAll()
        saved.size() == 1
        saved[0].userId == 1L
        saved[0].friendId == 2L
    }
}
```

---

## 18. 使用 H2 数据库进行 Repository 测试

Spring Boot 测试中，默认即可使用 H2 内存数据库。只需配置：

### `application-test.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate.ddl-auto: create-drop
    show-sql: true
```

### Repository 测试示例

```groovy
@DataJpaTest
@ActiveProfiles("test")
class FriendshipRepositorySpec extends Specification {

    @Autowired
    FriendshipRepository repository

    def "保存和查找好友关系"() {
        given:
        def friendship = new Friendship(userId: 1L, friendId: 2L)

        when:
        repository.save(friendship)

        then:
        repository.findAll().size() == 1
    }
}
```

---

## 19. 使用 Testcontainers 启动真实数据库

适用于你希望在测试中使用真实的 PostgreSQL/MySQL 等。

### 添加依赖

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.1</version>
    <scope>test</scope>
</dependency>
```

### 示例：使用 PostgreSQL

```groovy
@Testcontainers
@SpringBootTest
class PostgreSqlIntegrationSpec extends Specification {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl)
        registry.add("spring.datasource.username", postgres::getUsername)
        registry.add("spring.datasource.password", postgres::getPassword)
    }

    @Autowired
    FriendshipRepository repository

    def "真实PostgreSQL中保存数据"() {
        when:
        repository.save(new Friendship(userId: 1L, friendId: 2L))

        then:
        repository.findAll().size() == 1
    }
}
```

---

## 20. Jacoco 配置覆盖率阈值

强制 CI 中测试覆盖率达标，否则构建失败。

### 修改 `jacoco-maven-plugin`

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>CLASS</element>
                        <limits>
                            <limit>
                                <counter>INSTRUCTION</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum> <!-- 80% -->
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### CI 中构建失败示例

```bash
[ERROR] Rule violated for class com.example.service.FriendshipService: 
       covered ratio is 0.78, but expected minimum is 0.80
```

---

## 总结：Spock + Spring 测试金字塔推荐

| 层级             | 工具                | 特点                                      |
|------------------|---------------------|-------------------------------------------|
| 单元测试         | Spock + Mock        | 运行快，Mock依赖，聚焦逻辑正确性          |
| Web 层           | Spock + MockMvc     | 测试 Controller，模拟 HTTP 请求           |
| 数据库层         | Spock + H2          | 测试 JPA、SQL，使用内存数据库             |
| 集成测试         | Spock + SpringBoot  | 全容器测试，适合服务链路验证              |
| 容器化集成测试   | Spock + Testcontainers | 测试真实 DB、中间件，适合 CI              |

---

---

## 21. 异常断言与边界值测试（Spock 风格）

在服务层或 Controller 中，我们通常要测试边界输入和异常处理逻辑：

```groovy
class UserServiceSpec extends Specification {

    def userService = new UserService()

    def "用户ID不能为负数"() {
        when:
        userService.findById(-1L)

        then:
        def e = thrown(IllegalArgumentException)
        e.message == "userId must be positive"
    }

    def "合法ID时返回用户"() {
        expect:
        userService.findById(1L).name == "张三"
    }
}
```

---

## 22. 使用 MockMvc 测试 Spring REST 接口

可以不启动整个容器，专测 Controller 层逻辑：

```groovy
@WebMvcTest(UserController)
class UserControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @MockBean
    UserService userService

    def "GET /user/{id} 应返回用户数据"() {
        given:
        userService.findById(1L) >> new User(1L, "张三")

        expect:
        mockMvc.perform(get("/user/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath('$.name').value("张三"))
    }
}
```

---

## 23. 参数化测试和 `@Unroll` 展开

Spock 支持表格式的参数测试，结合 `@Unroll` 展开显示测试案例：

```groovy
@Unroll
class PasswordValidatorSpec extends Specification {

    def validator = new PasswordValidator()

    def "密码合法性校验：#password 是否有效"() {
        expect:
        validator.isValid(password) == expected

        where:
        password     || expected
        "abc123"     || true
        "123"        || false
        ""           || false
        "Admin1234"  || true
    }
}
```

输出：

```txt
密码合法性校验：abc123 是否有效   PASSED
密码合法性校验：123 是否有效     PASSED
密码合法性校验："" 是否有效       PASSED
```

---

## 24. Mock 静态方法 / 构造器（高级）

如果你测试的类中调用了 **静态方法、final类** 或 **构造器**，Spock + Mockito 默认是无法模拟的。

解决方案：

### 使用 PowerMock（适配 JUnit4）

```xml
<dependency>
  <groupId>org.powermock</groupId>
  <artifactId>powermock-module-junit4</artifactId>
  <version>2.0.9</version>
  <scope>test</scope>
</dependency>
```

```groovy
@RunWith(PowerMockRunner.class)
@PrepareForTest([StaticUtils])
class StaticMethodSpec extends Specification {

    def "静态方法返回值可以mock"() {
        given:
        PowerMockito.mockStatic(StaticUtils)
        StaticUtils.calculateValue() >> 42

        expect:
        StaticUtils.calculateValue() == 42
    }
}
```

或者迁移到 Kotlin + [MockK](https://mockk.io/)，支持静态方法和 final 类 mock。

---

## 25. 测试私有方法（不推荐但可行）

Spock 强调行为测试，不建议直接测私有方法。但可借助反射：

```groovy
def "测试私有方法 calculateChecksum"() {
    given:
    def service = new UserService()
    def method = UserService.getDeclaredMethod("calculateChecksum", String)
    method.setAccessible(true)

    expect:
    method.invoke(service, "test") == 12345
}
```

---

## 26. 使用 `@TestPropertySource` 加载测试专用配置

有些测试需要使用测试环境专属配置文件：

```groovy
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class UserServiceWithTestProfileSpec extends Specification {

    @Autowired
    Environment environment

    def "读取测试配置中的特殊值"() {
        expect:
        environment.getProperty("my.test.flag") == "enabled"
    }
}
```

---

## 27. 多模块项目中的测试配置技巧

多模块结构下（如：`common` / `service` / `web`），需注意：

- 公共模块中可使用 `@TestConfiguration` 定义 mock Bean
- 各模块测试用不同的 profile 区分资源加载
- `mvn test` 时，确认子模块配置未冲突

示例：在 `common-test` 中放置：

```java
@TestConfiguration
public class TestBeansConfig {
    @Bean
    public Clock fixedClock() {
        return Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC);
    }
}
```

---


---

## 28. Spock & Groovy 特性技巧小结

Spock 是建立在 Groovy 语言之上的测试框架，Groovy 本身提供了很多增强语法，让测试更简洁、表达力更强：

---

### 28.1 Groovy 的闭包表达式

闭包可以像函数一样传递，是 Groovy 最强大的语法之一。

```groovy
def list = [1, 2, 3, 4]
def result = list.findAll { it % 2 == 0 }

assert result == [2, 4]
```

在 Spock 中经常用于 mock 的返回逻辑：

```groovy
mockService.getUser(_) >> { Long id -> return new User(id, "User$id") }
```

---

### 28.2 Groovy 的数据表（Data Table）

Spock 支持表格化参数，使用 `where:` 结合 `@Unroll`，方便进行参数化测试：

```groovy
@Unroll
def "合法用户输入校验: #username"() {
    expect:
    validator.isValid(username) == expected

    where:
    username   || expected
    "张三"     || true
    "李四"     || true
    ""         || false
    null       || false
}
```

---

### 28.3 Groovy 的简洁 Map/List 语法

Groovy 支持 JSON 风格的 Map 声明，非常适合构造测试数据：

```groovy
def user = [id: 1L, name: "张三", roles: ["ADMIN", "USER"]]
assert user.name == "张三"
```

---

### 28.4 Spock 的交互式 Mocking（行为驱动）

可以声明预期调用和参数：

```groovy
def userService = Mock(UserService)

when:
userService.getUser("admin")

then:
1 * userService.getUser("admin") >> new User("admin")
```

含义：
- `1 *` 表示期望调用一次
- `>>` 表示模拟返回值

---

### 28.5 Spock 的 Exception Assertion 更直观

不需要 try-catch，可以直接断言异常：

```groovy
when:
userService.deleteUser(null)

then:
def ex = thrown(IllegalArgumentException)
ex.message == "userId cannot be null"
```

---

### 28.6 Mock 方法的入参匹配（高级）

可以指定参数类型、值范围等：

```groovy
1 * userService.getUser(_ as String) >> new User("default")

1 * userService.getUser({ it.startsWith("admin") }) >> new User("admin1")
```

---

### 28.7 条件判断语法更强大（Groovy Truth）

Groovy 允许将对象直接作为条件，简化了判断逻辑：

```groovy
if (list) { // 非空 list 执行
}

if (!map) { // 空 map 执行
}
```

---

### 28.8 元编程调试技巧

可以在运行时输出变量名和内容：

```groovy
println "结果是: ${result}"
```

也可以使用断言失败自动输出：

```groovy
assert result == expected // 不等时会自动打印 result 的值
```

---

### 28.9 使用 Category 为旧类动态扩展方法（Groovy 高级）

Groovy 支持动态扩展类方法：

```groovy
class MyCategory {
    static String shout(String self) {
        return self.toUpperCase() + "!"
    }
}

use(MyCategory) {
    assert "hello".shout() == "HELLO!"
}
```

---

### 28.10 Groovy DSL 写法用于测试数据工厂

```groovy
class UserBuilder {
    Long id
    String name = "默认用户"

    User build() {
        new User(id, name)
    }
}

def user = new UserBuilder(id: 1L, name: "张三").build()
```

这在构造复杂对象做测试时非常方便。

---

## 总结

| 特性类型 | 示例说明 |
|----------|----------|
| 闭包传参 | `{ it -> it * 2 }` |
| 数据表   | `where:` 语法 |
| Mock 行为 | `1 * service.method(_) >> value` |
| 异常断言 | `thrown(ExceptionType)` |
| 参数匹配 | `_ as Type`、`{ cond -> cond > 0 }` |
| Groovy Truth | `if (obj)` |
| Map/List 声明 | `[a:1, b:2]` / `[1,2,3]` |
| println 调试 | `"result: ${value}"` |

---
---

## 29. 使用 Groovy 编写共享测试 DSL

Groovy 语言非常适合构建 DSL（Domain Specific Language，领域特定语言），在测试场景中尤其强大。通过 DSL，可以让测试数据构造更语义化、可读性更强，减少冗余。

---

### 29.1 DSL 使用场景

- 构建复杂对象（如 User、Order）
- 复用通用测试数据模板
- 简化 mock 和断言的写法

---

### 29.2 示例目标：构建 User 测试数据 DSL

假设我们有如下 Java 类：

```java
public class User {
    private Long id;
    private String username;
    private String role;
    
    // 构造函数 + getter + setter
}
```

---

### 29.3 编写 Groovy DSL Builder

我们可以使用 Groovy 的类 + 闭包语法来自定义 DSL：

```groovy
class UserBuilder {
    Long id = 1L
    String username = "default"
    String role = "USER"

    static UserBuilder user(@DelegatesTo(UserBuilder) Closure closure) {
        def builder = new UserBuilder()
        closure.delegate = builder
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        return builder
    }

    User build() {
        new User(id, username, role)
    }
}
```

---

### 29.4 在测试中使用该 DSL

```groovy
def user = UserBuilder.user {
    id = 100L
    username = "admin"
    role = "ADMIN"
}.build()

assert user.username == "admin"
assert user.role == "ADMIN"
```

使用 DSL 之后语义更清晰，比直接 new 对象要优雅很多。

---

### 29.5 配合 Spock 参数化测试

我们可以结合 Spock 的 `@Unroll` + DSL：

```groovy
@Unroll
def "用户角色测试: #user.username - #user.role"() {
    expect:
    user.role == expected

    where:
    user << [
        UserBuilder.user { username = "admin"; role = "ADMIN" }.build(),
        UserBuilder.user { username = "guest"; role = "GUEST" }.build()
    ]
    expected << ["ADMIN", "GUEST"]
}
```

---

### 29.6 DSL 支持嵌套对象（进阶）

假设一个 `Order` 包含多个 `Item`：

```java
public class Order {
    private Long id;
    private List<Item> items;
}
public class Item {
    private String name;
    private int count;
}
```

DSL 构建器可以嵌套：

```groovy
class OrderBuilder {
    Long id = 1L
    List<Item> items = []

    def item(@DelegatesTo(ItemBuilder) Closure closure) {
        def builder = new ItemBuilder()
        closure.delegate = builder
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        items << builder.build()
    }

    Order build() {
        new Order(id, items)
    }
}

class ItemBuilder {
    String name = "默认商品"
    int count = 1

    Item build() {
        new Item(name, count)
    }
}
```

使用方式：

```groovy
def order = new OrderBuilder().with {
    id = 888L
    item {
        name = "Apple"
        count = 3
    }
    item {
        name = "Banana"
        count = 5
    }
    build()
}

assert order.items.size() == 2
```

---

### 29.7 DSL 好处总结

| 优点 | 说明 |
|------|------|
| 结构清晰 | 表意更清晰，类 JSON 格式 |
| 易于维护 | 集中维护数据构造逻辑 |
| 扩展方便 | 支持默认值、校验、嵌套 |
| 灵活重用 | 不同测试中快速复用逻辑 |

---

### 29.8 实战建议

- 每个复杂实体写一个 `Builder.groovy`
- 使用 `@DelegatesTo` 和闭包构造流式 API
- 可扩展：增加默认值、校验逻辑
- 和 `@Unroll`、Mock、DataTable 搭配使用

---

[//]: # ()
[//]: # (如果你使用 Spring Boot + Groovy + Spock，建议将这些 `*Builder.groovy` 放到 `src/test/groovy/testsupport/builder/` 目录下统一管理。)

[//]: # ()
[//]: # (是否需要我生成一个完整的 `UserBuilder.groovy` 和使用示例代码文件？或者继续扩展数据工厂的其他部分？)



