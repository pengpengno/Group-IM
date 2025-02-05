package com.github.im.server.repository;

import com.github.im.server.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // 根据 GroupID 查找所有成员
    @Query("SELECT gm FROM GroupMember gm WHERE gm.conversation.conversationId = :conversationId")
    List<GroupMember> findByConversationId(Long conversationId);

    // 根据 UserID 查找所有成员

    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.userId = :userId")
    List<GroupMember> findByUserId(Long userId);


    // 查找某个用户在某个群组中的成员信息
    @Query("SELECT gm FROM GroupMember gm WHERE gm.user.userId = :userId and  gm.conversation.conversationId = :conversationId ")
    Optional<GroupMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    // 查找某个群组内，具有某种角色的所有成员
//    List<GroupMember> findByGroupIdAndRole(Long groupId, Role role);
}
