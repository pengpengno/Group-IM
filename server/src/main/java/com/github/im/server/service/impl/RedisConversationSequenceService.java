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
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sequence.mode", havingValue = "redis")
public class RedisConversationSequenceService implements ConversationSequenceService {

    private final StringRedisTemplate redisTemplate;

    private final MessageRepository messageRepository;

    /**
     *  会话序列号 Redis Key 前缀
     */
    private static final String REDIS_SEQ_KEY_PREFIX = "im:conversation:seq:";

    @Override
    public long nextSequence(Long conversationId) {
        // TODO 段式 预分配 优化
        String redisKey = REDIS_SEQ_KEY_PREFIX + conversationId;
        return Optional.ofNullable(redisTemplate.opsForValue().increment(redisKey)).orElseGet(()-> {

            var maxSequenceByConversationId = messageRepository.findMaxSequenceByConversationId(conversationId);
            redisTemplate.opsForValue().setIfAbsent(redisKey, String.valueOf(maxSequenceByConversationId + 1));
            return maxSequenceByConversationId;
        });
    }

    @Override
    @PostConstruct
    public void initializeSequences() {
        List<MessageRepository.SequenceRes> sequences = messageRepository.findAllConversationMaxSequences();
        // TODO 会话多的情况下 的优化
        List<List<MessageRepository.SequenceRes>> batches = batchList(sequences, 1000); // 每批1000个

        for (List<MessageRepository.SequenceRes> batch : batches) {
            // 使用 pipeline 批量设置
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                for (MessageRepository.SequenceRes seq : batch) {
                    String key = REDIS_SEQ_KEY_PREFIX + seq.getConversationId();
                    //  key 不存在时设置
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
