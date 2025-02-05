package com.github.im.server.repository;

import com.github.im.server.model.Conversation;
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
//    @Query("SELECT c FROM Conversation c WHERE c.user.userId = :userId")
    List<Conversation> findByMembers_User_UserId(Long userId);
}
