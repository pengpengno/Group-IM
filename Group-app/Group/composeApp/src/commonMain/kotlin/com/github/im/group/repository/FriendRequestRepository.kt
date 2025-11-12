package com.github.im.group.repository

import com.github.im.group.api.FriendShipApi
import com.github.im.group.api.FriendshipDTO
import com.github.im.group.db.AppDatabase
import com.github.im.group.db.entities.FriendRequestStatus
import com.github.im.group.model.UserInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 好友关系本地存储仓库
 */
class FriendRequestRepository(
    private val db: AppDatabase
) {
    
    /**
     * 保存好友关系到本地数据库
     */
    fun saveFriendRequest(friendshipDTO: FriendshipDTO) {
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        db.friendshipQueries.insertFriendship(
            id = friendshipDTO.id ?: 0,
            from_user_id = friendshipDTO.userInfo?.userId ?: 0,
            to_user_id = friendshipDTO.friendUserInfo?.userId ?: 0,
            status = friendshipDTO.status ?: FriendRequestStatus.PENDING,
            conversation_id = friendshipDTO.conversationId,
            created_at = currentTime,
            updated_at = currentTime
        )
    }
    
    /**
     * 批量保存好友关系
     */
    fun saveFriendRequests(friendshipDTOs: List<FriendshipDTO>) {
        if (friendshipDTOs.isEmpty()) return
        
        db.transaction {
            friendshipDTOs.forEach { friendshipDTO ->
                saveFriendRequest(friendshipDTO)
            }
        }
    }
    
    /**
     * 获取待处理的好友请求数量
     */
    fun getPendingFriendRequestsCount(userId: Long): Long {
        return db.friendshipQueries.selectPendingRequestsCount(userId).executeAsOneOrNull() ?: 0
    }
    
    /**
     * 获取用户收到的所有好友请求
     */
    fun getReceivedFriendRequests(userId: Long): List<FriendshipDTO> {
        return db.friendshipQueries.selectReceivedRequests(userId).executeAsList().map { entity ->
            FriendshipDTO(
                id = entity.id,
                userInfo = UserInfo(userId = entity.from_user_id),
                friendUserInfo = UserInfo(userId = entity.to_user_id),
                conversationId = entity.conversation_id,
                status = entity.status
            )
        }
    }
    
    /**
     * 获取用户发送的所有好友请求
     */
    fun getSentFriendRequests(userId: Long): List<FriendshipDTO> {
        return db.friendshipQueries.selectSentRequests(userId).executeAsList().map { entity ->
            FriendshipDTO(
                id = entity.id,
                userInfo = UserInfo(userId = entity.from_user_id),
                friendUserInfo = UserInfo(userId = entity.to_user_id),
                conversationId = entity.conversation_id,
                status = entity.status
            )
        }
    }
    
    /**
     * 更新好友关系状态
     */
    fun updateFriendRequestStatus(requestId: Long, status: FriendRequestStatus) {
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        db.friendshipQueries.updateFriendshipStatus(status, currentTime, requestId)
    }
    
    /**
     * 同步好友关系
     * 从服务器获取最新的好友关系并保存到本地数据库
     * @param userId 用户ID
     * @param maxId 客户端目前最大的关系ID，只获取比这个ID大的数据
     * @return 从服务器获取到的新好友关系列表
     */
    suspend fun syncFriendRequests(userId: Long, maxId: Long = 0): List<FriendshipDTO> {
        // 从服务器获取最新的好友关系
        val friendshipDTOs = FriendShipApi.syncFriendRequests(userId, maxId)
        
        // 保存到本地数据库
        saveFriendRequests(friendshipDTOs)
        
        return friendshipDTOs
    }
}