package com.github.im.server.service.impl;

import com.github.im.server.repository.MessageRepository;
import com.github.im.server.service.ConversationSequenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/***
 * 进程缓存实现 会话  序列号自增
 * 单节点状态 没有引入 redis 的时候用这个
 */
@Service
@ConditionalOnProperty(name = "group.sequence.mode", havingValue = "memory")
@RequiredArgsConstructor
public class InMemoryConversationSequenceService implements ConversationSequenceService {

    private final ConcurrentHashMap<Long, AtomicLong> conversationSequences = new ConcurrentHashMap<>();

    private final MessageRepository messageRepository;


    /**
     * 进程中的 conversationId -> sequence 缓存
     * 每次重启都要保证 conversation 中的sequence 已经同步至缓存
     */
    @PostConstruct
    public void initializeSequences() {
        // 1. 查询所有会话的最新 sequence
        List<MessageRepository.SequenceRes> conversationMaxSequences = messageRepository.findAllConversationMaxSequences();
        for (MessageRepository.SequenceRes record: conversationMaxSequences) {

            conversationSequences.put(record.getConversationId(), new AtomicLong(record.getMaxSequenceId()));
        }
    }


    @Override
    public long getMaxSequence(Long conversationId) {
        // 1. 尝试从缓存中获取 sequence
        var sequence = conversationSequences.get(conversationId);
        if (sequence != null) {
            return sequence.get();
        }
        // 2. 如果缓存中没有，则从数据库中获取
        Long maxSequenceByConversationId = messageRepository.findMaxSequenceByConversationId(conversationId);
        conversationSequences.put(conversationId, new AtomicLong(maxSequenceByConversationId));
        return maxSequenceByConversationId;
    }

    @Override
    public long nextSequence(Long conversationId) {
        return conversationSequences
                .computeIfAbsent(conversationId, id ->{
                    var nextSequenceId = messageRepository.findMaxSequenceByConversationId(id);
                    return new AtomicLong(nextSequenceId);
                })
                .incrementAndGet();
    }
}