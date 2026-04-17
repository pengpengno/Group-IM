package com.github.im.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.server.model.User;
import com.github.im.server.service.MessageService;
import com.github.im.server.service.OnlineService;
import com.github.im.server.util.SchemaSwitcher;
import com.github.im.server.utils.UserTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.Disposable;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一 WebSocket 处理器
 * 处理 WebRTC 信令消息 (JSON) 和 IM 协议消息 (Protobuf Binary)
 */
@Component
public class SignalWebSocketHandler extends AbstractWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper;
    private final MessageService messageService;
    private final UserTokenManager userTokenManager;
    private final OnlineService onlineService;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> inCall = new ConcurrentHashMap<>();
    private final Map<String, Disposable> pushSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> meetingRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userMeetings = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userSignalProfiles = new ConcurrentHashMap<>();

    private static SignalWebSocketHandler instance;

    public static SignalWebSocketHandler getInstance() {
        return instance;
    }

    public SignalWebSocketHandler(MessageService messageService, UserTokenManager userTokenManager, OnlineService onlineService) {
        this.mapper = new ObjectMapper();
        this.messageService = messageService;
        this.userTokenManager = userTokenManager;
        this.onlineService = onlineService;
        instance = this;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = extractUserId(session.getUri());
        if (userId != null) {
            sessions.put(userId, session);
            log.info("User session connected: {}, total={}", userId, sessions.size());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SignalMessage msg = mapper.readValue(message.getPayload(), SignalMessage.class);
            String from = msg.getFromUser();
            String to = msg.getToUser();
            String type = msg.getType().toLowerCase();
            cacheSignalProfile(msg);
            log.info("Received Text Signal: type={}, from={}, to={}", type, from, to);

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
                    forward(peer, message);
                    break;
                case "offer":
                case "answer":
                case "candidate":
                    forward(to, message);
                    break;
                case "meeting/request":
                case "meeting/reject":
                    forward(to, message);
                    break;
                case "meeting/join":
                    handleMeetingJoin(msg);
                    break;
                case "meeting/leave":
                    handleMeetingLeave(msg.getRoomId(), from);
                    break;
                default:
                    log.warn("Unknown text message type: " + type);
            }
        } catch (Exception e) {
            log.error("Failed to handle text message", e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
            // Protobuf message
            BaseMessage.BaseMessagePkg pkg = BaseMessage.BaseMessagePkg.parseFrom(message.getPayload().array());
            log.debug("Received Binary IM Message: payloadCase={}", pkg.getPayloadCase());

            switch (pkg.getPayloadCase()) {
                case USERINFO:
                    handleUserInfo(session, pkg);
                    break;
                case MESSAGE:
                    handleChatMessage(session, pkg);
                    break;
                case HEARTBEAT:
                    handleHeartbeat(session, pkg);
                    break;
                case ACK:
                    log.debug("Received ACK from WebSocket: {}", pkg.getAck());
                    break;
                default:
                    log.warn("Unsupported binary payload case: {}", pkg.getPayloadCase());
            }
        } catch (Exception e) {
            log.error("Failed to handle binary message", e);
        }
    }

    private void handleUserInfo(WebSocketSession session, BaseMessage.BaseMessagePkg pkg) {
        var userInfo = pkg.getUserInfo();
        String accessToken = userInfo.getAccessToken();
        try {
            User user = userTokenManager.jwt2User(accessToken);
            Long userId = user.getUserId();
            String username = user.getAccount();
            
            // 绑定用户到会话
            session.getAttributes().put("USER", user);
            session.getAttributes().put("USER_INFO", userInfo);
            sessions.put(userId.toString(), session);

            onlineService.online(userId);
            log.info("IM User Online (WS): {} (ID: {})", username, userId);

            // 注册推送流
            var bindAttr = BindAttr.getBindAttr(userInfo);
            var sinkFlow = ReactiveConnectionManager.registerSinkFlow(bindAttr).asFlux();
            
            Disposable subscription = sinkFlow.subscribe(pushPkg -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new BinaryMessage(pushPkg.toByteArray()));
                    }
                } catch (IOException e) {
                    log.error("Failed to push message to user {} via WebSocket", username, e);
                }
            });
            
            pushSubscriptions.put(session.getId(), subscription);

        } catch (Exception e) {
            log.error("Authentication failed for WebSocket IM registration", e);
            closeQuietly(session);
        }
    }

    private void handleChatMessage(WebSocketSession session, BaseMessage.BaseMessagePkg pkg) {
        User user = (User) session.getAttributes().get("USER");
        if (user == null) {
            log.warn("Received CHAT message from unauthenticated WebSocket session: {}", session.getId());
            return;
        }

        var chatMessage = pkg.getMessage();
        log.info("Received IM message from {} via WS, content: {}", user.getAccount(), chatMessage.getContent());

        String schemaName = user.getCurrentSchema();
        SchemaSwitcher.executeInSchema(schemaName, () -> {
            return messageService.handleMessage(chatMessage);
        });
    }

    private void handleHeartbeat(WebSocketSession session, BaseMessage.BaseMessagePkg pkg) {
        var hb = pkg.getHeartbeat();
        if (hb.getPing()) {
            BaseMessage.BaseMessagePkg pong = BaseMessage.BaseMessagePkg.newBuilder()
                    .setHeartbeat(BaseMessage.Heartbeat.newBuilder().setPing(false).build())
                    .build();
            try {
                session.sendMessage(new BinaryMessage(pong.toByteArray()));
            } catch (IOException e) {
                log.error("Failed to send PONG via WebSocket", e);
            }
        }
        
        User user = (User) session.getAttributes().get("USER");
        if (user != null) {
            onlineService.heartbeat(user.getUserId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        User user = (User) session.getAttributes().get("USER");
        
        Disposable sub = pushSubscriptions.remove(session.getId());
        if (sub != null) sub.dispose();

        if (user != null) {
            Long uid = user.getUserId();
            onlineService.offline(uid);
            log.info("IM User Offline (WS): {}", uid);
            String userIdString = uid.toString();
            sessions.remove(userIdString);
            removeUserFromMeetings(userIdString);
            String peer = inCall.remove(userIdString);
            if (peer != null) inCall.remove(peer);
            send(peer, "{\"type\":\"call/end\",\"fromUser\":\"" + userIdString + "\"}");
        } else {
            String userIdString = extractUserId(session.getUri());
            if (userIdString != null) {
                sessions.remove(userIdString);
                removeUserFromMeetings(userIdString);
                String peer = inCall.remove(userIdString);
                if (peer != null) inCall.remove(peer);
                send(peer, "{\"type\":\"call/end\",\"fromUser\":\"" + userIdString + "\"}");
            }
        }
    }

    private void cacheSignalProfile(SignalMessage msg) {
        if (msg.getFromUser() == null) {
            return;
        }

        Map<String, Object> profile = userSignalProfiles.computeIfAbsent(msg.getFromUser(), ignored -> new ConcurrentHashMap<>());
        if (msg.getFromUserName() != null && !msg.getFromUserName().isBlank()) {
            profile.put("userName", msg.getFromUserName());
        }
        if (msg.getFromAvatar() != null && !msg.getFromAvatar().isBlank()) {
            profile.put("avatar", msg.getFromAvatar());
        }
    }

    private void handleMeetingJoin(SignalMessage msg) {
        String roomId = msg.getRoomId();
        String from = msg.getFromUser();
        if (roomId == null || roomId.isBlank() || from == null || from.isBlank()) {
            log.warn("Invalid meeting/join message: roomId={}, from={}", roomId, from);
            return;
        }

        cacheSignalProfile(msg);

        Set<String> roomMembers = meetingRooms.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet());
        List<String> existingMembers = new ArrayList<>(roomMembers);
        roomMembers.add(from);
        userMeetings.computeIfAbsent(from, ignored -> ConcurrentHashMap.newKeySet()).add(roomId);

        sendMeetingParticipants(from, roomId, existingMembers);
        notifyParticipantsJoined(roomId, from, existingMembers);
    }

    private void handleMeetingLeave(String roomId, String userId) {
        if (roomId == null || roomId.isBlank() || userId == null || userId.isBlank()) {
            return;
        }

        Set<String> roomMembers = meetingRooms.get(roomId);
        if (roomMembers == null) {
            return;
        }

        roomMembers.remove(userId);

        Set<String> ownedRooms = userMeetings.get(userId);
        if (ownedRooms != null) {
            ownedRooms.remove(roomId);
            if (ownedRooms.isEmpty()) {
                userMeetings.remove(userId);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "meeting/participant-left");
        payload.put("roomId", roomId);
        payload.put("fromUser", userId);

        String json = toJson(payload);
        for (String memberId : roomMembers) {
            send(memberId, json);
        }

        if (roomMembers.isEmpty()) {
            meetingRooms.remove(roomId);
        }
    }

    private void removeUserFromMeetings(String userId) {
        Set<String> rooms = userMeetings.remove(userId);
        if (rooms == null) {
            return;
        }

        for (String roomId : new HashSet<>(rooms)) {
            handleMeetingLeave(roomId, userId);
        }
    }

    private void sendMeetingParticipants(String userId, String roomId, List<String> participants) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "meeting/participants");
        payload.put("roomId", roomId);
        payload.put("fromUser", userId);

        List<Map<String, Object>> participantInfos = new ArrayList<>();
        for (String participantId : participants) {
            participantInfos.add(buildParticipantInfo(participantId));
        }
        payload.put("participants", participantInfos);

        send(userId, toJson(payload));
    }

    private void notifyParticipantsJoined(String roomId, String joinedUserId, List<String> existingMembers) {
        Map<String, Object> joinedPayload = new HashMap<>();
        joinedPayload.put("type", "meeting/participant-joined");
        joinedPayload.put("roomId", roomId);
        joinedPayload.put("fromUser", joinedUserId);
        joinedPayload.putAll(buildParticipantInfo(joinedUserId));

        String json = toJson(joinedPayload);
        for (String memberId : existingMembers) {
            send(memberId, json);
        }
    }

    /**
     * 向指定会议的所有当前参与者广播信令消息
     */
    public void broadcastToMeeting(String roomId, String type, Map<String, Object> extraData) {
        Set<String> members = meetingRooms.get(roomId);
        if (members == null || members.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("roomId", roomId);
        if (extraData != null) {
            payload.putAll(extraData);
        }

        String json = toJson(payload);
        for (String memberId : members) {
            send(memberId, json);
        }
    }

    /**
     * 向指定用户发送信令消息
     */
    public void sendToUser(String userId, String type, Map<String, Object> extraData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        if (extraData != null) {
            payload.putAll(extraData);
        }

        send(userId, toJson(payload));
    }

    private Map<String, Object> buildParticipantInfo(String userId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);

        Map<String, Object> profile = userSignalProfiles.get(userId);
        if (profile != null) {
            if (profile.get("userName") != null) {
                payload.put("userName", profile.get("userName"));
            }
            if (profile.get("avatar") != null) {
                payload.put("avatar", profile.get("avatar"));
            }
        }
        return payload;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return mapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize signaling payload", e);
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
