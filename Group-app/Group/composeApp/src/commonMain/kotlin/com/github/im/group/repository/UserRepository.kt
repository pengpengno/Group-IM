package com.github.im.group.repository

import com.github.im.group.GlobalCredentialProvider
import com.github.im.group.Platform
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.UserStatus
import com.github.im.group.getPlatform
import com.github.im.group.model.UserInfo
import com.github.im.group.model.proto.AccountInfo
import com.github.im.group.model.proto.PlatformType
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow



//TODO 需要保留登出状态的时候再启用次 sealed
// 初期使用 lateinit 声明 用户信息的必定初始化即可
sealed class UserState {
    object LoggedOut : UserState()  // 尚未登录模式

    object Logging : UserState()   // 登录中
    data class LoggedIn(val info: CurrentUserInfoContainer) : UserState()
}


data class CurrentUserInfoContainer(
    val user: UserInfo,
    val accountInfo: AccountInfo
)

class UserRepository (
    private val db:AppDatabase
){

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
                Napier.d("用户未登录")
                // 从数据库获取用户信息
//                GlobalCredentialProvider.storage.getUserInfo()?.let {
//                    saveCurrentUser(it)
//                }

                throw IllegalStateException("用户未登录")
            }
            is UserState.Logging -> {
                Napier.d("用户登录中")
                throw IllegalStateException("用户登录中")
            }
        }
    }

    /**
     * 保存当前用户至 数据库 ， 存在则更新信息
     * 同时 将当前用户 绑定 APP
     */
    fun saveCurrentUser(user: UserInfo) {

        val accountInfo = AccountInfo(
            account = user.username,
            accountName = user.username,
            userId = user.userId,
            eMail = user.email,
            accessToken = user.token,
            platformType = PlatformType.valueOf(getPlatform().name),
        )
        _userState.value = UserState.LoggedIn(CurrentUserInfoContainer(user, accountInfo))
        addOrUpdateUser(user)
    }



    /**
     * 插入或更新用户信息
     * 用户不存在时才会插入， 存在则更新
     */
    fun addOrUpdateUsers(users: List<UserInfo>) {
        db.transaction {
            users.forEach { user ->
                addOrUpdateUser(user)
            }
        }
    }

    /**
     * 插入一条用户信息
     * 用户不存在时才会插入， 存在则忽略
     */
    fun addOrUpdateUser(user: UserInfo) {
        db.transaction {
            // 先检查用户是否存在
            val existingUser = db.userQueries.selectById(user.userId).executeAsOneOrNull()
            if (existingUser == null) {
                // 用户不存在则插入
                db.userQueries.insertUser(
                    userId = user.userId,
                    username = user.username,
                    email = user.email,
                    phoneNumber = "",
                    avatarUrl = null,
                    bio = null,
                    userStatus = UserStatus.ACTIVE
                )
            } else {
                db.userQueries.updateUser(
                    username = user.username,
                    email = user.email,
                    phoneNumber = "",
                    avatarUrl = null,
                    bio = null,
                    userStatus = UserStatus.ACTIVE,
                    userId = existingUser.userId
                )
            }
        }
    }
    
    /**
     * 根据用户ID获取用户信息
     */
    fun getUserById(userId: Long): UserInfo? {
        val userEntity = db.userQueries.selectById(userId).executeAsOneOrNull()
        return userEntity?.let {
            UserInfo(
                userId = it.userId,
                username = it.username,
                email = it.email ?: "",
                token = "",
                refreshToken = ""
            )
        }
    }
}