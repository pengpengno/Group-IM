package com.github.im.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.server.config.NodeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 用户在线状态管理服务
 * 负责维护用户在线状态信息，包括上线、下线、心跳续期等功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnlineService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    
    private static final String REDIS_ONLINE_PREFIX = "im:online:";
    private static final String REDIS_NODE_USERS_PREFIX = "im:node:";
    private static final String REDIS_USERS_SUFFIX = ":users";
    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    /**
     * 用户上线
     * 记录用户当前连接的节点ID，并设置TTL过期时间
     *
     * @param userId 用户ID
     */
    public void online(Long userId) {
        String nodeId = NodeId.NODE_ID;

        redis.opsForValue().set(
                REDIS_ONLINE_PREFIX + userId,
                nodeId,
                ONLINE_TTL
        );

        redis.opsForSet().add(
                REDIS_NODE_USERS_PREFIX + nodeId + REDIS_USERS_SUFFIX,
                userId.toString()
        );
        
        log.info("User {} is now online on node {}", userId, nodeId);
    }

    /**
     * 用户心跳续期
     * 更新用户在线状态的TTL，防止因超时而下线
     *
     * @param userId 用户ID
     */
    public void heartbeat(Long userId) {
        redis.expire(REDIS_ONLINE_PREFIX + userId, ONLINE_TTL);
    }

    /**
     * 用户下线
     * 清除用户的在线状态信息
     *
     * @param userId 用户ID
     */
    public void offline(Long userId) {
        redis.delete(REDIS_ONLINE_PREFIX + userId);
        redis.opsForSet().remove(
                REDIS_NODE_USERS_PREFIX + NodeId.NODE_ID + REDIS_USERS_SUFFIX,
                userId.toString()
        );
        
        log.info("User {} is now offline", userId);
    }
    
    /**
     * 查询用户所在节点
     * 根据用户ID查询其当前连接的节点ID
     *
     * @param userId 用户ID
     * @return 节点ID，如果用户不在线则返回null
     */
    public String getUserNodeId(Long userId) {
        return redis.opsForValue().get(REDIS_ONLINE_PREFIX + userId);
    }
}