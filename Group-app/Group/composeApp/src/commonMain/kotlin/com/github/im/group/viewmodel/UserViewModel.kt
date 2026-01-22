package com.github.im.group.viewmodel

import UnauthorizedException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.FriendShipApi
import com.github.im.group.api.FriendshipDTO
import com.github.im.group.api.LoginApi
import com.github.im.group.api.UserApi
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.UserRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/***
 * 用于表明接口的调用状态
 */

sealed class LoginState {
    object Idle : LoginState()           // 未开始登录流程
    object Checking : LoginState()       // 检查本地凭据
    object Authenticating : LoginState() // 使用本地凭据进行认证
    data class Authenticated(val userInfo: UserInfo) : LoginState() // 已认证
    data class AuthenticationFailed(val error: String, val isNetworkError: Boolean = false) : LoginState() // 认证失败
    object LoggingOut : LoginState()     // 正在登出
}

class UserViewModel(
    val userRepository: UserRepository,
    val loginStateManager: LoginStateManager,
) : ViewModel() {

    /**
     * 登录状态
     * 从 UserRepository 获取状态，保持单一数据源
     */
    val loginState: StateFlow<LoginState> = userRepository.userState

    private val _friends = MutableStateFlow<List<FriendshipDTO>>(emptyList())
    val friends: StateFlow<List<FriendshipDTO>> = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserInfo>>(emptyList())
    val searchResults: StateFlow<List<UserInfo>> = _searchResults.asStateFlow()

    private val _currentUserInfo =  MutableStateFlow<UserInfo?>(null)

    var currentUserInfo : StateFlow<UserInfo?> =  _currentUserInfo.asStateFlow()
    
//    // 待处理好友请求数量状态
//    private val _pendingFriendRequestsCount = MutableStateFlow<Long>(0)
//    val pendingFriendRequestsCount: StateFlow<Long> = _pendingFriendRequestsCount.asStateFlow()
//
//    // 待处理好友请求列表
//    private val _pendingFriendRequests = MutableStateFlow<List<FriendshipDTO>>(emptyList())
//    val pendingFriendRequests: StateFlow<List<FriendshipDTO>> = _pendingFriendRequests.asStateFlow()

    init {
        viewModelScope.launch {
            // 监听登录状态
            loginState.collect { state ->
                when (state) {
                    is LoginState.Authenticated -> {
                        // 登录成功，更新当前用户信息
                        _currentUserInfo.value = state.userInfo
                    }
                    else -> {
                        // 登录失败，更新当前用户信息
                    }
                }
            }

            _currentUserInfo.value = GlobalCredentialProvider.storage.getUserInfo()

        }
    }
    /**
     * 安全获取当前用户信息
     * 登录状态必须是认证状态
     * 返回可空的UserInfo，避免抛出异常
     */
    fun getCurrentUser() : UserInfo {
        val authState = loginState.value as LoginState.Authenticated
        return  authState.userInfo
    }



    
    /**
     * 获取联系人列表
     */
    fun loadFriends() {
        viewModelScope.launch {
            try {
                val currentUser = getCurrentUser()
                if (currentUser?.userId != 0L) {
                    val friendList = FriendShipApi.getFriends(currentUser!!.userId)
                    _friends.value = friendList
                }
            } catch (e: Exception) {
                Napier.d("获取联系人失败: $e")
            }
        }
    }

    /**
     * 查询用户
     */
    fun searchUser(queryString: String) {
        if (queryString.isBlank()) {
            Napier.d("查询用户为空")
            _searchResults.value = emptyList()
            return
        }

        if (queryString.length < 2) {
            Napier.d("查询用户长度小于2")
            _searchResults.value = emptyList()
            return
        }

        if (queryString.startsWith("@")) {
            Napier.d("查询用户为@开头")
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            Napier.d("查询用户为${queryString}")
            try {
                val result = UserApi.findUser(queryString)
                Napier.d("搜索用户成功: ${result.content}")
                _searchResults.value = result.content
            } catch (e: Exception) {
                Napier.e("搜索用户失败: $e")
                _searchResults.value = emptyList()
            }
        }
    }


    /**
     * 自动登录
     * 如果本地存储了 用户信息 则尝试自动登录
     */
    fun autoLogin(){
        viewModelScope.launch {
            // 更新状态为检查中
            userRepository.updateToChecking()
            
            val userInfo = GlobalCredentialProvider.storage.getUserInfo()
            if (userInfo != null) {
                // 更新状态为认证中
                userRepository.updateToAuthenticating()
                // 尝试自动登录
                login(refreshToken = userInfo.refreshToken)
            } else {
                // 没有本地凭据，更新状态为空闲状态（需要登录）
                userRepository.updateToLoggedOut()
            }
        }

    }

    /**
     * 登录的方法
     *
     */
    suspend fun login(uname: String ="",
                      pwd:String ="",
                      refreshToken:String =""): Boolean {
        userRepository.updateToAuthenticating()
        try {
            val response = LoginApi.login(uname, pwd,refreshToken)

            GlobalCredentialProvider.storage.saveUserInfo(response)
            GlobalCredentialProvider.currentToken = response.token
            GlobalCredentialProvider.currentUserId = response.userId
            GlobalCredentialProvider.companyId = response.currentLoginCompanyId

            // 通知登录状态管理器用户已登录
            loginStateManager.setLoggedIn(response)
            // 更新用户仓库中的状态
            userRepository.updateToAuthenticated(response)
            // 登录成功返回true
            return true
        } catch (e: UnauthorizedException) {
            // 专门处理认证失败异常（401错误）
            Napier.d { "认证失败： ${e.message}" }
            // 更新用户仓库状态为认证失败，标记为非网络错误
            userRepository.updateToAuthenticationFailed(e.message ?: "认证失败", false)
            // 通知登录状态管理器用户已登出
            loginStateManager.setLoggedOut()
            // 清除本地存储的无效凭据
            GlobalCredentialProvider.storage.clearUserInfo()
            return false
        } catch (e: Exception) {
            Napier.d { "登录错误 ： ${e.message}" }
            // 判断错误类型，决定是网络错误还是认证错误
            val isNetworkError = e.message?.contains("timeout", ignoreCase = true) == true ||
                               e.message?.contains("network", ignoreCase = true) == true ||
                               e.message?.contains("connection", ignoreCase = true) == true ||
                               e.message?.contains("connect", ignoreCase = true) == true ||
                               e is java.net.ConnectException ||
                               e is java.net.SocketTimeoutException
            
            userRepository.updateToAuthenticationFailed(e.message ?: "登录失败", isNetworkError)
            Napier.d("loginState ${userRepository.userState.value}")
            return false
        }

    }
    
    /**
     * 登出方法
     */
    fun logout() {
        userRepository.updateToLoggingOut()
        loginStateManager.setLoggingOut()
        try {

            // 清除用户信息  避免下次自动登录
            runBlocking {
                GlobalCredentialProvider.storage.clearUserInfo()
                GlobalCredentialProvider.currentToken = ""
            }

            // 通知登录状态管理器用户已登出
            loginStateManager.setLoggedOut()
            // 更新用户仓库中的状态为登出
            userRepository.updateToLoggedOut()
        } catch (e: Exception) {
            Napier.d { "登出错误 ： ${e.message}" }
        }
    }

    /**
     * 重试登录
     * 当遇到网络错误时，可以调用此方法重试
     */
    fun retryLogin() {
        viewModelScope.launch {
            val userInfo = GlobalCredentialProvider.storage.getUserInfo()
            if (userInfo != null) {
                login(refreshToken = userInfo.refreshToken)
            } else {
                // 如果没有本地凭据，则需要用户重新登录
                userRepository.updateToLoggedOut()
            }
        }
    }
}