package com.github.im.server.repository;

import com.github.im.enums.MessageStatus;
import com.github.im.server.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // 根据会话ID查询所有消息
    List<Message> findBySessionId(Long sessionId);

    // 获取某个账户的所有消息
    List<Message> findByFromAccountId(Long fromAccountId);

    // 根据状态查询消息
    List<Message> findByStatus(MessageStatus status);
}
