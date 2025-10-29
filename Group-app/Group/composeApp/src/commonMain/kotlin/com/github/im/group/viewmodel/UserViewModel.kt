package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.FriendshipDTO
import com.github.im.group.api.LoginApi
import com.github.im.group.api.PageResult
import com.github.im.group.api.UserApi
import com.github.im.group.listener.LoginStateManager
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.repository.CurrentUserInfoContainer
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.SenderSdk
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class UserViewModel(
    private val senderSdk: SenderSdk,
    val userRepository: UserRepository,
    private val loginStateManager: LoginStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserInfo>(UserInfo())

    val uiState:  StateFlow<UserInfo> = _uiState.asStateFlow()

    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading

    private val _friends = MutableStateFlow<List<FriendshipDTO>>(emptyList())
    val friends: StateFlow<List<FriendshipDTO>> = _friends.asStateFlow()

    private val _searchResults = MutableStateFlow<List<UserInfo>>(emptyList())
    val searchResults: StateFlow<List<UserInfo>> = _searchResults.asStateFlow()


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
                    val friendList = com.github.im.group.api.FriendShipApi.getFriends(currentUser.userId)
                    _friends.value = friendList
                }
            } catch (e: Exception) {
                println("获取联系人失败: $e")
            }
        }
    }

    /**
     * 查询用户
     */
    fun searchUser(queryString: String) {
        if (queryString.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val result = UserApi.findUser(queryString)
                _searchResults.value = result.content
            } catch (e: Exception) {
                println("搜索用户失败: $e")
                _searchResults.value = emptyList()
            }
        }
    }

    fun updateUserInfo(userInfo: UserInfo){
        userRepository.saveCurrentUser(userInfo)
    }

    /**
     * 添加好友
     */
    fun addFriend(userId: Long, friendId: Long) {
        viewModelScope.launch {
            try {
                // TODO: 实现添加好友的API调用
                // 这里需要根据实际的API来实现
                println("添加好友: userId=$userId, friendId=$friendId")
            } catch (e: Exception) {
                println("添加好友失败: $e")
            }
        }
    }

    /**
     * 是否允许自动登录
     */
    suspend fun autoLogin(): Boolean {
        val userInfo = GlobalCredentialProvider.storage.getUserInfo()
        return userInfo?.refreshToken != null
    }

    /**
     * 登录的方法
     *
     */
    suspend fun login(uname: String ="",
                      pwd:String ="",
                      refreshToken:String =""): Boolean {
//        viewModelScope.launch {
            _loading.value = true
            loginStateManager.setLoggingIn()
            try {
                val response = LoginApi.login(uname, pwd,refreshToken)

                GlobalCredentialProvider.storage.saveUserInfo(response)
                GlobalCredentialProvider.currentToken = response.token

                _uiState.value = response
                // 进程中保存用户
                userRepository.saveCurrentUser(response)
////                // 长连接到服务端远程
//                senderSdk.loginConnect()

                // 通知登录状态管理器用户已登录
                loginStateManager.setLoggedIn(response)
                
                // 登录成功返回true
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                Napier.d { "登录错误 ： ${e.message}" }
                // 登录失败返回false
                return false
            } finally {
                _loading.value = false
            }


    }
    
    /**
     * 登出方法
     */
    fun logout() {
        loginStateManager.setLoggingOut()
        try {

            // 停止自动重连
            senderSdk.stopAutoReconnect()
            
            // 清除用户信息
            runBlocking {
                GlobalCredentialProvider.storage.clearUserInfo()
                GlobalCredentialProvider.currentToken = ""
            }

            
            // 清除 UserRepository 中的用户状态
            // 这里我们不直接修改 UserRepository 的状态，因为它是密封类
            // 而是通过通知监听器来处理
            
            // 通知登录状态管理器用户已登出
            loginStateManager.setLoggedOut()
        } catch (e: Exception) {
            Napier.d { "登出错误 ： ${e.message}" }
        }
    }

}