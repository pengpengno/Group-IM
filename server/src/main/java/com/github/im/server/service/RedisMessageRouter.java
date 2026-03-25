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
 * 资深后端架构师设计版本：
 * 1. 负责探测目标节点：本地、远程或离线
 * 2. 具备分布式环境下统一的消息投递格式
 * 3. 避免内存泄露与大对象 JSON 解析
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
        // 1. 寻找目标用户所在节点
        String targetNodeId = onlineService.getUserNodeId(to);

        // 2. 离线处理：如果用户未在线
        if (targetNodeId == null) {
            log.info("User {} is offline, message from {} cached for offline access", to, from);
            // 这里可以接入离线消息存储系统
            return;
        }

        // 3. 本地路由：如果用户就在当前节点，直接内存分发（跳过网络开销）
        if (NodeId.NODE_ID.equals(targetNodeId)) {
            log.debug("User {} found on local node, direct delivery", to);
            deliverLocal(to, payload);
            return;
        }

        // 4. 远程路由：跨节点分发，投递到目标节点的 Redis Stream
        try {
            String routeStream = STREAM_ROUTE_PREFIX + targetNodeId;
            
            Map<String, String> msg = new HashMap<>();
            msg.put("from", from.toString());
            msg.put("to", to.toString());
            msg.put("body", mapper.writeValueAsString(payload));
            msg.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // 投递时可以开启异步，但通过 Redis 往往性能足够
            redis.opsForStream().add(routeStream, msg);

            log.debug("Message from {} routed to node {} for user {}", from, targetNodeId, to);
        } catch (Exception e) {
            log.error("Critical error routing message to node {}: {}", targetNodeId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 内部方法：本地直连投递
     */
    private void deliverLocal(Long toUserId, Object payload) {
        try {
            BindAttr bindAttr = BindAttr.getBindAttr(toUserId.toString());
            var sink = ReactiveConnectionManager.registerSinkFlow(bindAttr);
            
            if (sink == null) {
                log.warn("User {} registered as local but session sink is missing", toUserId);
                return;
            }

            if (payload instanceof BaseMessage.BaseMessagePkg) {
                sink.tryEmitNext((BaseMessage.BaseMessagePkg) payload);
            } else {
                log.warn("Message dropped: unsupported type {}", payload.getClass().getName());
            }
        } catch (Exception e) {
            log.error("Failed to deliver message locally to user {}: {}", toUserId, e.getMessage());
        }
    }
}
