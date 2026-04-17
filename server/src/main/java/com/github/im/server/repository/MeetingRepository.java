package com.github.im.server.repository;

import com.github.im.server.model.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByRoomId(String roomId);
    List<Meeting> findByConversation_ConversationIdOrderByCreatedAtDesc(Long conversationId);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Meeting m JOIN m.participants p WHERE p.user.userId = :userId ORDER BY m.createdAt DESC")
    List<Meeting> findByParticipantUserId(Long userId);
}
