package com.github.im.server.repository;

import com.github.im.server.model.Conversation;
import com.github.im.enums.ConversationStatus;
import com.github.im.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // 根据群组名称查询群组
    Optional<Conversation> findByGroupName(String groupName);

    // 查询某个用户的所有群组
    List<Conversation> findByMembers_User_UserId(Long userId);

    // 查询某个用户正在进行的群组
    @Query("SELECT c FROM Conversation c JOIN c.members m WHERE m.user.userId = :userId AND c.status = :status")
    List<Conversation> findActiveConversationsByUserId(Long userId, ConversationStatus status);

    // 查询两个用户之间的私聊会话
    @Query("SELECT c FROM Conversation c JOIN c.members m1 JOIN c.members m2 " +
           "WHERE m1.user.userId = :userId1 AND m2.user.userId = :userId2 " +
           "AND c.conversationType = :conversationType AND c.status = :status")
    Optional<Conversation> findPrivateChatBetweenUsers(Long userId1, Long userId2, ConversationType conversationType, ConversationStatus status);
}