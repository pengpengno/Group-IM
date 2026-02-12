# KMP单元测试实战指导

## 1. KMP测试基础概念

### 1.1 什么是KMP测试？
Kotlin Multiplatform (KMP) 测试允许你在共享代码中编写一次测试，然后在所有目标平台上运行。这对于确保跨平台行为一致性非常重要。

### 1.2 KMP测试的优势
- **一次编写，多处运行**：同一套测试代码可在Android、iOS、Web等多个平台执行
- **早期发现问题**：在共享逻辑层就能发现跨平台兼容性问题
- **提高代码质量**：强制性的测试驱动开发提升整体代码质量

## 2. 测试环境搭建

### 2.1 项目结构要求
```
src/
├── commonMain/          # 共享业务逻辑
├── commonTest/          # 共享测试代码
├── androidMain/         # Android特定实现
├── androidUnitTest/     # Android单元测试
├── iosMain/             # iOS特定实现
└── iosTest/             # iOS测试代码
```

### 2.2 依赖配置
在 `build.gradle.kts` 中添加测试依赖：

```kotlin
kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        
        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("androidx.test.ext:junit:1.1.5")
            }
        }
    }
}
```

## 3. 测试编写基础

### 3.1 基本测试结构
```kotlin
import kotlin.test.*

class ExampleTest {
    
    @BeforeTest
    fun setup() {
        // 测试前的准备工作
    }
    
    @AfterTest
    fun teardown() {
        // 测试后的清理工作
    }
    
    @Test
    fun testSomething() {
        // Arrange - 准备测试数据
        val input = "test"
        
        // Act - 执行被测试的方法
        val result = processString(input)
        
        // Assert - 验证结果
        assertEquals("processed_test", result)
    }
}
```

### 3.2 常用断言方法
```kotlin
// 相等性断言
assertEquals(expected, actual)
assertNotEquals(unexpected, actual)

// 布尔断言
assertTrue(condition)
assertFalse(condition)

// 空值断言
assertNull(value)
assertNotNull(value)

// 异常断言
assertFailsWith<ExceptionType> {
    // 可能抛出异常的代码
}
```

## 4. ViewModel测试实战

### 4.1 测试UserViewModel登录功能

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UserViewModelTest {
    
    private lateinit var viewModel: UserViewModel
    
    @BeforeTest
    fun setup() {
        viewModel = UserViewModel()
    }
    
    @Test
    fun `登录成功时应该更新用户状态`() = runTest {
        // Given
        val username = "test@example.com"
        val password = "password123"
        
        // When
        viewModel.login(username, password)
        
        // Then
        assertEquals(LoginState.Authenticated, viewModel.loginState.value)
        assertNotNull(viewModel.currentUser)
    }
    
    @Test
    fun `登录失败时应该显示错误信息`() = runTest {
        // Given
        val invalidUsername = "invalid"
        val password = "wrong"
        
        // When
        viewModel.login(invalidUsername, password)
        
        // Then
        val state = viewModel.loginState.value
        assertTrue(state is LoginState.AuthenticationFailed)
        assertNotNull((state as LoginState.AuthenticationFailed).errorMessage)
    }
}
```

### 4.2 测试异步操作

```kotlin
@Test
fun `异步登录应该正确处理`() = runTest {
    // Given
    val username = "user@example.com"
    val password = "password"
    
    // When
    val deferredResult = async {
        viewModel.login(username, password)
    }
    
    // 等待异步操作完成
    deferredResult.await()
    
    // Then
    assertEquals(LoginState.Authenticated, viewModel.loginState.value)
}
```

## 5. Repository层测试

### 5.1 测试数据访问逻辑

```kotlin
class UserRepositoryTest {
    
    private lateinit var repository: UserRepository
    private lateinit var mockDatabase: Database
    
    @BeforeTest
    fun setup() {
        mockDatabase = createMockDatabase()
        repository = UserRepository(mockDatabase)
    }
    
    @Test
    fun `保存用户信息应该成功`() {
        // Given
        val userInfo = UserInfo(1L, "test", "test@example.com")
        
        // When
        val result = repository.saveUser(userInfo)
        
        // Then
        assertTrue(result.isSuccess)
        assertEquals(userInfo, repository.getCurrentUser())
    }
    
    @Test
    fun `获取不存在的用户应该返回null`() {
        // When
        val user = repository.getUserById(999L)
        
        // Then
        assertNull(user)
    }
}
```

## 6. Mocking和依赖注入

### 6.1 使用接口进行Mock

```kotlin
// 定义接口
interface UserRepositoryInterface {
    fun getCurrentUser(): UserInfo?
    fun saveUser(user: UserInfo): Result<Unit>
}

// 实现类
class UserRepository(private val database: Database) : UserRepositoryInterface {
    // 实现方法...
}

// 测试中使用Mock
class ViewModelTest {
    private val mockRepository = object : UserRepositoryInterface {
        override fun getCurrentUser(): UserInfo? = UserInfo(1L, "test", "test@example.com")
        override fun saveUser(user: UserInfo): Result<Unit> = Result.success(Unit)
    }
    
    private val viewModel = MyViewModel(mockRepository)
    
    @Test
    fun `应该能够获取当前用户`() {
        val user = viewModel.currentUser
        assertNotNull(user)
        assertEquals("test", user.username)
    }
}
```

### 6.2 手动Mock实现

```kotlin
class MockUserRepository : UserRepositoryInterface {
    private var currentUser: UserInfo? = null
    private val savedUsers = mutableMapOf<Long, UserInfo>()
    
