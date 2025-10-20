//package com.github.im.server.handler;
//
//import com.github.im.server.model.WebrtcMessage;
//import com.github.im.server.service.WebrtcService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.event.EventListener;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.socket.messaging.SessionConnectedEvent;
//import org.springframework.web.socket.messaging.SessionDisconnectEvent;
//
//import java.security.Principal;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.Map;
//
//@Slf4j
//@Controller
//public class WebSocketHandler {
//
//    // 用于跟踪在线用户
//    private static final Map<String, Boolean> onlineUsers = new ConcurrentHashMap<>();
//
//    @Autowired
//    private WebrtcService webrtcService;
//
//    /**
//     * 处理WebSocket连接建立事件
//     */
//    @EventListener
//    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
//        try {
//            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//            Principal principal = headerAccessor.getUser();
//
//            if (principal != null) {
//                String userId = principal.getName();
//                onlineUsers.put(userId, true);
//                webrtcService.userOnline(userId);
//                log.info("WebSocket连接已建立，用户ID: {}", userId);
//
//                // 打印当前所有在线用户
//                log.info("当前在线用户: {}", onlineUsers.keySet());
//            } else {
//                // 尝试从URL路径中获取userId
//                String userId = (String) headerAccessor.getAttribute("userId");
//                if (userId == null) {
//                    userId = headerAccessor.getFirstNativeHeader("login");
//                }
//
//                if (userId != null && !userId.isEmpty()) {
//                    onlineUsers.put(userId, true);
//                    webrtcService.userOnline(userId);
//                    log.info("WebSocket连接已建立（通过URL路径），用户ID: {}", userId);
//
//                    // 打印当前所有在线用户
//                    log.info("当前在线用户: {}", onlineUsers.keySet());
//                } else {
//                    log.warn("WebSocket连接已建立，但无法确定用户ID");
//                }
//            }
//        } catch (Exception e) {
//            log.error("处理WebSocket连接建立事件时发生错误", e);
//        }
//    }
//
//    /**
//     * 处理WebSocket连接断开事件
//     */
//    @EventListener
//    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
//        try {
//            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
//            Principal principal = headerAccessor.getUser();
//
//            if (principal != null) {
//                String userId = principal.getName();
//                onlineUsers.remove(userId);
//                webrtcService.userOffline(userId);
//                log.info("WebSocket连接已断开，用户ID: {}", userId);
//
//                // 打印当前所有在线用户
//                log.info("当前在线用户: {}", onlineUsers.keySet());
//            } else {
//                // 尝试从URL路径中获取userId
//                String userId = (String) headerAccessor.getAttribute("userId");
//                if (userId == null) {
//                    userId = headerAccessor.getFirstNativeHeader("login");
//                }
//
//                if (userId != null && !userId.isEmpty()) {
//                    onlineUsers.remove(userId);
//                    webrtcService.userOffline(userId);
//                    log.info("WebSocket连接已断开（通过URL路径），用户ID: {}", userId);
//
//                    // 打印当前所有在线用户
//                    log.info("当前在线用户: {}", onlineUsers.keySet());
//                } else {
//                    log.warn("WebSocket连接已断开，但无法确定用户ID");
//                }
//            }
//        } catch (Exception e) {
//            log.error("处理WebSocket连接断开事件时发生错误", e);
//        }
//    }
//
//    /**
//     * 处理WebRTC消息
//     * @param message WebRTC消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/webrtc/message")
//    public void handleWebrtcMessage(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的WebRTC消息");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定WebRTC消息发送者");
//                return;
//            }
//
//            log.debug("收到WebRTC消息: 类型={}, 从={}, 到={}", message.getType(), fromUser, message.getTo());
//
//            // 记录用户信息
//            log.info("当前在线用户信息: {}", onlineUsers.keySet());
//
//            // 设置消息发送者
//            message.setFrom(fromUser);
//
//            // 处理消息
//            webrtcService.handleMessage(message);
//
//            // 转发消息给接收者
//            if (message.getTo() != null && !message.getTo().isEmpty()) {
//                messagingTemplate.convertAndSendToUser(
//                    message.getTo(),
//                    "/queue/webrtc",
//                    message
//                );
//                log.debug("转发WebRTC消息给用户: {}", message.getTo());
//            }
//        } catch (Exception e) {
//            log.error("处理WebRTC消息时出错", e);
//        }
//    }
//
//    /**
//     * 处理呼叫请求
//     * @param message 呼叫消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/webrtc/call")
//    public void handleCall(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的呼叫请求");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定呼叫请求发送者");
//                return;
//            }
//
//            log.info("收到呼叫请求: 从={} 到={}", fromUser, message.getTo());
//
//            // 记录用户信息
//            log.info("当前在线用户信息: {}", onlineUsers.keySet());
//
//            message.setFrom(fromUser);
//            message.setType(WebrtcMessage.MessageType.OFFER);
//
//            // 检查接收者是否在线
//            if (message.getTo() == null || message.getTo().isEmpty()) {
//                log.warn("呼叫请求缺少接收者信息");
//                return;
//            }
//
//            if (!webrtcService.isUserOnline(message.getTo())) {
//                log.warn("呼叫请求接收者不在线: {}", message.getTo());
//                // 可以选择通知发送者接收者不在线
//                return;
//            }
//
//            // 创建WebRTC会话
//            Long callerId = Long.valueOf(fromUser);
//            Long calleeId = Long.valueOf(message.getTo());
//            String sessionId = webrtcService.createSession(callerId, calleeId);
//
//            // 发送呼叫请求给被叫方
//            messagingTemplate.convertAndSendToUser(
//                message.getTo(),
//                "/queue/webrtc/call",
//                message
//            );
//
//            log.info("已发送呼叫请求: 会话ID={}, 从={} 到={}", sessionId, callerId, calleeId);
//        } catch (Exception e) {
//            log.error("处理呼叫请求时出错", e);
//        }
//    }
//
//    /**
//     * 处理呼叫应答
//     * @param message 应答消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/webrtc/answer")
//    public void handleAnswer(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的呼叫应答");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定呼叫应答发送者");
//                return;
//            }
//
//            log.info("收到呼叫应答: 从={} 到={}", fromUser, message.getTo());
//
//            // 记录用户信息
//            log.info("当前在线用户信息: {}", onlineUsers.keySet());
//
//            message.setFrom(fromUser);
//            message.setType(WebrtcMessage.MessageType.ANSWER);
//
//            // 发送应答给呼叫方
//            if (message.getTo() != null && !message.getTo().isEmpty()) {
//                messagingTemplate.convertAndSendToUser(
//                    message.getTo(),
//                    "/queue/webrtc/answer",
//                    message
//                );
//
//                log.info("已发送呼叫应答给: {}", message.getTo());
//            } else {
//                log.warn("呼叫应答缺少接收者信息");
//            }
//        } catch (Exception e) {
//            log.error("处理呼叫应答时出错", e);
//        }
//    }
//
//    /**
//     * 处理ICE候选
//     * @param message ICE候选消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/webrtc/ice")
//    public void handleIceCandidate(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的ICE候选消息");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定ICE候选消息发送者");
//                return;
//            }
//
//            log.debug("收到ICE候选: 从={} 到={}", fromUser, message.getTo());
//
//            // 记录用户信息
//            log.info("当前在线用户信息: {}", onlineUsers.keySet());
//
//            message.setFrom(fromUser);
//            message.setType(WebrtcMessage.MessageType.ICE_CANDIDATE);
//
//            // 发送ICE候选给对方
//            if (message.getTo() != null && !message.getTo().isEmpty()) {
//                messagingTemplate.convertAndSendToUser(
//                    message.getTo(),
//                    "/queue/webrtc/ice",
//                    message
//                );
//
//                log.debug("已发送ICE候选给: {}", message.getTo());
//            } else {
//                log.warn("ICE候选消息缺少接收者信息");
//            }
//        } catch (Exception e) {
//            log.error("处理ICE候选时出错", e);
//        }
//    }
//
//    /**
//     * 处理挂断请求
//     * @param message 挂断消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/webrtc/hangup")
//    public void handleHangup(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的挂断请求");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定挂断请求发送者");
//                return;
//            }
//
//            log.info("收到挂断请求: 从={} 到={}", fromUser, message.getTo());
//
//            // 记录用户信息
//            log.info("当前在线用户信息: {}", onlineUsers.keySet());
//
//            message.setFrom(fromUser);
//            message.setType(WebrtcMessage.MessageType.HANGUP);
//
//            // 通知对方挂断
//            if (message.getTo() != null && !message.getTo().isEmpty()) {
//                messagingTemplate.convertAndSendToUser(
//                    message.getTo(),
//                    "/queue/webrtc/hangup",
//                    message
//                );
//
//                log.info("已发送挂断通知给: {}", message.getTo());
//            } else {
//                log.warn("挂断请求缺少接收者信息");
//            }
//        } catch (Exception e) {
//            log.error("处理挂断请求时出错", e);
//        }
//    }
//
//    /**
//     * 处理拒绝请求
//     * @param message 拒绝消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/webrtc/reject")
//    public void handleReject(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的拒绝请求");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定拒绝请求发送者");
//                return;
//            }
//
//            log.info("收到拒绝请求: 从={} 到={}", fromUser, message.getTo());
//
//            // 记录用户信息
//            log.info("当前在线用户信息: {}", onlineUsers.keySet());
//
//            message.setFrom(fromUser);
//            message.setType(WebrtcMessage.MessageType.REJECT);
//
//            // 通知对方拒绝
//            if (message.getTo() != null && !message.getTo().isEmpty()) {
//                messagingTemplate.convertAndSendToUser(
//                    message.getTo(),
//                    "/queue/webrtc/reject",
//                    message
//                );
//
//                log.info("已发送拒绝通知给: {}", message.getTo());
//            } else {
//                log.warn("拒绝请求缺少接收者信息");
//            }
//        } catch (Exception e) {
//            log.error("处理拒绝请求时出错", e);
//        }
//    }
//}