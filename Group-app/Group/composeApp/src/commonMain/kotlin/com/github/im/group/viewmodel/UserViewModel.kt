package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.api.LoginApi
import com.github.im.group.api.PageResult
import com.github.im.group.api.UserApi
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.repository.UserRepository
import com.github.im.group.sdk.SenderSdk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class UserViewModel(
    private val senderSdk: SenderSdk,
    val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserInfo>(UserInfo())

    val uiState:  StateFlow<UserInfo> = _uiState.asStateFlow()

    private val _loading = MutableStateFlow(false)

    val loading: StateFlow<Boolean> = _loading


    fun getUser(): UserInfo? {
        return userRepository.getUser()
    }


    fun getAccountInfo(): AccountInfo? {
        return userRepository.getAccountInfo()
    }

    /**
     * 查询用户
     */
    suspend fun queryUser(queryString:String): PageResult<UserInfo>{
//        viewModelScope.launch {
            return UserApi.findUser(queryString)

//        }

    }

    /**
     * 是否允许自动登录
     */
    suspend fun autoLogin(): Boolean {
        val userInfo = GlobalCredentialProvider.storage.getUserInfo()
        return userInfo?.token != null
    }

    suspend fun login(uname: String ="",
                      pwd:String ="",
                      refreshToken:String ="") {
//        viewModelScope.launch {
            _loading.value = true
            try {
                val response = LoginApi.login(uname, pwd,refreshToken)

                GlobalCredentialProvider.storage.saveUserInfo(response)
                GlobalCredentialProvider.currentToken = response.token

                _uiState.value = response
                // 内存中保存用户
                userRepository.saveUser(response)
                // 长连接到服务端远程
                senderSdk.loginConnect(response)

            } catch (e: Exception) {
                println("加载失败: $e")
            } finally {
                _loading.value = false
            }


//        }

    }

}
