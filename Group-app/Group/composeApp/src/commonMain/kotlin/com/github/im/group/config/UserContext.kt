package com.github.im.group.config

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.flow.MutableStateFlow

//class UserContext :  ViewModel() {
//@Composable
//fun UserContext :   ViewModel() {
fun UserContext() {
    private val _uiState = MutableStateFlow(OrderUiState(pickupOptions = pickupOptions()))

//    MutableStateFlow<UserInfo>
//    private val _userInfo = MutableState<UserInfo?>()
//    val userInfo: StateFlow<UserInfo?> = _userInfo

    fun login(user: UserInfo) {
        _userInfo.value = user
    }

    fun logout() {
        _userInfo.value = null
    }
}