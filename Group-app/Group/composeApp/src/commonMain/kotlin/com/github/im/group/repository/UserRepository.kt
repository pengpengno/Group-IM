package com.github.im.group.repository

import com.github.im.group.db.AppDatabase
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.PlatformType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow



//TODO 需要保留登出状态的时候再启用次 sealed
// 初期使用 lateinit 声明 用户信息的必定初始化即可
sealed class UserState {
    object LoggedOut : UserState()  // 尚未登录模式
    data class LoggedIn(val info: CurrentUserInfoContainer) : UserState()
}


data class CurrentUserInfoContainer(
    val user: UserInfo,
    val accountInfo: AccountInfo
)

class UserRepository (
    private val db:AppDatabase
){



//    private var _userState = MutableStateFlow<CurrentUserInfoContainer>(CurrentUserInfoContainer())
//    val userState = _userState.asStateFlow()
//


    private val _userState = MutableStateFlow<UserState>(UserState.LoggedOut)
    val userState = _userState.asStateFlow()

    inline fun <T> withLoggedInUser(action: (CurrentUserInfoContainer) -> T): T {
        val user = requireLoggedInUser() // 抛异常提示未登录
        return action(user)
    }



    fun requireLoggedInUser(): CurrentUserInfoContainer{
        return when (val state = userState.value){
            is UserState.LoggedIn -> state.info
            is UserState.LoggedOut ->{
                print("用户未登录")
//                null
                throw IllegalStateException("用户未登录")
            }
        }
    }

    /**
     * 保存当前用户至 数据库
     * 同时 将当前用户 绑定 APP
     */
    fun saveCurrentUser(user: UserInfo) {

        val accountInfo = AccountInfo(
            account = user.username,
            accountName = user.username,
            userId = user.userId,
            eMail = user.email,
            platformType = PlatformType.ANDROID,
        )
//        _userState.value = CurrentUserInfoContainer(user, accountInfo)
        _userState.value = UserState.LoggedIn(CurrentUserInfoContainer(user, accountInfo))
        addUser(user)
    }



    /**
     * 插入一条用户信息
     * 用户不存在时才会插入， 存在则忽略
     */
    private  fun addUser(user: UserInfo) {
        db.transaction {
            // 先检查用户是否存在
            val existingUser = db.userQueries.selectByUsername(user.username).executeAsOneOrNull()
            if (existingUser == null) {
                // 用户不存在则插入
                db.userQueries.insertUser(
                    userId = user.userId,
                    username = user.username,
                    email = user.email,
                    phoneNumber = "",
                )
            }
        }
    }
}