package com.github.im.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.config.NodeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageRouter implements MessageRouter {

    public static final String FIELD_FROM = "from";
    public static final String FIELD_TO = "to";
    public static final String FIELD_BODY = "body";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_PAYLOAD_KIND = "payloadKind";

    public static final String PAYLOAD_KIND_IM_BINARY = "im-binary";
    public static final String PAYLOAD_KIND_SIGNAL_TEXT = "signal-text";

    private final StringRedisTemplate redis;
    private final OnlineService onlineService;
    private final ClusterLocalDeliveryService clusterLocalDeliveryService;

    @Override
    public void send(Long from, Long to, Object payload) throws JsonProcessingException {
        route(from, to, payload);
    }

    public void sendSignal(Long from, Long to, String payload) {
        route(from, to, payload);
    }

    private void route(Long from, Long to, Object payload) {
        String targetNodeId = onlineService.getUserNodeId(to);
        if (targetNodeId == null) {
            log.info("User {} is offline, message from {} cached for offline access", to, from);
            return;
        }

        if (NodeId.NODE_ID.equals(targetNodeId)) {
            deliverLocal(to, payload);
            return;
        }

        try {
            Map<String, String> msg = new HashMap<>();
            msg.put(FIELD_FROM, String.valueOf(from));
            msg.put(FIELD_TO, String.valueOf(to));
            msg.put(FIELD_BODY, serializePayload(payload));
            msg.put(FIELD_PAYLOAD_KIND, resolvePayloadKind(payload));
            msg.put(FIELD_TIMESTAMP, String.valueOf(System.currentTimeMillis()));

            redis.opsForStream().add(STREAM_ROUTE_PREFIX + targetNodeId, msg);
            log.debug("Message from {} routed to node {} for user {}", from, targetNodeId, to);
        } catch (Exception e) {
            log.error("Critical error routing message to node {}: {}", targetNodeId, e.getMessage(), e);
            throw new IllegalStateException("Failed to route cluster message", e);
        }
    }

    private void deliverLocal(Long toUserId, Object payload) {
        if (payload instanceof BaseMessage.BaseMessagePkg messagePkg) {
            clusterLocalDeliveryService.deliverBaseMessage(toUserId, messagePkg);
            return;
        }

        if (payload instanceof String signalPayload) {
            clusterLocalDeliveryService.deliverSignalText(toUserId, signalPayload);
            return;
        }

        throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass().getName());
    }

    private String serializePayload(Object payload) {
        if (payload instanceof BaseMessage.BaseMessagePkg messagePkg) {
            return Base64.getEncoder().encodeToString(messagePkg.toByteArray());
        }

        if (payload instanceof String textPayload) {
            return Base64.getEncoder().encodeToString(textPayload.getBytes(StandardCharsets.UTF_8));
        }

        throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass().getName());
    }

    private String resolvePayloadKind(Object payload) {
        if (payload instanceof BaseMessage.BaseMessagePkg) {
            return PAYLOAD_KIND_IM_BINARY;
        }

        if (payload instanceof String) {
            return PAYLOAD_KIND_SIGNAL_TEXT;
        }

        throw new IllegalArgumentException("Unsupported payload type: " + payload.getClass().getName());
    }
}
