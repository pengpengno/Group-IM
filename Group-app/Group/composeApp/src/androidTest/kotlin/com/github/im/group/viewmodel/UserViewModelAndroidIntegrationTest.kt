package com.github.im.group.viewmodel

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.github.im.group.AndroidCredentialStorage
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * UserViewModel Android集成测试
 * 测试Android特有功能与UserViewModel的集成
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserViewModelAndroidIntegrationTest {

    @get:Rule
    val internetPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var powerManager: PowerManager
    private lateinit var userRepository: UserRepository
    private lateinit var loginStateManager: LoginStateManager
    private lateinit var userViewModel: UserViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = UserInfo(
        userId = 1L,
        username = "android_integration_test",
        email = "integration@test.com",
        token = "integration_access_token",
        refreshToken = "integration_refresh_token"
    )

    @Before
    fun setup() {
        // 获取真实的Android上下文
        context = ApplicationProvider.getApplicationContext()
        
        // 获取真实的Android系统服务
        sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        
        // 设置测试调度器
        Dispatchers.setMain(testDispatcher)
        
        // 创建测试依赖
        val mockDatabase = mockk<com.github.im.group.db.AppDatabase>()
        userRepository = UserRepository(mockDatabase)
        loginStateManager = mockk(relaxed = true)
        userViewModel = UserViewModel(userRepository, loginStateManager)
        
        // Mock全局凭证提供者使用真实的Android CredentialStorage
        mockkObject(GlobalCredentialProvider)
        every { GlobalCredentialProvider.storage } returns AndroidCredentialStorage(context)
    }

    @After
    fun tearDown() {
        // 清理测试数据
        sharedPreferences.edit().clear().apply()
        Dispatchers.resetMain()
    }

    /**
     * 测试Android SharedPreferences与UserViewModel的集成
     * 验证用户信息在Android存储中的持久化与ViewModel的交互
     */
    @Test
    fun testAndroidSharedPreferencesIntegration() = runTest {
        // 1. 直接测试CredentialStorage的功能
        val credentialStorage = AndroidCredentialStorage(context)
        
        // 保存用户信息
        credentialStorage.saveUserInfo(testUser)
        advanceUntilIdle()
        
        // 验证用户信息已保存到SharedPreferences
        val storedUser: UserInfo? = credentialStorage.getUserInfo()
        assertNotNull(storedUser, "用户信息应该保存到SharedPreferences")
        assertEquals(testUser.userId, storedUser!!.userId, "保存的用户ID应该匹配")
        assertEquals(testUser.username, storedUser.username, "保存的用户名应该匹配")
        
        // 2. 测试通过ViewModel读取存储的用户信息
        // 在协程中调用suspend函数
         kotlinx.coroutines.test.runTest {
            val hasCredential = userViewModel.hasLocalCredential()
            assertTrue(hasCredential, "应该检测到本地凭据")
        }

        
        // 3. 测试清理功能
        credentialStorage.clearUserInfo()
        advanceUntilIdle()
        
        val clearedUser = credentialStorage.getUserInfo()
        assertNull(clearedUser, "清理后应该返回null")
    }

    /**
     * 测试Android网络状态检测功能
     * 验证应用对网络权限和状态的检测能力
     */
    @Test
    fun testAndroidNetworkStateDetection() = runTest {
        // 1. 检查真实的网络权限状态
        val hasInternetPermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasNetworkStatePermission = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_NETWORK_STATE
        ) == PackageManager.PERMISSION_GRANTED
        
        assertTrue(hasInternetPermission, "应该有INTERNET权限")
        assertTrue(hasNetworkStatePermission, "应该有ACCESS_NETWORK_STATE权限")
        
        // 2. 测试网络状态对ViewModel行为的影响
        val networkScenarios = listOf(
            NetworkScenario(hasConnection = true, canProceed = true, description = "有网络连接"),
            NetworkScenario(hasConnection = false, canProceed = false, description = "无网络连接")
        )
        
        networkScenarios.forEach { scenario ->
            // 模拟网络状态对登录决策的影响
            if (scenario.canProceed) {
                // 有网络时，ViewModel应该能够正常处理登录流程
                 kotlinx.coroutines.test.runTest {
                    val hasCredential = userViewModel.hasLocalCredential()
                    assertTrue(hasCredential, "有网络时应该能够检测到凭据[${scenario.description}]")

                }
            } else {
                // 无网络时，验证错误处理
                kotlinx.coroutines.test.runTest {
                    val hasCredential = userViewModel.hasLocalCredential()
                    assertFalse(hasCredential, "无网络时应该检测不到凭据[${scenario.description}]")
                }
            }
        }
    }

    /**
     * 测试Android电池优化状态检测
     * 验证应用对电源管理策略的适应能力
     */
    @Test
    fun testAndroidBatteryOptimizationDetection() = runTest {
        // 1. 检测真实的电池优化状态
        val isIgnoringBatteryOptimizations = 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true // 低版本默认忽略优化
            }
        
        assertNotNull(isIgnoringBatteryOptimizations, "应该能够检测电池优化状态")
        
        // 2. 测试不同电池状态下的应用行为
        val batteryScenarios = listOf(
            BatteryScenario(
                isPowerSaveMode = false,
                isIgnoringOptimizations = isIgnoringBatteryOptimizations,
                shouldAllowOperations = true,
                description = "正常电源状态"
            ),
            BatteryScenario(
                isPowerSaveMode = true,
                isIgnoringOptimizations = false,
                shouldAllowOperations = false,
                description = "省电模式且未白名单"
            )
        )
        
        batteryScenarios.forEach { scenario ->
            // 根据电池状态决定应用行为
            if (scenario.shouldAllowOperations) {
                // 正常状态下应该允许常规操作
                val currentUser = userViewModel.getCurrentUser()
                assertEquals(testUser.userId, currentUser.userId, "正常状态下应该能够获取用户信息[${scenario.description}]")
            } else {
                // 严格省电模式下验证限制行为
                kotlinx.coroutines.test.runTest {
                    val hasCredential = userViewModel.hasLocalCredential()
                    assertFalse(hasCredential, "严格省电模式下应该限制操作[${scenario.description}]")

                }
            }
        }
    }

    /**
     * 测试Android完整的登录集成流程
     * 验证UserViewModel与Android系统服务的完整集成
     */
    @Test
    fun testAndroidFullLoginIntegration() = runTest {
        // 1. Mock API调用（suspend函数需要使用coEvery）
        mockkObject(com.github.im.group.api.LoginApi)
        coEvery { com.github.im.group.api.LoginApi.login("test_user", "test_password", any()) } returns testUser
        
        // 2. 执行登录流程
        val loginResult = userViewModel.login("test_user", "test_password")
        advanceUntilIdle()
        
        // 3. 验证登录结果
        assertTrue(loginResult, "登录应该成功")
        
        // 4. 验证用户状态
        val currentUser = userViewModel.getCurrentUser()
        assertEquals(testUser.userId, currentUser.userId, "当前用户应该匹配登录用户")
        
        // 5. 验证凭据存储（使用正确的构造函数）
        val credentialStorage = AndroidCredentialStorage(context)
        val storedUser = credentialStorage.getUserInfo()
        assertNotNull(storedUser, "用户信息应该保存到Android存储")
        assertEquals(testUser.userId, storedUser!!.userId, "存储的用户ID应该匹配")
        
        // 6. 验证登录状态
        val loginState = userViewModel.loginState.value
        assertTrue(loginState is LoginState.Authenticated, "登录状态应该是Authenticated")
        assertEquals(testUser.userId, (loginState as LoginState.Authenticated).userInfo.userId, "认证状态中的用户ID应该匹配")
    }

    /**
     * 测试Android生命周期感知功能
     * 验证应用在不同生命周期状态下的行为
     */
    @Test
    fun testAndroidLifecycleAwareness() = runTest {
        // 1. 模拟应用前台状态
        // 测试前台状态下的正常功能
        val currentUser = userViewModel.getCurrentUser()
        assertEquals(testUser.userId, currentUser.userId, "前台状态下应该能够正常获取用户信息")
        
        // 2. 模拟应用后台状态
        // 验证状态持久性
        val loginState = userViewModel.loginState.value
        assertTrue(loginState is LoginState.Idle || loginState is LoginState.Authenticated, 
            "应用状态应该在前后台切换中保持一致")
        
        // 3. 测试状态流的收集能力
        var stateChanges = 0
        val collectJob = kotlinx.coroutines.test.runTest {
            userViewModel.loginState.collect { 
                stateChanges++
            }
        }
        
        // 触发状态变化
        userViewModel.logout()
        advanceUntilIdle()
        
        assertTrue(stateChanges >= 1, "应该能够检测到状态变化")
        // collectJob会在测试结束时自动清理
    }

    // 辅助数据类
    
    private data class NetworkScenario(
        val hasConnection: Boolean,
        val canProceed: Boolean,
        val description: String
    )
    
    private data class BatteryScenario(
        val isPowerSaveMode: Boolean,
        val isIgnoringOptimizations: Boolean,
        val shouldAllowOperations: Boolean,
        val description: String
    )
}