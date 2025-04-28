package com.github.im.server.config;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/meeting")
public class WebSocketServerBAK {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketServerBAK.class);

    // 存储每个房间的所有用户的Session
    private static final Map<String, Map<String, Session>> rooms = new ConcurrentHashMap<>();

    @OnOpen
//    public void onOpen(Session session, @PathParam("roomName") String roomName, @PathParam("userId") String userId) {
    public void onOpen(Session session) {
//        rooms.computeIfAbsent(roomName, k -> new ConcurrentHashMap<>()).put(userId, session);
//        LOG.info("User [{}] connected to room [{}]", userId, roomName);
//        sendMessage(session, "{\"type\":\"system\",\"message\":\"Welcome " + userId + "\"}");
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("roomName") String roomName) {
        LOG.info("Received message from room [{}]: {}", roomName, message);

        // 转发消息到同一房间的其他用户
        Map<String, Session> roomSessions = rooms.get(roomName);
        if (roomSessions != null) {
            for (Session s : roomSessions.values()) {
                if (!s.equals(session) && s.isOpen()) {
                    sendMessage(s, message);
                }
            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomName") String roomName, @PathParam("userId") String userId) {
        Map<String, Session> roomSessions = rooms.get(roomName);
        if (roomSessions != null) {
            roomSessions.remove(userId);
            if (roomSessions.isEmpty()) {
                rooms.remove(roomName);  // 如果房间没有用户了，则删除该房间
            }
            LOG.info("User [{}] disconnected from room [{}]", userId, roomName);
        }
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        LOG.error("Error on session [{}]: {}", session.getId(), throwable.getMessage());
    }

    private void sendMessage(Session session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                LOG.error("Failed to send message", e);
            }
        }
    }
}
