package com.github.im.server.service;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * 消息路由服务抽象接口
 */
public interface MessageRouter {

    String STREAM_ROUTE_PREFIX = "im:route:";
    String CONSUMER_GROUP = "im-group";

    /**
     * 发送消息
     * 
     * @param from    发送者ID
     * @param to      接收者ID
     * @param payload 消息内容
     */
    void send(Long from, Long to, Object payload) throws JsonProcessingException;
}