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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
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
    public Page<MessageDTO<MessagePayLoad>> pullHistoryMessages(MessagePullRequest request) {
        final var conversationId = request.getConversationId();
        final var startTime = request.getStartTime();
        final var endTime = request.getEndTime();
        final var fromSequenceId = request.getFromSequenceId();
        final Long toSequenceId = request.getToSequenceId();
        final Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Optional.ofNullable(request.getSort())
                        .orElse("createTime")).descending()
        );
          return messageRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("conversation").get("conversationId"), conversationId));
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
            }
            // 获取指定会话中指定时间段内指定序列之后的消息
            if (fromSequenceId != null  ) {
                if (fromSequenceId > 0L){
                    predicates.add(cb.greaterThan(root.get("sequenceId"), fromSequenceId));
                }else {
                    // 如果传入的 fromSequenceId 为0L 则 获取该会话中 最新的条数据 不添加过滤条件

                }
            }
            // 获取指定会话中指定时间段内指定序列之前的消息
            if ( toSequenceId != null && toSequenceId > 0L) {
                predicates.add(cb.lessThan(root.get("sequenceId"), toSequenceId));
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
    public void markAsRead(Long msgId,User user) {
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
        message.setClientMsgId(chatMessage.getClientMsgId());
        var userProxy = entityManager.getReference(User.class,chatMessage.getFromAccountInfo().getUserId());
        // 生成 会话中的消息序列
        message.setSequenceId(conversationSequenceService.nextSequence(chatMessage.getConversationId()));
        message.setFromAccountId(userProxy);
        message.setType(EnumsTransUtil.convertMessageType(chatMessage.getType()));
        if(chatMessage.getMessagesStatus() == Chat.MessagesStatus.SENDING){
            message.setStatus(MessageStatus.SENT);
        }else{
            message.setStatus(EnumsTransUtil.convertMessageStatus(chatMessage.getMessagesStatus()));
        }
        long clientTimeStamp = chatMessage.getClientTimeStamp();
        if(clientTimeStamp == 0L){
            clientTimeStamp = System.currentTimeMillis();
        }
        // 时间戳转为日期
        message.setClientTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(clientTimeStamp), ZoneId.systemDefault()));
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    /**
     * message.getContent()
     * <ul>
     *     <li>TEXT: 纯文本</li>
     *     <li>FILE: 文件  内容为 文件UUid</li>
     * </ul>
     * 出现一场无法解析 获取文件/ 音频内容资源时候，直接返回原始Content
     * @param message
     * @return 返回给到前台战士 的 MessageDto
     */
    @SneakyThrows
    private MessageDTO<MessagePayLoad> convertMessage(Message message) {

        var type = message.getType();

        MessageDTO<MessagePayLoad> dto = messageMapper.toDTO(message);
        try{
            switch(type) {
                case TEXT:
                    dto.setPayload(new DefaultMessagePayLoad(message.getContent()));
                    return dto;
                case VOICE:
                case FILE:
                    final UUID fileID = UUID.fromString(message.getContent());
                    FileMeta fileMeta = fileStorageService.getFileMeta(fileID);
                    dto.setPayload(fileMeta);
                    return dto;
                default:
                    return messageMapper.toDTO(message);
            }
        } catch (Exception e) {
            log.error("error message format ",e);
        }
        dto.setPayload(new DefaultMessagePayLoad(message.getContent()));
        return dto;

    }

}