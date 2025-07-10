package com.github.im.server.service;

import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.session.*;
import com.github.im.enums.MessageStatus;
import com.github.im.enums.MessageType;
import com.github.im.server.mapstruct.MessageMapper;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.FileResource;
import com.github.im.server.model.Message;
import com.github.im.server.model.User;
import com.github.im.server.repository.MessageRepository;
import com.github.im.server.utils.EnumsTransUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    
    private final MessageMapper messageMapper;

    private final FileStorageService fileStorageService;

    private final ConversationSequenceService conversationSequenceService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public MessageDTO<MessagePayLoad> getMessageById(Long msgId) {
        return messageRepository.findById(msgId)
                .map(this::convertMessage)
                .orElseThrow(()-> new IllegalStateException("消息不存在"));
    }

    // 拉取历史消息
    @Transactional(readOnly = true)
    public Page<MessageDTO<MessagePayLoad>> pullHistoryMessages(Long sessionId,  LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
          return messageRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("conversation").get("conversationId"), sessionId));
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable)
        .map(this::convertMessage);
    }

    // 搜索消息
    public Page<MessageDTO<MessagePayLoad>> searchMessages(MessageSearchRequest request, Pageable pageable) {
        return messageRepository.searchMessages(request.getKeyword(), request.getSessionId(), pageable)
                .map(this::convertMessage);
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
    public Message saveMessage(Chat.ChatMessage chatMessage) {
        var message = new Message();
        var proxy = entityManager.getReference(Conversation.class, chatMessage.getConversationId());
        message.setConversation(proxy);
        message.setContent(chatMessage.getContent());

        var userProxy = entityManager.getReference(User.class,chatMessage.getFromAccountInfo().getUserId());
        // 生成 会话中的消息序列
        message.setSequenceId(conversationSequenceService.nextSequence(chatMessage.getConversationId()));
        message.setFromAccountId(userProxy);
        message.setType(EnumsTransUtil.convertMessageType(chatMessage.getType()));
        message.setStatus(MessageStatus.UNREAD);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }


    @SneakyThrows
    private MessageDTO<MessagePayLoad> convertMessage(Message message) {

        var type = message.getType();

        MessageDTO<MessagePayLoad> dto = messageMapper.toDTO(message);
//        dto.gs(message.getFromAccountId());

        switch(type) {
            case TEXT:
                dto.setPayload(new DefaultMessagePayLoad(message.getContent()));
                return dto;
            case FILE:
                var fileResourceById = fileStorageService.getFileResourceById(message.getContent());
                var fileMeta = FileMeta.builder()
                        .filename(fileResourceById.getOriginalName())
                        .fileSize(fileResourceById.getSize())
                        .contentType(fileResourceById.getContentType())
                        .hash(fileResourceById.getHash())
                        .build();
                dto.setPayload(fileMeta);
                return dto;
            default:
                return messageMapper.toDTO(message);
        }
    }

}