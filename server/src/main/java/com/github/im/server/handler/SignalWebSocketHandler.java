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

/**
 * WebRTC信令WebSocket处理器
 * 处理WebRTC信令消息，包括offer/answer/candidate等
 */
@Component
public class SignalWebSocketHandler extends TextWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    /**
     * 正在通话中
      */
    private final Map<String, String> inCall = new ConcurrentHashMap<>();

    public SignalWebSocketHandler() {
        this.mapper = new ObjectMapper();
    }

    /**
     * WebSocket连接建立时的回调
     * 将用户会话加入到会话管理中
     *
     * @param session WebSocket会话
     */
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

    /**
     * 处理接收到的文本消息
     * 解析信令消息并根据类型进行相应处理
     *
     * @param session WebSocket会话
     * @param message 接收到的消息
     */
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SignalMessage msg = mapper.readValue(message.getPayload(), SignalMessage.class);
            String from = msg.getFromUser();
            String to = msg.getToUser();
            String type = msg.getType().toLowerCase();
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
                    String peer = inCall.get(from);
                    if (peer != null) inCall.remove(peer);
                    // 挂断发送到相应的客户端
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

    /**
     * WebSocket连接关闭时的回调
     * 清理会话和通话状态
     *
     * @param session WebSocket会话
     * @param status 连接关闭状态
     */
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

    /**
     * 转发消息到目标用户
     *
     * @param toUser 目标用户ID
     * @param msg 要转发的消息
     */
    private void forward(String toUser, TextMessage msg) {
        WebSocketSession target = sessions.get(toUser);
        if (target != null && target.isOpen()) {
            send(target, msg.getPayload());
        }
    }

    /**
     * 发送消息给指定用户
     *
     * @param toUser 目标用户ID
     * @param payload 消息内容
     */
    private void send(String toUser, String payload) {
        if (toUser == null) return;
        send(sessions.get(toUser), payload);
    }

    /**
     * 发送消息到WebSocket会话
     *
     * @param session WebSocket会话
     * @param payload 消息内容
     */
    private void send(WebSocketSession session, String payload) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception ignored) {}
    }

    /**
     * 安静地关闭WebSocket会话
     *
     * @param session 要关闭的会话
     */
    private void closeQuietly(WebSocketSession session) {
        try { session.close(); } catch (Exception ignored) {}
    }

    /**
     * 从URI中提取用户ID
     *
     * @param uri WebSocket连接的URI
     * @return 用户ID，如果无法提取则返回null
     */
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