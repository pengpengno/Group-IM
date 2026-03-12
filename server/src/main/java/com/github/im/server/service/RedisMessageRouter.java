package com.github.im.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.config.NodeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息路由服务 Redis 实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisMessageRouter implements MessageRouter {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final OnlineService onlineService;

    @Override
    public void send(Long from, Long to, Object payload) throws JsonProcessingException {
        String nodeId = onlineService.getUserNodeId(to);

        if (nodeId == null) {
            log.info("User {} is offline, saving offline message", to);
            // 实现离线消息逻辑
            return;
        }

        if (NodeId.NODE_ID.equals(nodeId)) {
            log.debug("Delivering message locally to user {}", to);
            deliverLocal(to, payload);
            return;
        }

        // 跨节点，通过 Redis Stream
        Map<String, String> msg = new HashMap<>();
        msg.put("from", from.toString());
        msg.put("to", to.toString());
        msg.put("body", mapper.writeValueAsString(payload));

        redis.opsForStream().add(
                STREAM_ROUTE_PREFIX + nodeId,
                msg);

        log.debug("Message routed to node {} for user {}", nodeId, to);
    }

    private void deliverLocal(Long to, Object payload) {
        try {
            BindAttr bindAttr = BindAttr.getBindAttr(to.toString());
            var sink = ReactiveConnectionManager.registerSinkFlow(bindAttr);
            if (payload instanceof BaseMessage.BaseMessagePkg) {
                sink.tryEmitNext((BaseMessage.BaseMessagePkg) payload);
            } else {
                log.warn("Unsupported message type for local delivery: {}", payload.getClass());
            }
        } catch (Exception e) {
            log.error("Failed to deliver message locally to user {}", to, e);
        }
    }
}
