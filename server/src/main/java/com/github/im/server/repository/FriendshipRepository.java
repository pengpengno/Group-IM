package com.github.im.server.repository;

import com.github.im.server.model.Friendship;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

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
     * 根据好友关系  查询哈有
     * @param user 用户
     * @param status  状态
     * @return 返回好友关系
     */
    // 获取指定用户的所有好友关系（已接受的好友）
    List<Friendship> findByUserAndStatus(User user, Status status);

}
