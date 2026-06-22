package com.github.im.server.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.model.proto.BaseMessage;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.server.model.User;
import com.github.im.server.service.MessageService;
import com.github.im.server.service.OnlineService;
import com.github.im.server.service.RedisMessageRouter;
import com.github.im.server.util.SchemaSwitcher;
import com.github.im.server.utils.UserTokenManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;
import reactor.core.Disposable;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalWebSocketHandler extends AbstractWebSocketHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper mapper;
    private final MessageService messageService;
    private final UserTokenManager userTokenManager;
    private final OnlineService onlineService;
    private final RedisMessageRouter redisMessageRouter;

    @PersistenceContext
    private EntityManager entityManager;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> inCall = new ConcurrentHashMap<>();
    private final Map<String, Disposable> pushSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, BindAttr<String>> sessionBindAttrs = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> meetingRooms = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> userMeetings = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> userSignalProfiles = new ConcurrentHashMap<>();
    private final Map<String, RoomSignalContext> roomContexts = new ConcurrentHashMap<>();

    private static SignalWebSocketHandler instance;

    private static final class RoomSignalContext {
        private Long conversationId;
        private String callKind;
        private String title;
        private String initiatorUserId;
        private LocalDateTime startedAt;
        private boolean summaryPublished;
    }

    public static SignalWebSocketHandler getInstance() {
        return instance;
    }

    public SignalWebSocketHandler(
            MessageService messageService,
            UserTokenManager userTokenManager,
            OnlineService onlineService,
            RedisMessageRouter redisMessageRouter
    ) {
        this.mapper = new ObjectMapper();
        this.messageService = messageService;
        this.userTokenManager = userTokenManager;
        this.onlineService = onlineService;
        this.redisMessageRouter = redisMessageRouter;
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
            String type = normalizeSignalType(msg.getType());
            cacheSignalProfile(msg);
            log.info("Received Text Signal: type={}, from={}, to={}", type, from, to);

            switch (type) {
                case "offer":
                case "answer":
                case "candidate":
                case "meeting/request":
                case "meeting/reject":
                    if ("meeting/request".equals(type)) {
                        cacheRoomContext(msg);
                    }
                    if ("meeting/reject".equals(type)) {
                        publishCallSummaryIfNeeded(
                                msg.getRoomId(),
                                msg.getFromUser(),
                                "DECLINED",
                                msg.getReason(),
                                0
                        );
                    }
                    forward(to, msg, type);
                    break;
                case "meeting/join":
                    if (to != null && !to.isBlank()) {
                        inCall.put(from, to);
                        inCall.put(to, from);
                    }
                    handleMeetingJoin(msg);
                    break;
                case "meeting/leave":
                    String peer = inCall.remove(from);
                    if (peer != null) {
                        inCall.remove(peer);
                    }
                    handleMeetingLeave(msg.getRoomId(), from);
                    break;
                case "ping":
                case "pong":
                    break;
                default:
                    log.warn("Unknown text message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Failed to handle text message", e);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        try {
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
                    handleAckMessage(session, pkg);
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

            session.getAttributes().put("USER", user);
            session.getAttributes().put("USER_INFO", userInfo);
            sessions.put(userId.toString(), session);

            onlineService.online(userId);
            log.info("IM User Online (WS): {} (ID: {})", username, userId);

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
            sessionBindAttrs.put(session.getId(), bindAttr);
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
            var savedMessage = messageService.handleMessage(chatMessage);
            sendDeliveryAck(session, chatMessage, savedMessage.getMsgId());
            return savedMessage;
        });
    }

    private void handleAckMessage(WebSocketSession session, BaseMessage.BaseMessagePkg pkg) {
        var ackMessage = pkg.getAck();
        log.debug("Received ACK from WebSocket: {}", ackMessage);

        User user = (User) session.getAttributes().get("USER");
        if (user == null) {
            log.warn("Received ACK from unauthenticated WebSocket session: {}", session.getId());
            return;
        }

        if (ackMessage.getStatus() == Chat.MessagesStatus.READ) {
            String schemaName = user.getCurrentSchema();
            SchemaSwitcher.executeInSchema(schemaName, () -> {
                messageService.markConversationAsRead(
                        ackMessage.getConversationId(),
                        user.getUserId(),
                        ackMessage.getServerMsgId()
                );
                return null;
            });
        }
    }

    private void sendDeliveryAck(WebSocketSession session, Chat.ChatMessage chatMessage, Long serverMsgId) {
        if (session == null || !session.isOpen()) {
            return;
        }

        BaseMessage.BaseMessagePkg ackPkg = BaseMessage.BaseMessagePkg.newBuilder()
                .setAck(Chat.AckMessage.newBuilder()
                        .setClientMsgId(chatMessage.getClientMsgId())
                        .setServerMsgId(serverMsgId == null ? 0L : serverMsgId)
                        .setConversationId(chatMessage.getConversationId())
                        .setAckTimestamp(System.currentTimeMillis())
                        .setStatus(Chat.MessagesStatus.SENT)
                        .build())
                .build();

        try {
            session.sendMessage(new BinaryMessage(ackPkg.toByteArray()));
        } catch (IOException e) {
            log.error("Failed to send delivery ACK via WebSocket", e);
        }
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
        if (sub != null) {
            sub.dispose();
        }
        BindAttr<String> bindAttr = sessionBindAttrs.remove(session.getId());
        if (bindAttr != null) {
            ReactiveConnectionManager.unSubscribe(bindAttr);
        }

        if (user != null) {
            Long uid = user.getUserId();
            onlineService.offline(uid);
            log.info("IM User Offline (WS): {}", uid);
            String userIdString = uid.toString();
            sessions.remove(userIdString);
            removeUserFromMeetings(userIdString);
            String peer = inCall.remove(userIdString);
            if (peer != null) {
                inCall.remove(peer);
            }
            send(peer, "{\"type\":\"meeting/leave\",\"fromUser\":\"" + userIdString + "\"}");
            return;
        }

        String userIdString = extractUserId(session.getUri());
        if (userIdString != null) {
            sessions.remove(userIdString);
            removeUserFromMeetings(userIdString);
            String peer = inCall.remove(userIdString);
            if (peer != null) {
                inCall.remove(peer);
            }
            send(peer, "{\"type\":\"meeting/leave\",\"fromUser\":\"" + userIdString + "\"}");
        }
    }

    private String normalizeSignalType(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }

        return switch (type.toLowerCase()) {
            case "call/request" -> "meeting/request";
            case "call/accept" -> "meeting/join";
            case "call/end" -> "meeting/leave";
            case "call/failed", "meeting/rejected" -> "meeting/reject";
            case "participant/joined" -> "meeting/participant-joined";
            case "participant/left" -> "meeting/participant-left";
            default -> type.toLowerCase();
        };
    }

    private void cacheSignalProfile(SignalMessage msg) {
        if (msg.getFromUser() == null) {
            return;
        }

        Map<String, Object> profile = userSignalProfiles.computeIfAbsent(
                msg.getFromUser(),
                ignored -> new ConcurrentHashMap<>()
        );
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
        cacheRoomContext(msg);

        Set<String> roomMembers = meetingRooms.computeIfAbsent(roomId, ignored -> ConcurrentHashMap.newKeySet());
        List<String> existingMembers = new ArrayList<>(roomMembers);
        roomMembers.add(from);
        userMeetings.computeIfAbsent(from, ignored -> ConcurrentHashMap.newKeySet()).add(roomId);
        RoomSignalContext context = roomContexts.computeIfAbsent(roomId, ignored -> new RoomSignalContext());
        if (context.startedAt == null && roomMembers.size() >= 2) {
            context.startedAt = LocalDateTime.now();
        }

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

        RoomSignalContext context = roomContexts.get(roomId);
        if (context != null && roomMembers.size() < 2) {
            int durationSeconds = 0;
            if (context.startedAt != null) {
                durationSeconds = (int) java.time.Duration.between(context.startedAt, LocalDateTime.now()).getSeconds();
            }
            publishCallSummaryIfNeeded(
                    roomId,
                    userId,
                    context.startedAt != null ? "ENDED" : "MISSED",
                    null,
                    Math.max(durationSeconds, 0)
            );
        }

        if (roomMembers.isEmpty()) {
            meetingRooms.remove(roomId);
            roomContexts.remove(roomId);
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

    public void broadcastToMeeting(String roomId, String type, Map<String, Object> extraData) {
        Set<String> members = meetingRooms.get(roomId);
        if (members == null || members.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", normalizeSignalType(type));
        payload.put("roomId", roomId);
        if (extraData != null) {
            payload.putAll(extraData);
        }

        String json = toJson(payload);
        for (String memberId : members) {
            send(memberId, json);
        }
    }

    public void sendToUser(String userId, String type, Map<String, Object> extraData) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", normalizeSignalType(type));
        if (extraData != null) {
            payload.putAll(extraData);
        }

        send(userId, toJson(payload));
    }

    public boolean sendClusterSignal(String toUser, String payload) {
        return send(sessions.get(toUser), payload);
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

    private void forward(String toUser, SignalMessage msg, String normalizedType) {
        if (toUser == null || toUser.isBlank()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("type", normalizedType);
        if (msg.getFromUser() != null) {
            payload.put("fromUser", msg.getFromUser());
        }
        if (msg.getFromUserName() != null) {
            payload.put("fromUserName", msg.getFromUserName());
        }
        if (msg.getFromAvatar() != null) {
            payload.put("fromAvatar", msg.getFromAvatar());
        }
        if (msg.getToUser() != null) {
            payload.put("toUser", msg.getToUser());
        }
        if (msg.getRoomId() != null) {
            payload.put("roomId", msg.getRoomId());
        }
        if (msg.getConversationId() != null) {
            payload.put("conversationId", msg.getConversationId());
        }
        if (msg.getCallKind() != null) {
            payload.put("callKind", msg.getCallKind());
        }
        if (msg.getSdp() != null) {
            payload.put("sdp", msg.getSdp());
        }
        if (msg.getSdpType() != null) {
            payload.put("sdpType", msg.getSdpType());
        }
        if (msg.getReason() != null) {
            payload.put("reason", msg.getReason());
        }
        if (msg.getParticipants() != null) {
            payload.put("participants", msg.getParticipants());
        }
        if (msg.getCandidate() != null) {
            payload.put("candidate", msg.getCandidate());
        }

        send(toUser, toJson(payload));
    }

    private void send(String toUser, String payload) {
        if (toUser == null) {
            return;
        }

        if (send(sessions.get(toUser), payload)) {
            return;
        }

        try {
            redisMessageRouter.sendSignal(extractFromUserId(payload), Long.valueOf(toUser), payload);
        } catch (NumberFormatException e) {
            log.warn("Skipping clustered signal routing because user id is not numeric: {}", toUser);
        } catch (Exception e) {
            log.error("Failed to route signal payload to user {}", toUser, e);
        }
    }

    private boolean send(WebSocketSession session, String payload) {
        try {
            if (session != null && session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close();
        } catch (Exception ignored) {
        }
    }

    private String extractUserId(URI uri) {
        if (uri == null || uri.getQuery() == null) {
            return null;
        }

        for (String kv : uri.getQuery().split("&")) {
            String[] arr = kv.split("=");
            if (arr.length == 2 && "userId".equals(arr[0])) {
                return arr[1];
            }
        }
        return null;
    }

    private Long extractFromUserId(String payload) {
        try {
            var node = mapper.readTree(payload);
            if (node.hasNonNull("fromUser")) {
                return node.get("fromUser").asLong(0L);
            }
        } catch (Exception e) {
            log.debug("Unable to extract fromUser from signal payload", e);
        }
        return 0L;
    }

    private void cacheRoomContext(SignalMessage msg) {
        String roomId = msg.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            return;
        }

        RoomSignalContext context = roomContexts.computeIfAbsent(roomId, ignored -> new RoomSignalContext());
        if (msg.getConversationId() != null) {
            context.conversationId = msg.getConversationId();
        }
        if (msg.getCallKind() != null && !msg.getCallKind().isBlank()) {
            context.callKind = msg.getCallKind();
        }
        if (msg.getFromUser() != null && !msg.getFromUser().isBlank() && context.initiatorUserId == null) {
            context.initiatorUserId = msg.getFromUser();
        }
        if (context.title == null) {
            context.title = switch ((context.callKind == null ? "" : context.callKind).toUpperCase()) {
                case "VOICE_CALL" -> "Voice call";
                case "VIDEO_CALL" -> "Video call";
                default -> "Meeting";
            };
        }
    }

    private void publishCallSummaryIfNeeded(String roomId, String actorUserId, String status, String reason, int durationSeconds) {
        if (roomId == null || roomId.isBlank()) {
            return;
        }

        RoomSignalContext context = roomContexts.get(roomId);
        if (context == null || context.summaryPublished || context.conversationId == null) {
            return;
        }

        context.summaryPublished = true;
        try {
            User actor = null;
            if (actorUserId != null && !actorUserId.isBlank()) {
                actor = entityManager.find(User.class, Long.valueOf(actorUserId));
            }
            if (actor == null && context.initiatorUserId != null) {
                actor = entityManager.find(User.class, Long.valueOf(context.initiatorUserId));
            }
            if (actor == null) {
                return;
            }

            var payload = new com.github.im.dto.message.MeetingMessagePayLoad();
            payload.setRoomId(roomId);
            payload.setTitle(context.title);
            payload.setAction("CALL_SUMMARY");
            payload.setCategory(context.callKind == null ? "MEETING" : context.callKind);
            payload.setStatus(status);
            payload.setHostId(context.initiatorUserId == null ? null : Long.valueOf(context.initiatorUserId));
            payload.setActorId(actor.getUserId());
            payload.setDurationSeconds(durationSeconds);
            payload.setSummary(buildCallSummaryText(context.callKind, status, reason, durationSeconds));

            Chat.ChatMessage chatMessage = Chat.ChatMessage.newBuilder()
                    .setConversationId(context.conversationId)
                    .setContent(mapper.writeValueAsString(payload))
                    .setType(Chat.MessageType.MEETING)
                    .setClientMsgId(java.util.UUID.randomUUID().toString())
                    .setFromUser(com.github.im.common.connect.model.proto.User.UserInfo.newBuilder()
                            .setUserId(actor.getUserId())
                            .setUsername(actor.getUsername())
                            .build())
                    .setClientTimeStamp(System.currentTimeMillis())
                    .build();
            messageService.handleMessage(chatMessage);
        } catch (Exception e) {
            log.warn("Failed to publish call summary for room {}", roomId, e);
        }
    }

    private String buildCallSummaryText(String callKind, String status, String reason, int durationSeconds) {
        String label = switch ((callKind == null ? "" : callKind).toUpperCase()) {
            case "VOICE_CALL" -> "Voice call";
            case "VIDEO_CALL" -> "Video call";
            default -> "Meeting";
        };

        return switch (status) {
            case "DECLINED" -> label + " was declined.";
            case "MISSED" -> label + " was not answered.";
            case "ENDED" -> label + " ended after " + formatDuration(durationSeconds) + ".";
            default -> reason != null && !reason.isBlank() ? reason : label + " ended.";
        };
    }

    private String formatDuration(int durationSeconds) {
        int minutes = durationSeconds / 60;
        int seconds = durationSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
