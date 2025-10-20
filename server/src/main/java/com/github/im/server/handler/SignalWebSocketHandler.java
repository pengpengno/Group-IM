package com.github.im.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalWebSocketHandler extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> inCall = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session.getUri());
        if (userId == null) {
            log.warn("Connection rejected, missing userId.");
            closeQuietly(session);
            return;
        }
        sessions.put(userId, session);
        log.info("User connected: " + userId + ", total=" + sessions.size());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SignalMessage msg = mapper.readValue(message.getPayload(), SignalMessage.class);
            String from = msg.getFromUser();
            String to = msg.getToUser();
            String type = msg.getType();

            log.info("Received message: type={}, from={}, to={}", type, from, to);

            switch (type) {
                case "call/request":
                    forward(to, message);
                    break;

                case "call/accept":
                    inCall.put(from, to);
                    inCall.put(to, from);
                    forward(to, message);
                    break;

                case "call/end":
                    String peer = inCall.remove(from);
                    if (peer != null) inCall.remove(peer);
                    forward(peer, message);
                    break;

                case "offer":
                case "answer":
                case "candidate":
                    forward(to, message);
                    break;

                default:
                    log.warn("Unknown message type: " + type);
            }

        } catch (Exception e) {
            log.error("Failed to handle message", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String userId = extractUserId(session.getUri());
        if (userId != null) {
            sessions.remove(userId);
            String peer = inCall.remove(userId);
            if (peer != null) inCall.remove(peer);

            log.info("User disconnected: " + userId);
            send(peer, "{\"type\":\"call/end\",\"fromUser\":\"" + userId + "\"}");
        }
    }

    private void forward(String toUser, TextMessage msg) {
        WebSocketSession target = sessions.get(toUser);
        if (target != null && target.isOpen()) {
            send(target, msg.getPayload());
        }
    }

    private void send(String toUser, String payload) {
        if (toUser == null) return;
        send(sessions.get(toUser), payload);
    }

    private void send(WebSocketSession session, String payload) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception ignored) {}
    }

    private void closeQuietly(WebSocketSession session) {
        try { session.close(); } catch (Exception ignored) {}
    }

    private String extractUserId(URI uri) {
        if (uri == null || uri.getQuery() == null) return null;
        for (String kv : uri.getQuery().split("&")) {
            String[] arr = kv.split("=");
            if (arr.length == 2 && "userId".equals(arr[0])) {
                return arr[1];
            }
        }
        return null;
    }
}
