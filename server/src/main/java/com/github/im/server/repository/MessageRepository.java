package com.github.im.server.repository;

import com.github.im.server.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :sessionId " +
           "AND (:fromMessageId IS NULL OR m.msgId > :fromMessageId) " +
           "AND (:toMessageId IS NULL OR m.msgId < :toMessageId) " +
           "AND (:startTime IS NULL OR m.timestamp >= :startTime) " +
           "AND (:endTime IS NULL OR m.timestamp <= :endTime)")
    Page<Message> findHistoryMessages(@Param("sessionId") Long sessionId,
                                      @Param("fromMessageId") Long fromMessageId,
                                      @Param("toMessageId") Long toMessageId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :sessionId " +
           "AND m.content LIKE %:keyword%")
    Page<Message> searchMessages(@Param("keyword") String keyword,
                                 @Param("sessionId") Long sessionId,
                                 Pageable pageable);
}