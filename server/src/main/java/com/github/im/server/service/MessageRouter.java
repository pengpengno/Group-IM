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
 * 消息路由服务
 * 负责在分布式节点间路由消息，实现跨节点用户通信
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageRouter {

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final OnlineService onlineService;
    
    private static final String STREAM_ROUTE_PREFIX = "im:route:";
    private static final String REDIS_ONLINE_PREFIX = "im:online:";

    /**
     * 发送消息的核心方法
     * 根据目标用户所在节点，决定是本地投递还是跨节点路由
     *
     * @param from 发送者ID
     * @param to 接收者ID
     * @param payload 消息内容
     * @throws JsonProcessingException JSON序列化异常
     */
    public void send(Long from, Long to, Object payload) throws JsonProcessingException {
        String nodeId = onlineService.getUserNodeId(to);

        if (nodeId == null) {
            // 用户离线，走离线消息逻辑
            log.info("User {} is offline, saving offline message", to);
            saveOfflineMessage(to, payload);
            return;
        }

        if (NodeId.NODE_ID.equals(nodeId)) {
            // 同节点，直接投递
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
                msg
        );
        
        log.debug("Message routed to node {} for user {}", nodeId, to);
    }

    /**
     * 保存离线消息
     * 当目标用户不在线时，将消息暂存到离线消息队列
     *
     * @param to 目标用户ID
     * @param payload 消息内容
     */
    private void saveOfflineMessage(Long to, Object payload) {
        // 实现离线消息存储逻辑
        log.info("Saving offline message for user {}", to);
    }

    /**
     * 本地投递消息
     * 将消息直接投递给本节点的用户连接
     *
     * @param to 目标用户ID
     * @param payload 消息内容
     */
    private void deliverLocal(Long to, Object payload) {
        try {
            // 构建绑定属性，用于定位用户连接
            BindAttr bindAttr = BindAttr.getBindAttr(to.toString());
            
            // 通过 ReactiveConnectionManager 发送消息到指定用户
            var sink = ReactiveConnectionManager.registerSinkFlow(bindAttr);
            if (payload instanceof BaseMessage.BaseMessagePkg) {
                sink.tryEmitNext((BaseMessage.BaseMessagePkg) payload);
            } else {
                // 如果不是 BaseMessagePkg 类型，需要转换
                log.warn("Unsupported message type for local delivery: {}", payload.getClass());
            }
        } catch (Exception e) {
            log.error("Failed to deliver message locally to user {}", to, e);
        }
    }
}