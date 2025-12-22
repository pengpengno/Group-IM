package com.github.im.server.repository;

import com.github.im.server.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> , JpaSpecificationExecutor<Message> {

    @Query("""
        SELECT m FROM Message m 
        WHERE m.conversation.conversationId = :sessionId 
        AND (:startTime IS NULL OR m.timestamp >= :startTime) 
        AND (:endTime IS NULL OR m.timestamp <= :endTime)
        """)
    Page<Message> findHistoryMessages(@Param("sessionId") Long sessionId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      Pageable pageable);


    /**
     * 查询指定会话的最大 sequence
     */
    @Query("SELECT COALESCE(MAX(m.sequenceId), 0) FROM Message m WHERE m.conversation.conversationId = :conversationId")
    Long findMaxSequenceByConversationId(@Param("conversationId") Long conversationId);


    /**
     * 查询所有会话及其最大 sequence（用于系统初始化）
     */
    @Query("SELECT m.conversation.conversationId as conversationId, COALESCE(MAX(m.sequenceId), 0) as maxSequenceId FROM Message m GROUP BY m.conversation.conversationId")
    List<SequenceRes> findAllConversationMaxSequences();

    @Query("SELECT m FROM Message m WHERE m.conversation.conversationId = :sessionId " +
           "AND m.content LIKE %:keyword%")
    Page<Message> searchMessages(@Param("keyword") String keyword,
                                 @Param("sessionId") Long sessionId,
                                 Pageable pageable);


    interface SequenceRes {

        Long getConversationId();

        Long getMaxSequenceId();

    }
}