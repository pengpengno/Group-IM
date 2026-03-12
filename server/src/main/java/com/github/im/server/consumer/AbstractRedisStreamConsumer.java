package com.github.im.server.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.time.Duration;
import java.util.Collections;

/**
 * Redis Stream 消费者抽象基类
 * 提供通用的流和消费者组初始化逻辑，避免 NOGROUP 等错误
 */
@Slf4j
public abstract class AbstractRedisStreamConsumer {

    protected final StringRedisTemplate redis;
    protected Subscription subscription;

    protected AbstractRedisStreamConsumer(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 确保 Redis Stream 和 消费者组 存在
     * 使用 MKSTREAM 逻辑确保键存在
     */
    protected void ensureStreamAndConsumerGroup(String streamKey, String consumerGroup) {
        try {
            // 检查 Stream 是否存在，不存在则通过创建一个临时的组（带 MKSTREAM）来创建
            // 或者直接 add 一个初始消息
            if (Boolean.FALSE.equals(redis.hasKey(streamKey))) {
                log.info("Stream key '{}' does not exist, creating with initial message", streamKey);
                redis.opsForStream().add(streamKey, Collections.singletonMap("_init", "true"));
            }

            // 尝试创建消费者组
            try {
                // ReadOffset.from("0") 表示从头开始消费，或者使用 ReadOffset.latest()
                redis.opsForStream().createGroup(streamKey, ReadOffset.from("0"), consumerGroup);
                log.info("Created consumer group '{}' for stream '{}'", consumerGroup, streamKey);
            } catch (Exception e) {
                // 如果消费者组已经存在，会抛出异常，通常包含 BUSYGROUP
                if (e.getMessage() != null
                        && (e.getMessage().contains("BUSYGROUP") || e.getMessage().contains("already exists"))) {
                    log.debug("Consumer group '{}' for stream '{}' already exists", consumerGroup, streamKey);
                } else {
                    log.error("Failed to create consumer group '{}' for stream '{}': {}", consumerGroup, streamKey,
                            e.getMessage());
                    throw e; // 重新抛出，让初始化失败，而不是带病运行
                }
            }
        } catch (Exception e) {
            log.error("Error during stream/group initialization for key: {}", streamKey, e);
            throw new RuntimeException("Redis Stream initialization failed", e);
        }
    }

    protected abstract void consume();

    protected void stop() {
        if (subscription != null) {
            subscription.cancel();
        }
    }
}
