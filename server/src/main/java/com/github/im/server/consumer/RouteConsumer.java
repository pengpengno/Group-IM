package com.github.im.server.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.config.NodeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 跨节点消息路由消费者
 * 负责消费Redis Stream中的跨节点消息，并将其投递给本节点的用户连接
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RouteConsumer {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private Subscription subscription;
    private final Executor executor = Executors.newSingleThreadExecutor();
    
    private static final String STREAM_ROUTE_PREFIX = "im:route:";
    private static final String CONSUMER_GROUP = "im-group";
    private static final String CONSUMER_NAME = NodeId.NODE_ID;

    /**
     * 初始化消费者，启动消息监听
     */
    @PostConstruct
    public void startConsumer() {
        executor.execute(this::consume);
    }

    /**
     * 销毁消费者，停止消息监听并清理资源
     */
    @PreDestroy
    public void stopConsumer() {
        if (subscription != null) {
            subscription.cancel();
        }
    }

    /**
     * 启动Redis Stream消息消费
     * 创建监听容器并订阅指定的Stream
     */
    private void consume() {
        String streamKey = STREAM_ROUTE_PREFIX + NodeId.NODE_ID;

        try {
            // 确保Stream和Consumer Group存在
            ensureStreamAndConsumerGroup(streamKey);
            
            // 创建监听容器配置
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> containerOptions =
                    StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                            .builder()
                            .pollTimeout(Duration.ofSeconds(2))
                            .build();

            // 创建监听容器
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer =
                    null;
            if (redis.getConnectionFactory() != null) {
                listenerContainer = StreamMessageListenerContainer.create(redis.getConnectionFactory(), containerOptions);
            }

            // 订阅流
            subscription = listenerContainer.receive(
                    Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                    StreamOffset.create(streamKey, org.springframework.data.redis.connection.stream.ReadOffset.lastConsumed()),
                    message -> {
                        try {
                            handle(message);
                            // 确认消息已处理
                            redis.opsForStream().acknowledge(streamKey, CONSUMER_GROUP, message.getId());
                        } catch (Exception e) {
                            log.error("Error handling stream message", e);
                        }
                    }
            );

            // 启动容器
            listenerContainer.start();
        } catch (Exception e) {
            log.error("Error starting stream consumer", e);
        }
    }
    
    /**
     * 确保Redis Stream和Consumer Group存在
     * 如果不存在，则创建它们
     */
    private void ensureStreamAndConsumerGroup(String streamKey) {
        try {
            // 首先尝试创建消费者组，如果它不存在的话
            try {
                redis.opsForStream().createGroup(streamKey, CONSUMER_GROUP);
                log.info("Created consumer group '{}' for stream '{}'", CONSUMER_GROUP, streamKey);
            } catch (Exception e) {
                // 如果消费者组已经存在，会抛出异常，这是正常的
                if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                    log.debug("Consumer group '{}' for stream '{}' already exists", CONSUMER_GROUP, streamKey);
                } else {
                    log.warn("Could not create consumer group '{}': {}", CONSUMER_GROUP, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error ensuring stream and consumer group exist", e);
        }
    }

    /**
     * 处理从Redis Stream接收到的消息
     * 解析消息内容并投递给目标用户
     *
     * @param record Redis Stream中的消息记录
     */
    private void handle(MapRecord<String, String, String> record) {
        try {
            String toStr = record.getValue().get("to");
            String fromStr = record.getValue().get("from");
            String body = record.getValue().get("body");

            if (!StringUtils.hasText(toStr) || !StringUtils.hasText(body)) {
                log.warn("Invalid message format: {}", record.getValue());
                return;
            }

            Long to = Long.valueOf(toStr);
            // 解析消息体
            BaseMessage.BaseMessagePkg baseMessage = objectMapper.readValue(body, BaseMessage.BaseMessagePkg.class);

            // 投递到本节点的 TCP Channel
            deliverLocal(to, baseMessage);

        } catch (Exception e) {
            log.error("Error handling route message", e);
        }
    }

    /**
     * 将消息投递给本节点的指定用户
     * 通过ReactiveConnectionManager找到用户连接并发送消息
     *
     * @param to 目标用户ID
     * @param message 消息内容
     */
    private void deliverLocal(Long to, BaseMessage.BaseMessagePkg message) {
        try {
            // 构建绑定属性，用于定位用户连接
            BindAttr bindAttr = BindAttr.getBindAttr(to.toString());

            // 通过 ReactiveConnectionManager 发送消息到指定用户
            var sink = ReactiveConnectionManager.registerSinkFlow(bindAttr);
            sink.tryEmitNext(message);
            
            log.debug("Message delivered locally to user {}", to);
        } catch (Exception e) {
            log.error("Failed to deliver message locally to user {}", to, e);
        }
    }
}