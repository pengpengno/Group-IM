package com.github.im.group.repository

import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.im.group.api.FriendShipApi
import com.github.im.group.db.AppDatabase
import com.github.im.group.model.UserInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


/**
 * 好友关系
 */
class FriendShipRepository(
    val userRepository: UserRepository,
    val friendRequestRepository: FriendRequestRepository,
    val db: AppDatabase
) {


    private val _users = MutableStateFlow<List<UserInfo>>(emptyList())

    /**
     * 加载中
     */
    private val _loading = MutableStateFlow(false)
    var users: StateFlow<List<UserInfo>> = _users.asStateFlow()
    val loading: StateFlow<Boolean> = _loading

    /**
     *  v1 每次调用的时候都通过API 拉去下最新的用户数据
     *  比对id 采用存在则更新  不存在则新增的策略
     *
     */
    suspend fun getFriends () {
        val user = userRepository.withLoggedInUser {
            it.user
        }

        val userId =user.userId


        val userInfos= FriendShipApi.getFriends(userId)

        _users.value = userInfos.mapNotNull { userInfo ->
            userInfo.userInfo
        }

        //更新数据库 DB
        db.transaction {

        }


    }
    
    /**
     * 同步好友关系
     * 使用本地数据库中最大的ID作为基准从服务器获取更新
     */
    suspend fun syncFriends() {
        val user = userRepository.withLoggedInUser {
            it.user
        }
        
        val userId = user.userId
        
        // 获取本地数据库中最大的好友关系ID
        val maxId = db.friendshipQueries.selectMaxId().executeAsOneOrNull()
            .let {
                it?.MAX?:0L
            }
        
        // 同步好友关系
        friendRequestRepository.syncFriendRequests(userId, maxId)
    }


}