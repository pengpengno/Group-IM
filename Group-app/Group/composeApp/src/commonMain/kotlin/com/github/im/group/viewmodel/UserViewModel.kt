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
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.SenderSdk
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
    private val loginStateManager: LoginStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserInfo())

    val uiState:  StateFlow<UserInfo> = _uiState.asStateFlow()

    /**
     * 登录状态
     * 用于主页监听  默认情况下使用 true
     *
     */
    private val _LoginState = MutableStateFlow<LoginState>(LoginState.LoggedIn)

    val loginState: StateFlow<LoginState> = _LoginState.asStateFlow()

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
                Napier.d("获取联系人失败: $e")
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
                Napier.d("搜索用户失败: $e")
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
                // TODO: 实现添加好友的API调用
                // 这里需要根据实际的API来实现
                FriendShipApi.addFriend(userId, friendId)
                Napier.d("添加好友: userId=$userId, friendId=$friendId")
            } catch (e: Exception) {
                Napier.d("添加好友失败: $e")
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