    override fun getCurrentUser(): UserInfo? = currentUser
    
    override fun saveUser(user: UserInfo): Result<Unit> {
        savedUsers[user.userId] = user
        currentUser = user
        return Result.success(Unit)
    }
    
    fun setCurrentUser(user: UserInfo?) {
        currentUser = user
    }
}
```

## 7. 测试协程和异步代码

### 7.1 使用TestCoroutineScheduler

```kotlin
import kotlinx.coroutines.test.*

class AsyncOperationTest {
    
    private val testScope = TestScope()
    
    @Test
    fun `延迟操作应该按预期执行`() = testScope.runTest {
        var completed = false
        
        backgroundScope.launch {
            delay(1000) // 模拟延迟
            completed = true
        }
        
        // 在测试中立即推进时间
        testScheduler.advanceTimeBy(1000)
        
        assertTrue(completed)
    }
    
    @Test
    fun `多个协程应该正确调度`() = testScope.runTest {
        val results = mutableListOf<String>()
        
        repeat(3) { i ->
            backgroundScope.launch {
                delay((3 - i) * 100L) // 不同的延迟
                results.add("Task $i")
            }
        }
        
        // 推进足够的时间让所有任务完成
        testScheduler.advanceUntilIdle()
        
        assertEquals(listOf("Task 2", "Task 1", "Task 0"), results)
    }
}
```

## 8. 测试Android特定代码

### 8.1 Android Instrumentation测试

```kotlin
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSpecificTest {
    
    @Test
    fun `应该能够访问Android上下文`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context)
        assertEquals("com.github.im.group", context.packageName)
    }
    
    @Test
    fun `SharedPreferences应该正常工作`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        
        prefs.edit().putString("test_key", "test_value").apply()
        assertEquals("test_value", prefs.getString("test_key", ""))
    }
}
```

## 9. 测试最佳实践

### 9.1 测试命名规范
```kotlin
// 好的命名 - 描述行为和期望结果
@Test
fun `登录成功时应该更新认证状态`()

@Test
fun `无效凭证登录时应该显示错误信息`()

@Test
fun `网络异常时应该重试登录请求`()

// 避免的命名 - 过于技术化或模糊
@Test
fun testLogin()          // 太模糊
@Test
fun loginTestSuccess()   // 倒装语序
@Test
fun test123()            // 无意义编号
```

### 9.2 AAA测试模式
```kotlin
@Test
fun `用户登录功能测试`() {
    // Arrange - 准备阶段
    val viewModel = UserViewModel()
    val username = "test@example.com"
    val password = "password123"
    
    // Act - 执行阶段
    viewModel.login(username, password)
    
    // Assert - 验证阶段
    assertEquals(LoginState.Authenticated, viewModel.loginState.value)
}
```

### 9.3 测试独立性
```kotlin
class IndependentTest {
    
    @Test
    fun `测试A应该独立运行`() {
        // 不依赖其他测试的状态
        val service = createFreshService()
        val result = service.doSomething()
        assertEquals(expected, result)
    }
    
    @Test
    fun `测试B应该独立运行`() {
        // 不依赖测试A的结果
        val service = createFreshService()
        val result = service.doSomethingElse()
        assertEquals(otherExpected, result)
    }
}
```

## 10. 常见问题和解决方案

### 10.1 依赖解析问题
```kotlin
// 问题：找不到测试依赖
// 解决方案：确保在正确的sourceSet中声明依赖

kotlin {
    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test")) // 确保添加这个
            }
        }
    }
}
```

### 10.2 协程测试超时
```kotlin
// 问题：测试因为协程挂起而超时
// 解决方案：使用TestScope和advanceUntilIdle

@Test
fun `长时间运行的协程测试`() = runTest(timeout = 5.seconds) {
    val job = launch {
        // 长时间运行的操作
        delay(10000) // 10秒
    }
    
    // 使用虚拟时间而不是真实等待
    testScheduler.advanceTimeBy(10000)
    job.cancelAndJoin()
}
```

### 10.3 Android Context缺失
```kotlin
// 问题：测试中无法获取Android Context
// 解决方案：使用InstrumentationRegistry或Robolectric

@Test
fun `需要Context的测试`() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    // 或者使用 Robolectric.setupActivity(MainActivity::class.java)
}
```

## 11. 测试运行和调试

### 11.1 命令行运行测试
```bash
# 运行所有测试
./gradlew test

# 运行特定模块的测试
./gradlew :composeApp:test

# 运行特定测试类
./gradlew test --tests "*UserViewModelTest"

# 生成测试报告
./gradlew test jacocoTestReport
```

### 11.2 IDE集成
- **IntelliJ IDEA**：右键测试文件或方法选择"Run"
- **Android Studio**：使用内置的测试运行器
- **VS Code**：安装Kotlin扩展后支持测试运行

## 12. 测试覆盖率和质量

### 12.1 关注的覆盖率指标
- **行覆盖率**：≥ 80%
- **分支覆盖率**：≥ 75%
- **函数覆盖率**：≥ 85%

### 12.2 质量检查清单
- [ ] 每个公共方法都有对应的测试
- [ ] 测试覆盖正常和异常路径
- [ ] 测试名称清晰描述测试意图
- [ ] 测试之间相互独立
- [ ] 测试运行速度快且稳定

---
*文档版本：v1.0*
*最后更新：2026-02-09*

这个指导文档提供了KMP单元测试的完整入门指南，从基础概念到实战技巧，帮助开发者快速掌握KMP测试开发技能。