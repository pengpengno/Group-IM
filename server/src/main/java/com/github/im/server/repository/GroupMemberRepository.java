package com.github.im.server.repository;

import com.github.im.server.model.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<ConversationMember, Long> {

    // 根据 GroupID 查找所有成员
    @Query("SELECT gm FROM ConversationMember gm WHERE gm.conversation.conversationId = :conversationId")
    List<ConversationMember> findByConversationId(Long conversationId);

    // 根据 UserID 查找所有成员

    @Query("SELECT gm FROM ConversationMember gm WHERE gm.user.userId = :userId")
    List<ConversationMember> findByUserId(Long userId);


    // 查找某个用户在某个群组中的成员信息
    @Query("SELECT gm FROM ConversationMember gm WHERE gm.user.userId = :userId and  gm.conversation.conversationId = :conversationId ")
    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);


}
