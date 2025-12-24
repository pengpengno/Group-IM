package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.FriendShipApi
import com.github.im.group.api.FriendshipDTO
import com.github.im.group.api.LoginApi
import com.github.im.group.api.UserApi
import com.github.im.group.manager.LoginStateManager
import com.github.im.group.model.UserInfo
import com.github.im.group.repository.FriendRequestRepository
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
    object LoggedIn : LoginState()  // 已成功登录
    object LoggedFailed : LoginState()  // 登录失败
    object Logging : LoginState()  // 登录中
}

class UserViewModel(
    val userRepository: UserRepository,
    private val loginStateManager: LoginStateManager,
    private val friendRequestRepository: FriendRequestRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserInfo())

    val uiState:  StateFlow<UserInfo> = _uiState.asStateFlow()

    /**
     * 登录状态
     * 用于主页监听  默认情况下使用 true
     */
    private val _LoginState = MutableStateFlow<LoginState>(LoginState.LoggedIn)

    val loginState: StateFlow<LoginState> = _LoginState.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendshipDTO>>(emptyList())
    val friends: StateFlow<List<FriendshipDTO>> = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserInfo>>(emptyList())
    val searchResults: StateFlow<List<UserInfo>> = _searchResults.asStateFlow()
    
    // 待处理好友请求数量状态
    private val _pendingFriendRequestsCount = MutableStateFlow<Long>(0)
    val pendingFriendRequestsCount: StateFlow<Long> = _pendingFriendRequestsCount.asStateFlow()
    
    // 待处理好友请求列表
    private val _pendingFriendRequests = MutableStateFlow<List<FriendshipDTO>>(emptyList())
    val pendingFriendRequests: StateFlow<List<FriendshipDTO>> = _pendingFriendRequests.asStateFlow()

    fun getCurrentUser() : UserInfo{
       return userRepository.withLoggedInUser { it.user }
    }


    /**
     * 获取联系人列表
     */
    fun loadFriends() {
        viewModelScope.launch {
            try {
                val currentUser = getCurrentUser()
                if (currentUser.userId != 0L) {
                    val friendList = FriendShipApi.getFriends(currentUser.userId)
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

    fun updateUserInfo(userInfo: UserInfo){
        userRepository.saveCurrentUser(userInfo)
        // TODO  API 更新 服务端信息
    }

    /**
     * 添加好友
     */
    fun addFriend(userId: Long, friendId: Long) {
        viewModelScope.launch {
            try {
                FriendShipApi.addFriend(userId, friendId,"")
                Napier.d("添加好友: userId=$userId, friendId=$friendId")
            } catch (e: Exception) {
                Napier.d("添加好友失败: $e")
            }
        }
    }
    
    /**
     * 加载已发送的好友请求
     */
    fun loadSentFriendRequests(userId: Long) {
        viewModelScope.launch {
            try {
                // 1. 从服务端获取好友请求
                val sentRequests = FriendShipApi.getSentFriendRequests(userId)
                
                // 2. 保存到本地数据库
                friendRequestRepository.saveFriendRequests(sentRequests)
                
                // 3. 从本地数据库加载并更新状态
                val localRequests = friendRequestRepository.getSentFriendRequests(userId)
                _pendingFriendRequests.value = localRequests
                Napier.d("加载已发送的好友请求: ${sentRequests.size} 个")
            } catch (e: Exception) {
                Napier.e("加载已发送的好友请求失败: $e")
            }
        }
    }
    
    /**
     * 加载收到的好友请求（用于显示未读请求）
     */
    fun loadReceivedFriendRequests(userId: Long) {
        viewModelScope.launch {
            try {
                // 这里可以添加从服务端获取最新好友请求的逻辑
                // 然后保存到本地数据库
                // 最后从本地数据库加载显示
                
                // 获取未读好友请求数量
                val pendingCount = friendRequestRepository.getPendingFriendRequestsCount(userId)
                _pendingFriendRequestsCount.value = pendingCount
                
                // 获取待处理的好友请求列表
                val pendingRequests = friendRequestRepository.getReceivedFriendRequests(userId)
                _pendingFriendRequests.value = pendingRequests.filter { 
                    it.status == com.github.im.group.db.entities.FriendRequestStatus.PENDING 
                }
                
                Napier.d("未读好友请求数量: $pendingCount")
            } catch (e: Exception) {
                Napier.e("加载收到的好友请求失败: $e")
            }
        }
    }
    
    /**
     * 获取待处理的好友请求数量
     */
    fun getPendingFriendRequestsCount(userId: Long): Long {
        return friendRequestRepository.getPendingFriendRequestsCount(userId)
    }
    
    /**
     * 接受好友请求
     */
    fun acceptFriendRequest(requestId: Long) {
        viewModelScope.launch {
            try {
                // 更新本地数据库状态
                friendRequestRepository.updateFriendRequestStatus(
                    requestId, 
                    com.github.im.group.db.entities.FriendRequestStatus.ACCEPTED
                )
                
                // 重新加载好友列表和待处理请求
                loadFriends()
                val currentUser = getCurrentUser()
                if (currentUser.userId != 0L) {
                    loadReceivedFriendRequests(currentUser.userId)
                }
                
                Napier.d("已接受好友请求: $requestId")
            } catch (e: Exception) {
                Napier.e("接受好友请求失败: $e")
            }
        }
    }
    
    /**
     * 拒绝好友请求
     */
    fun rejectFriendRequest(requestId: Long) {
        viewModelScope.launch {
            try {
                // 更新本地数据库状态
                friendRequestRepository.updateFriendRequestStatus(
                    requestId, 
                    com.github.im.group.db.entities.FriendRequestStatus.REJECTED
                )
                
                // 重新加载待处理请求
                val currentUser = getCurrentUser()
                if (currentUser.userId != 0L) {
                    loadReceivedFriendRequests(currentUser.userId)
                }
                
                Napier.d("已拒绝好友请求: $requestId")
            } catch (e: Exception) {
                Napier.e("拒绝好友请求失败: $e")
            }
        }
    }
    
    /**
     * 自动登录
     * 如果本地存储了 用户信息 则尝试自动登录
     */
    fun autoLogin(){
        viewModelScope.launch {
            val userInfo = GlobalCredentialProvider.storage.getUserInfo()
            if (userInfo != null) {
                // 尝试自动登录
               login( refreshToken = userInfo.refreshToken)
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
            _LoginState.value = LoginState.Logging

            try {
                val response = LoginApi.login(uname, pwd,refreshToken)

                GlobalCredentialProvider.storage.saveUserInfo(response)
                GlobalCredentialProvider.currentToken = response.token
                GlobalCredentialProvider.currentUserId = response.userId
                GlobalCredentialProvider.companyId = response.currentLoginCompanyId

                _uiState.value = response

                // 通知登录状态管理器用户已登录
                loginStateManager.setLoggedIn(response)
                _LoginState.value  = LoginState.LoggedIn
                // 登录成功返回true
                return true
            } catch (e: Exception) {
                Napier.d { "登录错误 ： ${e.message}" }
                _LoginState.value  = LoginState.LoggedFailed
                Napier.d("loginState ${_LoginState.value}")
                return false
            } finally {
//                _loading.value = false
            }


    }
    
    /**
     * 登出方法
     */
    fun logout() {
        loginStateManager.setLoggingOut()
        try {

            // 清除用户信息
            runBlocking {
                GlobalCredentialProvider.storage.clearUserInfo()
                GlobalCredentialProvider.currentToken = ""
            }

            // 通知登录状态管理器用户已登出
            loginStateManager.setLoggedOut()
        } catch (e: Exception) {
            Napier.d { "登出错误 ： ${e.message}" }
        }
    }

}