package com.github.im.server.service;

import com.github.im.dto.session.MessageDTO;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import com.github.im.server.model.Message;
import com.github.im.server.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    // 发送消息
    @Transactional
    public MessageDTO sendMessage(Long sessionId, String content, Long fromAccountId, Long toAccountId, MessageType type) {
        Message message = new Message(sessionId, content, fromAccountId, toAccountId, type, MessageStatus.UNREAD);
        messageRepository.save(message);
        return convertToDTO(message);
    }

    // 获取会话中的所有消息
    public List<MessageDTO> getMessages(Long sessionId) {
        List<Message> messages = messageRepository.findBySessionId(sessionId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 获取某个用户的所有消息
    public List<MessageDTO> getMessagesByUser(Long fromAccountId) {
        List<Message> messages = messageRepository.findByFromAccountId(fromAccountId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // 标记消息为已读
    @Transactional
    public void markAsRead(Long msgId) {
        messageRepository.findById(msgId).ifPresent(message -> {
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);
        });
    }

    // 将 Message 实体类转换为 DTO
    private MessageDTO convertToDTO(Message message) {
        return new MessageDTO(message.getMsgId(), message.getSessionId(), message.getContent(), message.getFromAccountId(), message.getToAccountId(), message.getType(), message.getStatus(), message.getTimestamp());
    }
}
