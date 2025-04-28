package com.github.im.server.config;

import jakarta.websocket.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebRtcHandler extends TextWebSocketHandler {

    // 存储每个房间的所有用户的Session
    private static final Map<String, Map<String, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String roomName = getRoomNameFromSession(session); // Get room name from session attributes or headers
        String userId = getUserIdFromSession(session); // Get user ID from session

        rooms.computeIfAbsent(roomName, k -> new ConcurrentHashMap<>()).put(userId, session);
        log.info("User [{}] connected to room [{}]", userId, roomName);
        sendMessage(session, "{\"type\":\"system\",\"message\":\"Welcome " + userId + "\"}");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String roomName = getRoomNameFromSession(session);  // Extract room from session
        log.info("Received message from room [{}]: {}", roomName, message.getPayload());

        // Forward the message to other users in the same room
        Map<String, WebSocketSession> roomSessions = rooms.get(roomName);
        if (roomSessions != null) {
            for (WebSocketSession s : roomSessions.values()) {
                if (!s.equals(session) && s.isOpen()) {
                    sendMessage(s, message.getPayload());
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String roomName = getRoomNameFromSession(session);  // Extract room from session
        String userId = getUserIdFromSession(session);  // Extract user ID from session

        Map<String, WebSocketSession> roomSessions = rooms.get(roomName);
        if (roomSessions != null) {
            roomSessions.remove(userId);
            if (roomSessions.isEmpty()) {
                rooms.remove(roomName);  // Remove the room if it has no users
            }
            log.info("User [{}] disconnected from room [{}]", userId, roomName);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
        log.error("Error on session [{}]: {}", session.getId(), exception.getMessage());
    }

    private void sendMessage(WebSocketSession session, String message) {
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("Failed to send message", e);
            }
        }
    }

    private String getRoomNameFromSession(WebSocketSession session) {
        // You can retrieve room information from session attributes or headers
        return session.getAttributes().get("roomName").toString(); // Example
    }

    private String getUserIdFromSession(WebSocketSession session) {
        // You can retrieve user ID information from session attributes or headers
        return session.getAttributes().get("userId").toString(); // Example
    }
}
