package com.github.im.group.viewmodel

import androidx.lifecycle.ViewModel
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class UserViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<UserInfo>(UserInfo())

    val uiState:  StateFlow<UserInfo> = _uiState.asStateFlow()


    suspend fun updateUserInfo(userInfo: UserInfo) {
//        _uiState.emit(userInfo)
        _uiState.value = userInfo
        _uiState.emit(userInfo)
    }


}
