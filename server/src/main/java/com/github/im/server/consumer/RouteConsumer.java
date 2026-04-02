package com.github.im.server.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.config.NodeId;
import com.github.im.server.service.MessageRouter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 跨节点消息路由消费者
 * 生产级实现：手写长轮询循环 (XREADGROUP) + 自动 Pending 恢复
 * 替代了传统的 StreamMessageListenerContainer，具备更高的可控性
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RouteConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private volatile boolean running = true;

    private static final String STREAM_KEY = MessageRouter.STREAM_ROUTE_PREFIX + NodeId.NODE_ID;
    private static final String GROUP_NAME = MessageRouter.CONSUMER_GROUP;
    private static final String CONSUMER_NAME = NodeId.NODE_ID;

    // 核心配置
    private static final int BATCH_COUNT = 10;
    private static final long BLOCK_SECONDS = 10;
    private static final long ERROR_RETRY_MS = 2000;

    @PostConstruct
    public void start() {
        // 使用守护线程运行，不阻塞主应用启动
        Thread consumerThread = new Thread(this::runMainLoop, "route-consumer-thread");
        consumerThread.setDaemon(true);
        consumerThread.start();
        log.info("Redis Stream RouteConsumer thread started for: {}", STREAM_KEY);
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping RouteConsumer for node: {}", NodeId.NODE_ID);
        this.running = false;
    }

    private void runMainLoop() {
        // 1. 初始化 Stream 和 Consumer Group
        while (running) {
            try {
                initGroup();
                break;
            } catch (Exception e) {
                log.error("Failed to init stream group, retrying in {}ms: {}", ERROR_RETRY_MS, e.getMessage());
                sleep(ERROR_RETRY_MS);
            }
        }

        // 2. 先处理历史 Pending 消息
        handlePendingMessages();

        // 3. 进入主监听循环 (读取新消息 '>')
        log.info("Starting increment message consumption loop (XREADGROUP '>')");
        while (running) {
            try {
                // 读取新消息，超时阻塞 10s
                var records = redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_COUNT).block(Duration.ofSeconds(BLOCK_SECONDS)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );

                processRecords(records);

            } catch (org.springframework.dao.QueryTimeoutException e) {
                // 超时正常，不打印 error
                log.debug("No new route messages (poll timeout)");
            } catch (Exception e) {
                log.error("Unexpected error in consumption loop: {}", e.getMessage());
                sleep(ERROR_RETRY_MS); // 发生异常时休眠重试，防止 CPU 飙高
            }
        }
    }

    /**
     * 初始化 Stream 和 消费者组
     */
    private void initGroup() {
        try {
            // 使用 MKSTREAM 自动创建 Stream
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            log.info("Created consumer group '{}' for stream '{}'", GROUP_NAME, STREAM_KEY);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group '{}' already exists", GROUP_NAME);
            } else {
                throw e;
            }
        }
    }

    /**
     * 处理该消费名下所有挂（Pending）的消息
     */
    private void handlePendingMessages() {
        log.info("Recovering pending messages (XREADGROUP '0')...");
        while (running) {
            try {
                // 读取已分配给本人但尚未 ACK 的消息
                var records = redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_COUNT),
                        StreamOffset.create(STREAM_KEY, ReadOffset.from("0")) // '0'
                );

                if (records == null || records.isEmpty()) {
                    log.info("Pending recovery finished for node: {}", CONSUMER_NAME);
                    break;
                }

                log.info("Reprocessing {} pending messages...", records.size());
                processRecords(records);

            } catch (Exception e) {
                log.error("Pending recovery error: {}", e.getMessage());
                sleep(ERROR_RETRY_MS);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRecords(List<?> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (Object recordObj : records) {
            if (recordObj instanceof MapRecord) {
                MapRecord<String, Object, Object> record = (MapRecord<String, Object, Object>) recordObj;
                onMessage(record);
            }
        }
    }

    private void onMessage(MapRecord<String, Object, Object> record) {
        RecordId id = record.getId();
        try {
            Map<Object, Object> bodyObj = record.getValue();
            
            // 安全的类型转换
            String toStr = getStringValue(bodyObj, "to");
            String bodyJson = getStringValue(bodyObj, "body");

            if (!StringUtils.hasText(toStr) || !StringUtils.hasText(bodyJson)) {
                log.warn("Invalid route record format: {}", id);
                return;
            }

            Long toUserId = Long.valueOf(toStr);
            BaseMessage.BaseMessagePkg baseMessage = objectMapper.readValue(bodyJson, BaseMessage.BaseMessagePkg.class);

            // 本地消息路由
            deliverLocal(toUserId, baseMessage);

            // 处理成功：执行 ACK
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, id);

        } catch (Exception e) {
            log.error("Failed to handle message {}: {}", id, e.getMessage());
            // 处理失败时不 ACK，由 handlePendingMessages 下次启动或手动干预处理
        }
    }

    private void deliverLocal(Long userId, BaseMessage.BaseMessagePkg messagePkg) {
        try {
            var bindAttr = BindAttr.getBindAttrForPush(userId.toString());
            ReactiveConnectionManager.addBaseMessage(bindAttr, messagePkg);
        } catch (Exception e) {
            log.warn("Deliver error for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Deliver failed", e); // 抛出异常触发不执行 ACK
        }
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 从 Map 中安全获取 String 值
     */
    private String getStringValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}