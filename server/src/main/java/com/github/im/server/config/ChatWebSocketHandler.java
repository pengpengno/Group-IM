package com.github.im.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<WebSocketSession, User> sessions = new ConcurrentHashMap<>();
    private static final AtomicLong idGenerator = new AtomicLong(System.currentTimeMillis());
    private static final ObjectMapper mapper = new ObjectMapper();
    private static int appendToMakeUnique = 1;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        long clientId = idGenerator.incrementAndGet();
        User user = new User(clientId, null);
        sessions.put(session, user);

        // Send ID to client
        Map<String, Object> msg = Map.of(
                "type", "id",
                "id", clientId
        );
        session.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
        log("New connection from " + session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log("Received message: " + payload);
        Map<String, Object> msg = mapper.readValue(payload, Map.class);

        User sender = sessions.get(session);
        if (sender == null) return;

        String type = (String) msg.get("type");

        switch (type) {
            case "username" -> {
                String requestedName = (String) msg.get("name");
                String finalName = getUniqueUsername(requestedName);
                if (!finalName.equals(requestedName)) {
                    Map<String, Object> rejectMsg = Map.of(
                            "type", "rejectusername",
                            "id", sender.clientId,
                            "name", finalName
                    );
                    session.sendMessage(new TextMessage(mapper.writeValueAsString(rejectMsg)));
                }
                sender.username = finalName;
                sendUserListToAll();
            }
            case "message" -> {
                msg.put("name", sender.username);
                String text = (String) msg.get("text");
                msg.put("text", sanitize(text));
                broadcastMessage(msg);
            }
            default -> {
                // unknown type (for signaling data, etc.)
                if (msg.containsKey("target")) {
                    String targetName = (String) msg.get("target");
                    sendToOneUser(targetName, msg);
                } else {
                    broadcastMessage(msg);
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        sendUserListToAll();
        log("Connection closed: " + session.getRemoteAddress());
    }

    private String getUniqueUsername(String requestedName) {
        Set<String> names = new HashSet<>();
        for (User u : sessions.values()) {
            if (u.username != null) {
                names.add(u.username);
            }
        }
        String finalName = requestedName;
        while (names.contains(finalName)) {
            finalName = requestedName + appendToMakeUnique++;
        }
        return finalName;
    }

    private void sendUserListToAll() {
        List<String> userList = new ArrayList<>();
        for (User u : sessions.values()) {
            if (u.username != null) {
                userList.add(u.username);
            }
        }
        Map<String, Object> userListMsg = Map.of(
                "type", "userlist",
                "users", userList
        );
        broadcastMessage(userListMsg);
    }

    private void broadcastMessage(Map<String, Object> msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            for (WebSocketSession sess : sessions.keySet()) {
                if (sess.isOpen()) {
                    sess.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendToOneUser(String username, Map<String, Object> msg) {
        try {
            String json = mapper.writeValueAsString(msg);
            for (Map.Entry<WebSocketSession, User> entry : sessions.entrySet()) {
                if (username.equals(entry.getValue().username)) {
                    WebSocketSession sess = entry.getKey();
                    if (sess.isOpen()) {
                        sess.sendMessage(new TextMessage(json));
                    }
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String sanitize(String input) {
        return input.replaceAll("<[^>]*>", "");
    }

    private void log(String s) {
        System.out.println("[" + new Date() + "] " + s);
    }

    private static class User {
        long clientId;
        String username;

        User(long clientId, String username) {
            this.clientId = clientId;
            this.username = username;
        }
    }
}