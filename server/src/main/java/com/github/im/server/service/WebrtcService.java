package com.github.im.server.service;

import com.github.im.server.model.WebrtcMessage;
import com.github.im.server.model.WebrtcSession;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebrtcService {
    
    private final Map<String, WebrtcSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    
    /**
     * 创建一个新的WebRTC会话
     * @param callerId 发起者ID
     * @param calleeId 接收者ID
     * @return 会话ID
     */
    public String createSession(Long callerId, Long calleeId) {
        String sessionId = java.util.UUID.randomUUID().toString();
        WebrtcSession session = new WebrtcSession();
        session.setSessionId(sessionId);
        session.setCallerId(callerId);
        session.setCalleeId(calleeId);
        session.setStatus(WebrtcSession.SessionStatus.INITIATED);
        
        sessions.put(sessionId, session);
        userSessions.put(callerId.toString(), sessionId);
        userSessions.put(calleeId.toString(), sessionId);
        
        log.info("创建新的WebRTC会话: {}，发起者: {}，接收者: {}", sessionId, callerId, calleeId);
        return sessionId;
    }
    
    /**
     * 创建一个新的WebRTC会话（字符串ID版本）
     * @param callerId 发起者ID
     * @param calleeId 接收者ID
     * @return 会话ID
     */
    public String createSession(String callerId, String calleeId) {
        String sessionId = java.util.UUID.randomUUID().toString();
        WebrtcSession session = new WebrtcSession();
        session.setSessionId(sessionId);
        session.setCallerId(Long.valueOf(callerId));
        session.setCalleeId(Long.valueOf(calleeId));
        session.setStatus(WebrtcSession.SessionStatus.INITIATED);
        
        sessions.put(sessionId, session);
        userSessions.put(callerId, sessionId);
        userSessions.put(calleeId, sessionId);
        
        log.info("创建新的WebRTC会话: {}，发起者: {}，接收者: {}", sessionId, callerId, calleeId);
        return sessionId;
    }
    
    /**
     * 处理WebRTC消息
     * @param message WebRTC消息
     */
    public void handleMessage(WebrtcMessage message) {
        log.debug("处理WebRTC消息: 类型={}, 从={}，到={}", message.getType(), message.getFrom(), message.getTo());
        
        switch (message.getType()) {
            case OFFER:
                handleOffer(message);
                break;
            case ANSWER:
                handleAnswer(message);
                break;
            case ICE_CANDIDATE:
                handleIceCandidate(message);
                break;
            case HANGUP:
                handleHangup(message);
                break;
            case REJECT:
                handleReject(message);
                break;
            default:
                log.warn("未知的WebRTC消息类型: {}", message.getType());
        }
    }
    
    /**
     * 处理Offer消息
     * @param message Offer消息
     */
    private void handleOffer(WebrtcMessage message) {
        String sessionId = userSessions.get(message.getFrom());
        if (sessionId != null) {
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.setOffer(message.getPayload());
                session.setStatus(WebrtcSession.SessionStatus.OFFER_SENT);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                log.info("会话{}收到Offer", sessionId);
            }
        }
    }
    
    /**
     * 处理Answer消息
     * @param message Answer消息
     */
    private void handleAnswer(WebrtcMessage message) {
        String sessionId = userSessions.get(message.getFrom());
        if (sessionId != null) {
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.setAnswer(message.getPayload());
                session.setStatus(WebrtcSession.SessionStatus.ANSWER_SENT);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                log.info("会话{}收到Answer", sessionId);
            }
        }
    }
    
    /**
     * 处理ICE候选消息
     * @param message ICE候选消息
     */
    private void handleIceCandidate(WebrtcMessage message) {
        String sessionId = userSessions.get(message.getFrom());
        if (sessionId != null) {
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.getIceCandidates().add(message.getPayload());
                session.setUpdatedAt(java.time.LocalDateTime.now());
                log.debug("会话{}收到ICE候选", sessionId);
            }
        }
    }
    
    /**
     * 处理挂断消息
     * @param message 挂断消息
     */
    private void handleHangup(WebrtcMessage message) {
        String sessionId = userSessions.get(message.getFrom());
        if (sessionId != null) {
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.setStatus(WebrtcSession.SessionStatus.DISCONNECTED);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                cleanupSession(sessionId);
                log.info("会话{}已挂断", sessionId);
            }
        }
    }
    
    /**
     * 处理拒绝消息
     * @param message 拒绝消息
     */
    private void handleReject(WebrtcMessage message) {
        String sessionId = userSessions.get(message.getFrom());
        if (sessionId != null) {
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.setStatus(WebrtcSession.SessionStatus.REJECTED);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                cleanupSession(sessionId);
                log.info("会话{}已被拒绝", sessionId);
            }
        }
    }
    
    /**
     * 清理会话
     * @param sessionId 会话ID
     */
    private void cleanupSession(String sessionId) {
        WebrtcSession session = sessions.get(sessionId);
        if (session != null) {
            userSessions.remove(session.getCallerId().toString());
            userSessions.remove(session.getCalleeId().toString());
            sessions.remove(sessionId);
            log.info("清理会话: {}", sessionId);
        }
    }
    
    /**
     * 获取会话信息
     * @param sessionId 会话ID
     * @return 会话对象
     */
    public WebrtcSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 检查用户是否有活动会话
     * @param userId 用户ID
     * @return 是否有活动会话
     */
    public boolean hasActiveSession(String userId) {
        String sessionId = userSessions.get(userId);
        if (sessionId != null) {
            WebrtcSession session = sessions.get(sessionId);
            return session != null && 
                   session.getStatus() != WebrtcSession.SessionStatus.DISCONNECTED &&
                   session.getStatus() != WebrtcSession.SessionStatus.REJECTED &&
                   session.getStatus() != WebrtcSession.SessionStatus.TIMEOUT;
        }
        return false;
    }
    
    /**
     * 获取所有在线用户ID
     * @return 在线用户ID集合
     */
    public java.util.Set<String> getOnlineUsers() {
        return userSessions.keySet();
    }
}