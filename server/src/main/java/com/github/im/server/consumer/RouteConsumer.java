package com.github.im.server.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.config.NodeId;
import com.github.im.server.service.MessageRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 跨节点消息路由消费者
 */
@Component
@Slf4j
public class RouteConsumer extends AbstractRedisStreamConsumer {

    private final ObjectMapper objectMapper;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private static final String CONSUMER_NAME = NodeId.NODE_ID;

    public RouteConsumer(StringRedisTemplate redis, ObjectMapper objectMapper) {
        super(redis);
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void startConsumer() {
        executor.execute(this::consume);
    }

    @PreDestroy
    public void stopConsumer() {
        super.stop();
    }

    @Override
    protected void consume() {
        String streamKey = MessageRouter.STREAM_ROUTE_PREFIX + NodeId.NODE_ID;

        try {
            // 确保 Stream 和 Consumer Group 存在
            ensureStreamAndConsumerGroup(streamKey, MessageRouter.CONSUMER_GROUP);

            // 配置和创建容器
            StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, MapRecord<String, String, String>> containerOptions = StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                    .builder()
                    .pollTimeout(Duration.ofSeconds(2))
                    .build();

            if (redis.getConnectionFactory() == null) {
                log.error("Redis connection factory is null, cannot start stream consumer");
                return;
            }

            StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer = StreamMessageListenerContainer
                    .create(redis.getConnectionFactory(), containerOptions);

            // 订阅流
            subscription = listenerContainer.receive(
                    Consumer.from(MessageRouter.CONSUMER_GROUP, CONSUMER_NAME),
                    StreamOffset.create(streamKey,
                            org.springframework.data.redis.connection.stream.ReadOffset.lastConsumed()),
                    message -> {
                        try {
                            handle(message);
                            redis.opsForStream().acknowledge(streamKey, MessageRouter.CONSUMER_GROUP, message.getId());
                        } catch (Exception e) {
                            log.error("Error handling stream message", e);
                        }
                    });

            listenerContainer.start();
            log.info("Redis Stream consumer started for key: {}", streamKey);
        } catch (Exception e) {
            log.error("Failed to start Redis Stream consumer, will retry later. Error: {}", e.getMessage());
            // 可以在这里实现简单的重试逻辑，或者让 Spring 容器管理的 Bean 在异常时有更好的表现
        }
    }

    private void handle(MapRecord<String, String, String> record) {
        try {
            String toStr = record.getValue().get("to");
            String body = record.getValue().get("body");

            if (!StringUtils.hasText(toStr) || !StringUtils.hasText(body)) {
                return;
            }

            Long to = Long.valueOf(toStr);
            BaseMessage.BaseMessagePkg baseMessage = objectMapper.readValue(body, BaseMessage.BaseMessagePkg.class);

            deliverLocal(to, baseMessage);
        } catch (Exception e) {
            log.error("Error handling route message", e);
        }
    }

    private void deliverLocal(Long to, BaseMessage.BaseMessagePkg message) {
        try {
            BindAttr bindAttr = BindAttr.getBindAttr(to.toString());
            var sink = ReactiveConnectionManager.registerSinkFlow(bindAttr);
            sink.tryEmitNext(message);
        } catch (Exception e) {
            log.error("Failed to deliver message locally to user {}", to, e);
        }
    }
}