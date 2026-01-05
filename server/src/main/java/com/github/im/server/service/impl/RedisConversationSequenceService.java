package com.github.im.server.service.impl;

import com.github.im.server.repository.MessageRepository;
import com.github.im.server.service.ConversationSequenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Duration;

/**
 * 使用 Redis 处理 自增
 * 保证全局唯一性和严格顺序性
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "group.sequence.mode", havingValue = "redis")
@Slf4j
public class RedisConversationSequenceService implements ConversationSequenceService {

    private final StringRedisTemplate redisTemplate;

    private final MessageRepository messageRepository;

    /**
     * 会话序列号 Redis Key 前缀
     */
    private static final String REDIS_SEQ_KEY_PREFIX = "im:conversation:seq:";
    private static final String REDIS_SEQ_LOCK_KEY_PREFIX = "lock:im:conversation:seq:";
    private static final String INIT_LOCK_KEY = "lock:initialize_sequences";

    @Override
    public long nextSequence(Long conversationId) {
        String redisKey = REDIS_SEQ_KEY_PREFIX + conversationId;
        
        // 首先尝试获取当前值
        String currentValue = redisTemplate.opsForValue().get(redisKey);
        
        if (currentValue == null) {
            // 使用分布式锁确保只有一个线程会执行初始化逻辑
            String lockKey = REDIS_SEQ_LOCK_KEY_PREFIX + conversationId;
            String lockValue = UUID.randomUUID().toString();
            
            try {
                // 尝试获取分布式锁，设置10秒过期时间
                Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                    lockKey, lockValue, Duration.ofSeconds(10));
                
                if (Boolean.TRUE.equals(lockAcquired)) {
                    try {
                        // 双重检查，确保在获取锁后没有其他线程初始化了该值
                        currentValue = redisTemplate.opsForValue().get(redisKey);
                        if (currentValue == null) {
                            // 从数据库获取当前最大sequence
                            var maxSequenceByConversationId = messageRepository.findMaxSequenceByConversationId(conversationId);
                            long initialValue = maxSequenceByConversationId + 1;
                            
                            // 设置初始值并返回
                            redisTemplate.opsForValue().set(redisKey, String.valueOf(initialValue));
                            return initialValue;
                        } else {
                            // 其他线程已经初始化，直接increment并返回
                            return redisTemplate.opsForValue().increment(redisKey);
                        }
                    } finally {
                        // 释放锁 - 检查锁是否仍由当前线程持有，然后删除
                        String currentLockValue = redisTemplate.opsForValue().get(lockKey);
                        if (lockValue.equals(currentLockValue)) {
                            redisTemplate.delete(lockKey);
                        }
                    }
                } else {
                    // 获取锁失败，等待一小段时间后重试
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    // 递归调用，避免死锁
                    return nextSequence(conversationId);
                }
            } catch (Exception e) {
                // 发生异常时也需释放锁
                String currentLockValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentLockValue)) {
                    redisTemplate.delete(lockKey);
                }
                throw e;
            }
        } else {
            // key已存在，直接increment并返回
            return redisTemplate.opsForValue().increment(redisKey);
        }
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
        String lockValue = UUID.randomUUID().toString();
        
        try {
            // 尝试获取分布式锁，设置较长时间的过期时间
            Boolean lockAcquired = redisTemplate.opsForValue().setIfAbsent(
                INIT_LOCK_KEY, lockValue, Duration.ofMinutes(5));
            
            if (Boolean.TRUE.equals(lockAcquired)) {
                // 成功获取锁，执行初始化逻辑
                List<MessageRepository.SequenceRes> sequences = messageRepository.findAllConversationMaxSequences();
                // 批量初始化所有会话的sequence到Redis中
                List<List<MessageRepository.SequenceRes>> batches = batchList(sequences, 1000);

                for (List<MessageRepository.SequenceRes> batch : batches) {
                    // 使用 pipeline 批量设置
                    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                        StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
                        for (MessageRepository.SequenceRes seq : batch) {
                            String key = REDIS_SEQ_KEY_PREFIX + seq.getConversationId();
                            // 只有当key不存在时才设置初始值，避免覆盖已有的序列号
                            stringRedisConn.setNX(key, String.valueOf(seq.getMaxSequenceId()));
                        }
                        return null;
                    });
                }
            } else {
                // 获取锁失败，说明其他实例正在初始化，等待一段时间后继续
                log.warn("Failed to acquire initialization lock, another instance may be initializing sequences");
            }
        } finally {
            // 释放锁 - 检查锁是否仍由当前线程持有，然后删除
            String currentLockValue = redisTemplate.opsForValue().get(INIT_LOCK_KEY);
            if (lockValue.equals(currentLockValue)) {
                redisTemplate.delete(INIT_LOCK_KEY);
            }
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