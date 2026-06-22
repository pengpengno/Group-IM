package com.github.im.server.consumer;

import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.config.NodeId;
import com.github.im.server.service.ClusterLocalDeliveryService;
import com.github.im.server.service.MessageRouter;
import com.github.im.server.service.RedisMessageRouter;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class RouteConsumer {

    private static final String STREAM_KEY = MessageRouter.STREAM_ROUTE_PREFIX + NodeId.NODE_ID;
    private static final String GROUP_NAME = MessageRouter.CONSUMER_GROUP;
    private static final String CONSUMER_NAME = NodeId.NODE_ID;
    private static final int BATCH_COUNT = 20;
    private static final long BLOCK_SECONDS = 30;
    private static final long ERROR_RETRY_MS = 2000;
    private static final Duration CLAIM_MIN_IDLE = Duration.ofSeconds(60);
    private static final Duration RECOVERY_INTERVAL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ClusterLocalDeliveryService clusterLocalDeliveryService;

    private volatile boolean running = true;
    private volatile long lastRecoveryAt = 0L;

    @PostConstruct
    public void start() {
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
        while (running) {
            try {
                initGroup();
                break;
            } catch (Exception e) {
                log.error("Failed to init stream group, retrying in {}ms: {}", ERROR_RETRY_MS, e.getMessage());
                sleep(ERROR_RETRY_MS);
            }
        }

        recoverPendingMessages(true);

        log.info("Starting route message consumption loop with block={}s", BLOCK_SECONDS);
        while (running) {
            try {
                List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        StreamReadOptions.empty().count(BATCH_COUNT).block(Duration.ofSeconds(BLOCK_SECONDS)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );
                processRecords(records);
                recoverPendingMessages(false);
            } catch (org.springframework.dao.QueryTimeoutException e) {
                recoverPendingMessages(false);
            } catch (Exception e) {
                log.error("Unexpected error in consumption loop: {}", e.getMessage(), e);
                sleep(ERROR_RETRY_MS);
            }
        }
    }

    private void initGroup() {
        try {
            RecordId initId = redisTemplate.opsForStream().add(STREAM_KEY, Map.of("_init", "1"));
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), GROUP_NAME);
            if (initId != null) {
                redisTemplate.opsForStream().delete(STREAM_KEY, initId);
            }
            log.info("Created consumer group '{}' for stream '{}'", GROUP_NAME, STREAM_KEY);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
                log.debug("Consumer group '{}' already exists", GROUP_NAME);
                return;
            }
            throw e;
        }
    }

    private void recoverPendingMessages(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastRecoveryAt < RECOVERY_INTERVAL.toMillis()) {
            return;
        }
        lastRecoveryAt = now;

        try {
            claimStalePendingMessages();
            drainOwnedPendingMessages();
        } catch (Exception e) {
            log.warn("Pending recovery skipped due to error: {}", e.getMessage(), e);
        }
    }

    private void claimStalePendingMessages() {
        PendingMessagesSummary summary = redisTemplate.opsForStream().pending(STREAM_KEY, GROUP_NAME);
        if (summary == null || summary.getTotalPendingMessages() <= 0) {
            return;
        }

        PendingMessages pendingMessages = redisTemplate.opsForStream().pending(
                STREAM_KEY,
                GROUP_NAME,
                Range.unbounded(),
                BATCH_COUNT
        );
        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return;
        }

        List<RecordId> staleIds = pendingMessages.stream()
                .filter(this::isClaimable)
                .map(PendingMessage::getId)
                .collect(Collectors.toList());

        if (staleIds.isEmpty()) {
            return;
        }

        List<MapRecord<String, Object, Object>> claimedRecords = redisTemplate.opsForStream().claim(
                STREAM_KEY,
                GROUP_NAME,
                CONSUMER_NAME,
                RedisStreamCommands.XClaimOptions.minIdle(CLAIM_MIN_IDLE).ids(staleIds)
        );

        if (claimedRecords != null && !claimedRecords.isEmpty()) {
            log.info("Claimed {} stale route messages for consumer {}", claimedRecords.size(), CONSUMER_NAME);
            processRecords(claimedRecords);
        }
    }

    private void drainOwnedPendingMessages() {
        while (running) {
            List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(
                    Consumer.from(GROUP_NAME, CONSUMER_NAME),
                    StreamReadOptions.empty().count(BATCH_COUNT),
                    StreamOffset.create(STREAM_KEY, ReadOffset.from("0"))
            );

            if (records == null || records.isEmpty()) {
                return;
            }

            log.info("Reprocessing {} owned pending route messages", records.size());
            processRecords(records);
        }
    }

    private boolean isClaimable(PendingMessage pendingMessage) {
        if (pendingMessage == null) {
            return false;
        }
        if (CONSUMER_NAME.equals(pendingMessage.getConsumerName())) {
            return false;
        }
        Duration elapsed = pendingMessage.getElapsedTimeSinceLastDelivery();
        return elapsed != null && elapsed.compareTo(CLAIM_MIN_IDLE) >= 0;
    }

    private void processRecords(List<MapRecord<String, Object, Object>> records) {
        if (records == null || records.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> record : records) {
            onMessage(record);
        }
    }

    private void onMessage(MapRecord<String, Object, Object> record) {
        RecordId id = record.getId();
        try {
            Map<Object, Object> bodyObj = record.getValue();
            String toStr = getStringValue(bodyObj, RedisMessageRouter.FIELD_TO);
            String encodedBody = getStringValue(bodyObj, RedisMessageRouter.FIELD_BODY);
            String payloadKind = getStringValue(bodyObj, RedisMessageRouter.FIELD_PAYLOAD_KIND);

            if (!StringUtils.hasText(toStr) || !StringUtils.hasText(encodedBody) || !StringUtils.hasText(payloadKind)) {
                log.warn("Invalid route record format: {}", id);
                acknowledgeQuietly(id);
                return;
            }

            Long toUserId = Long.valueOf(toStr);
            dispatchMessage(toUserId, payloadKind, encodedBody);
            acknowledgeQuietly(id);
        } catch (Exception e) {
            log.error("Failed to handle route message {}: {}", id, e.getMessage(), e);
        }
    }

    private void dispatchMessage(Long userId, String payloadKind, String encodedBody) throws Exception {
        byte[] payloadBytes = Base64.getDecoder().decode(encodedBody);

        if (RedisMessageRouter.PAYLOAD_KIND_IM_BINARY.equals(payloadKind)) {
            BaseMessage.BaseMessagePkg message = BaseMessage.BaseMessagePkg.parseFrom(payloadBytes);
            clusterLocalDeliveryService.deliverBaseMessage(userId, message);
            return;
        }

        if (RedisMessageRouter.PAYLOAD_KIND_SIGNAL_TEXT.equals(payloadKind)) {
            String payload = new String(payloadBytes, StandardCharsets.UTF_8);
            clusterLocalDeliveryService.deliverSignalText(userId, payload);
            return;
        }

        throw new IllegalArgumentException("Unsupported payload kind: " + payloadKind);
    }

    private void acknowledgeQuietly(RecordId id) {
        try {
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, id);
        } catch (Exception e) {
            log.warn("Failed to ACK route message {}: {}", id, e.getMessage());
        }
    }

    private void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String getStringValue(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
