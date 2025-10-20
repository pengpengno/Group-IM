//package com.github.im.server.controller;
//
//import com.github.im.server.model.WebrtcMessage;
//import com.github.im.server.service.WebrtcService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//
//import java.security.Principal;
//
//@Slf4j
//@Controller
//public class SignalingController {
//
//    @Autowired
//    private WebrtcService webrtcService;
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    /**
//     * 处理WebRTC信令消息
//     * 对应前端发送路径: /app/signaling
//     * @param message WebRTC信令消息
//     * @param principal 当前用户主体
//     */
//    @MessageMapping("/signaling")
//    public void handleSignaling(@Payload WebrtcMessage message, Principal principal) {
//        try {
//            if (message == null) {
//                log.warn("收到空的WebRTC信令消息");
//                return;
//            }
//
//            String fromUser = (principal != null) ? principal.getName() : message.getFrom();
//            if (fromUser == null || fromUser.isEmpty()) {
//                log.warn("无法确定消息发送者");
//                return;
//            }
//
//            log.debug("收到WebRTC信令消息: 类型={}, 从={}, 到={}", message.getType(), fromUser, message.getTo());
//
//            // 设置消息发送者
//            message.setFrom(fromUser);
//
//            // 根据消息类型处理
//            switch (message.getType()) {
//                case OFFER:
//                case ANSWER:
//                case ICE_CANDIDATE:
//                    handleWebRtcMessage(message);
//                    break;
//                default:
//                    log.warn("未知的信令消息类型: {}", message.getType());
//            }
//        } catch (Exception e) {
//            log.error("处理WebRTC信令消息时出错", e);
//        }
//    }
//
//    /**
//     * 处理WebRTC消息（Offer/Answer/ICE候选）
//     * @param message WebRTC消息
//     */
//    private void handleWebRtcMessage(WebrtcMessage message) {
//        try {
//            if (message == null) {
//                log.warn("收到空的WebRTC消息");
//                return;
//            }
//
//            // 检查必要字段
//            if (message.getTo() == null || message.getTo().isEmpty()) {
//                log.warn("WebRTC消息缺少接收者信息");
//                return;
//            }
//
//            if (message.getPayload() == null || message.getPayload().isEmpty()) {
//                log.warn("WebRTC消息缺少内容");
//                return;
//            }
//
//            // 转发给接收者
//            messagingTemplate.convertAndSendToUser(
//                message.getTo(),
//                "/queue/signaling",
//                message
//            );
//            log.debug("转发WebRTC消息给用户: {}", message.getTo());
//        } catch (Exception e) {
//            log.error("处理WebRTC消息时出错", e);
//        }
//    }
//}