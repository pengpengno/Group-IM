package com.github.im.group.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * UserViewModel Android Instrumentation测试
 * 直接测试生产代码，在Android设备/模拟器上运行
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UserViewModelAndroidTest {
    
    private lateinit var context: Context
    private lateinit var userRepository: UserRepository
    private lateinit var loginStateManager: LoginStateManager
    private lateinit var userViewModel: UserViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    private val testUserInfo = UserInfo(
        userId = 12345L,
        username = "android_test_user",
        email = "test@android.com",
        token = "android_access_token",
        refreshToken = "android_refresh_token"
    )
    
    @Before
    fun setup() {
        // 获取真实的Android应用上下文
        context = ApplicationProvider.getApplicationContext()
        
        // 设置测试调度器
        Dispatchers.setMain(testDispatcher)
        
        // Mock依赖项
        val mockDatabase = mockk<com.github.im.group.db.AppDatabase>()
        userRepository = UserRepository(mockDatabase)
        loginStateManager = mockk(relaxed = true)
        userViewModel = UserViewModel(userRepository, loginStateManager)
        
        // Mock全局凭证提供者
        mockkObject(GlobalCredentialProvider)
        every { GlobalCredentialProvider.storage } returns mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    /**
     * 测试Android环境下真实的登录流程
     * 直接测试UserViewModel的login方法
     */
    @Test
    fun testRealAndroidLoginFlow() = runTest {
        // Mock网络API调用
        mockkObject(com.github.im.group.api.LoginApi)
        coEvery { com.github.im.group.api.LoginApi.login(any(), any(), any()) } returns testUserInfo
        
        // 执行真实的登录
        val loginResult = userViewModel.login("testuser", "password123")
        advanceUntilIdle()
        
        // 验证登录结果
        assertTrue(loginResult, "Android环境下登录应该成功")
        
        // 验证状态更新
        val loginState = userViewModel.loginState.value
        assertTrue(loginState is LoginState.Authenticated, "登录成功后状态应该是Authenticated")
        assertEquals(testUserInfo.userId, (loginState as LoginState.Authenticated).userInfo.userId)
        
        // 验证Mock调用
        coVerify { com.github.im.group.api.LoginApi.login("testuser", "password123", "") }
        verify { loginStateManager.setLoggedIn(testUserInfo) }
    }
    
    /**
     * 测试Android环境下的网络异常处理
     * 验证UserViewModel对真实网络错误的处理
     */
    @Test
    fun testAndroidNetworkErrorHandling() = runTest {
        // Mock网络异常
        mockkObject(com.github.im.group.api.LoginApi)

        coEvery { com.github.im.group.api.LoginApi.login(any(), any(), any()) } throws
            java.net.ConnectException("Android: 网络连接超时")

        // 执行登录尝试
        val loginResult = userViewModel.login("testuser", "password123")
        advanceUntilIdle()
        
        // 验证错误处理
        assertFalse(loginResult, "网络错误时登录应该失败")
        
        // 验证状态更新
        val loginState = userViewModel.loginState.value
        assertTrue(loginState is LoginState.AuthenticationFailed, "网络错误后状态应该是AuthenticationFailed")
        
        val authFailed = loginState as LoginState.AuthenticationFailed
        assertTrue(authFailed.isNetworkError, "网络异常应该被标记为网络错误")
        assertTrue(authFailed.error.contains("Android:"), "错误信息应该包含Android环境标识")
    }
    
    /**
     * 测试Android环境下的自动登录功能
     * 验证UserViewModel的autoLogin方法
     */
    @Test
    fun testAndroidAutoLogin() = runTest {
        // Mock本地存储的用户信息
        coEvery { GlobalCredentialProvider.storage.getUserInfo() } returns testUserInfo
        
        // Mock网络API调用
        mockkObject(com.github.im.group.api.LoginApi)
        coEvery { com.github.im.group.api.LoginApi.login(any(), any(), testUserInfo.refreshToken) } returns testUserInfo
        
        // 执行自动登录
        userViewModel.autoLogin()
        advanceUntilIdle()
        
        // 验证自动登录流程
        val loginState = userViewModel.loginState.value
        assertTrue(loginState is LoginState.Authenticated, "自动登录成功后状态应该是Authenticated")
        
        // 验证使用了refreshToken
        coVerify { com.github.im.group.api.LoginApi.login("", "", testUserInfo.refreshToken) }
    }
    
    /**
     * 测试Android环境下的登出功能
     * 验证UserViewModel的logout方法
     */
    @Test
    fun testAndroidLogout() = runTest {
        // 先登录
        mockkObject(com.github.im.group.api.LoginApi)
        coEvery { com.github.im.group.api.LoginApi.login(any(), any(), any()) } returns testUserInfo
        userViewModel.login("testuser", "password123")
        advanceUntilIdle()
        
        // 执行登出
        userViewModel.logout()
        advanceUntilIdle()
        
        // 验证登出后状态
        val loginState = userViewModel.loginState.value
        assertTrue(loginState is LoginState.Idle, "登出后状态应该是Idle")
        
        // 验证清理操作
        verify { loginStateManager.setLoggedOut() }
        coVerify { GlobalCredentialProvider.storage.clearUserInfo() }
    }
    
    /**
     * 测试Android环境下的用户信息查询
     * 验证UserViewModel的searchUser方法
     */
    @Test
    fun testAndroidUserSearch() = runTest {
        // Mock用户搜索API
        val searchResults = listOf(
            UserInfo(1L, "user1", "user1@example.com", "", ""),
            UserInfo(2L, "user2", "user2@example.com", "", "")
        )
        
        mockkObject(com.github.im.group.api.UserApi)
        coEvery { com.github.im.group.api.UserApi.findUser("test") } returns 
            mockk {
                every { content } returns searchResults
            }
        
        // 执行搜索
        userViewModel.searchUser("test")
        advanceUntilIdle()
        
        // 验证搜索结果
        val searchState = userViewModel.searchResults.value
        assertEquals(2, searchState.size, "应该返回2个搜索结果")
        assertEquals("user1", searchState[0].username, "第一个用户应该是user1")
    }
    
    /**
     * 测试Android环境下的状态流收集
     * 验证UserViewModel的StateFlow在Android环境中的行为
     */
    @Test
    fun testAndroidStateFlowCollection() = runTest {
        // 测试初始状态
        val initialState = userViewModel.loginState.value
        assertTrue(initialState is LoginState.Idle, "初始状态应该是Idle")
        
        // 测试状态变化收集
        var stateChanges = 0
        val collectJob = kotlinx.coroutines.test.runTest {
            userViewModel.loginState.collect { 
                stateChanges++
            }
        }
        
        // 触发状态变化
        mockkObject(com.github.im.group.api.LoginApi)
        coEvery { com.github.im.group.api.LoginApi.login(any(), any(), any()) } returns testUserInfo
        userViewModel.login("test", "pass")
        advanceUntilIdle()
        
        assertTrue(stateChanges > 1, "应该检测到状态变化")
        // collectJob会在测试结束时自动清理
    }
}