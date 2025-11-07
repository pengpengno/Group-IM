package com.github.im.server.service.impl;

import com.github.im.server.repository.MessageRepository;
import com.github.im.server.service.ConversationSequenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 使用 Redis 处理 自增
 * 保证全局唯一性和严格顺序性
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sequence.mode", havingValue = "redis")
public class RedisConversationSequenceService implements ConversationSequenceService {

    private final StringRedisTemplate redisTemplate;

    private final MessageRepository messageRepository;

    /**
     * 会话序列号 Redis Key 前缀
     */
    private static final String REDIS_SEQ_KEY_PREFIX = "im:conversation:seq:";

    @Override
    public long nextSequence(Long conversationId) {
        // 每次都直接从Redis获取，保证全局顺序性，避免不同服务器间的sequence冲突
        String redisKey = REDIS_SEQ_KEY_PREFIX + conversationId;
        return Optional.ofNullable(redisTemplate.opsForValue().increment(redisKey))
                .orElseGet(() -> {
                    // 如果key不存在，从数据库获取当前最大sequence并+1
                    var maxSequenceByConversationId = messageRepository.findMaxSequenceByConversationId(conversationId);
                    redisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(maxSequenceByConversationId + 1));
                    return maxSequenceByConversationId + 1;
                });
    }

    @Override
    public long getMaxSequence(Long conversationId) {

        return Optional.ofNullable(redisTemplate.opsForValue().get(REDIS_SEQ_KEY_PREFIX + conversationId))
                .map(Long::parseLong)
                .orElseGet(() -> {
                    // 如果key不存在，从数据库获取当前最大sequence并+1
                    return messageRepository.findMaxSequenceByConversationId(conversationId);
                });

    }

    @Override
    @PostConstruct
    public void initializeSequences() {
        //TODO 初始化 优化
        List<MessageRepository.SequenceRes> sequences = messageRepository.findAllConversationMaxSequences();
        // 批量初始化所有会话的sequence到Redis中
        List<List<MessageRepository.SequenceRes>> batches = batchList(sequences, 1000);

        for (List<MessageRepository.SequenceRes> batch : batches) {
            // 使用 pipeline 批量设置
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                for (MessageRepository.SequenceRes seq : batch) {
                    String key = REDIS_SEQ_KEY_PREFIX + seq.getConversationId();
                    // 只有当key不存在时才设置初始值
                    stringRedisConn.setNX(key, String.valueOf(seq.getMaxSequenceId()));
                }
                return null;
            });
        }
    }

    private <T> List<List<T>> batchList(List<T> list, int batchSize) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return result;
    }
}