package com.github.im.server.config;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServer.class);
    private static Map<String, Session> userSessions = new HashMap<>();

    // 用户登录
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        userSessions.put(userId, session);
        LOG.info(userId + " connected to the server");
        sendMessage(session, "Welcome to the WebSocket chat!");
    }

    // 用户发送消息
    @OnMessage
    public void onMessage(String message, Session session) {
        LOG.info("Received message: " + message);
        // 将消息转发给所有在线用户
        for (Session s : userSessions.values()) {
            sendMessage(s, message);
        }
    }

    // 用户下线
    @OnClose
    public void onClose(Session session) {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            LOG.info(userId + " disconnected");
        }
    }

    // 发送消息
    private void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }

    // 获取用户ID
    private String getUserIdFromSession(Session session) {
        for (Map.Entry<String, Session> entry : userSessions.entrySet()) {
            if (entry.getValue().equals(session)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
