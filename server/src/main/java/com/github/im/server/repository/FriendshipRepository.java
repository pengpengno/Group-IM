package com.github.im.server.repository;

import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 *
 */
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    /**
     *
     * @param user
     * @param friend
     * @return
     */
    Optional<Friendship> findByUserAndFriend(User user, User friend);

    /**
     * 根据用户ID 和 好友ID 查询好友关系
     * @param userId  用户Id
     * @param friendId 好友Id
     * @return 好友关系
     */
    Optional<Friendship> findByUser_UserIdAndFriend_UserId(Long  userId, Long friendId);



    // 检查两个用户之间是否存在好友关系（任意方向）
    @Query("""
        SELECT f FROM Friendship f \s
        WHERE (f.user.userId = :userId1 AND f.friend.userId = :userId2)
           OR (f.user.userId = :userId2 AND f.friend.userId = :userId1)
   \s""")
    Optional<List<Friendship>> findBetweenUsers(@Param("userId1") Long userId1,
                                          @Param("userId2") Long userId2);
    /**
     * 根据好友关系  查询哈有
     * @param user 用户
     * @param status  状态
     * @return 返回好友关系
     */
    // 获取指定用户的所有好友关系（已接受的好友）
    List<Friendship> findByUserAndStatus(User user, Status status);

    /**
     * 根据用户ID 查询所有好友关系 包括申请与被申请(被申请的只 查询 {@link Status#PENDING PENDING} 的)
     *
     * @param userId 用户ID
     * @return 好友关系
     */
    @Query("SELECT f FROM Friendship f WHERE f.user.userId = ?1 OR (f.friend.userId = ?1 AND f.status = 'PENDING')")
    List<Friendship> findByUserIdOrFriendIdPENDING(Long userId);
    
    /**
     * 同步好友请求
     * 根据客户端传入的最大关系ID，获取之后的新关系数据
     * @param userId 用户ID
     * @param maxId 客户端当前最大的关系ID
     * @return 新的好友关系列表
     */
    @Query("SELECT f FROM Friendship f WHERE (f.user.userId = ?1 OR f.friend.userId = ?1) AND f.id > ?2 ORDER BY f.id ASC")
    List<Friendship> findFriendshipsAfterId(Long userId, Long maxId);
}
