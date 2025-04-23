package com.github.im.server.service;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.session.MessageDTO;
import com.github.im.dto.session.MessageSearchRequest;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import com.github.im.server.mapstruct.MessageMapper;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.Message;
import com.github.im.server.model.User;
import com.github.im.server.repository.MessageRepository;
import com.github.im.server.utils.EnumsTransUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    
    private final MessageMapper messageMapper;

    @PersistenceContext
    private EntityManager entityManager;


    // 批量发送消息
    @Transactional
    public List<MessageDTO> sendMessages(List<MessageDTO> messages) {
        List<Message> entities = messages.stream()
                .map(messageMapper::toEntity)
                .collect(Collectors.toList());
        List<Message> savedMessages = messageRepository.saveAll(entities);
        return savedMessages.stream()
                .map(messageMapper::toDTO)
                .collect(Collectors.toList());
    }

    // 拉取历史消息
    public Page<MessageDTO> pullHistoryMessages(Long sessionId, Long fromMessageId, Long toMessageId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return messageRepository.findHistoryMessages(sessionId, fromMessageId, toMessageId, startTime, endTime, pageable)
                .map(messageMapper::toDTO);
    }

    // 搜索消息
    public Page<MessageDTO> searchMessages(MessageSearchRequest request, Pageable pageable) {
        return messageRepository.searchMessages(request.getKeyword(), request.getSessionId(), pageable)
                .map(messageMapper::toDTO);
    }

    // 标记消息为已读
    @Transactional
    public void markAsRead(Long msgId) {
        messageRepository.findById(msgId).ifPresent(message -> {
            message.setStatus(MessageStatus.READ);
            messageRepository.save(message);
        });
    }

//    @Transactional
    public void saveMessage(Chat.ChatMessage chatMessage) {
        var message = new Message();
        var proxy = entityManager.getReference(Conversation.class, chatMessage.getConversationId());
        message.setConversation(proxy);
        message.setContent(chatMessage.getContent());

        var userProxy = entityManager.getReference(User.class,chatMessage.getFromAccountInfo().getUserId());

        message.setFromAccountId(userProxy);
        message.setType(EnumsTransUtil.convertMessageType(chatMessage.getType()));
        message.setStatus(MessageStatus.UNREAD);
        message.setTimestamp(LocalDateTime.now());
        messageRepository.save(message);
    }


}