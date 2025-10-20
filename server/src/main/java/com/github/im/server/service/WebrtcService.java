package com.github.im.server.service;

import com.github.im.server.model.WebrtcMessage;
import com.github.im.server.model.WebrtcSession;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class WebrtcService {
    
    // 跟踪在线用户
    private final Map<String, Boolean> onlineUsers = new ConcurrentHashMap<>();
    private final Map<String, WebrtcSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Boolean> screenShareSessions = new ConcurrentHashMap<>(); // 屏幕共享会话跟踪
    private final Gson gson = new Gson();
    
    /**
     * 用户上线
     * @param userId 用户ID
     */
    public void userOnline(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("尝试将空用户ID设置为在线状态");
                return;
            }
            
            onlineUsers.put(userId, true);
            log.info("用户上线: {}, 当前在线用户: {}", userId, onlineUsers.keySet());
        } catch (Exception e) {
            log.error("处理用户上线时发生错误，用户ID: {}", userId, e);
        }
    }
    
    /**
     * 用户下线
     * @param userId 用户ID
     */
    public void userOffline(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("尝试将空用户ID设置为离线状态");
                return;
            }
            
            onlineUsers.remove(userId);
            // 清理用户会话
            String sessionId = userSessions.remove(userId);
            if (sessionId != null) {
                WebrtcSession session = sessions.get(sessionId);
                if (session != null) {
                    session.setStatus(WebrtcSession.SessionStatus.DISCONNECTED);
                    sessions.remove(sessionId);
                }
            }
            log.info("用户下线: {}, 当前在线用户: {}", userId, onlineUsers.keySet());
        } catch (Exception e) {
            log.error("处理用户下线时发生错误，用户ID: {}", userId, e);
        }
    }
    
    /**
     * 检查用户是否在线
     * @param userId 用户ID
     * @return 是否在线
     */
    public boolean isUserOnline(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                return false;
            }
            return onlineUsers.containsKey(userId);
        } catch (Exception e) {
            log.error("检查用户在线状态时发生错误，用户ID: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 获取所有在线用户
     * @return 在线用户集合
     */
    public Set<String> getOnlineUsers() {
        try {
            return onlineUsers.keySet();
        } catch (Exception e) {
            log.error("获取在线用户列表时发生错误", e);
            return Set.of();
        }
    }
    
    /**
     * 创建一个新的WebRTC会话
     * @param callerId 发起者ID
     * @param calleeId 接收者ID
     * @return 会话ID
     */
    public String createSession(Long callerId, Long calleeId) {
        try {
            if (callerId == null || calleeId == null) {
                log.warn("创建会话失败，发起者或接收者ID为空");
                return null;
            }
            
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
        } catch (Exception e) {
            log.error("创建WebRTC会话时发生错误，发起者: {}，接收者: {}", callerId, calleeId, e);
            return null;
        }
    }
    
    /**
     * 创建一个新的WebRTC会话（字符串ID版本）
     * @param callerId 发起者ID
     * @param calleeId 接收者ID
     * @return 会话ID
     */
    public String createSession(String callerId, String calleeId) {
        try {
            if (callerId == null || callerId.isEmpty() || calleeId == null || calleeId.isEmpty()) {
                log.warn("创建会话失败，发起者或接收者ID为空");
                return null;
            }
            
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
        } catch (Exception e) {
            log.error("创建WebRTC会话时发生错误，发起者: {}，接收者: {}", callerId, calleeId, e);
            return null;
        }
    }
    
    /**
     * 处理WebRTC消息
     * @param message WebRTC消息
     */
    public void handleMessage(WebrtcMessage message) {
        try {
            if (message == null) {
                log.warn("尝试处理空的WebRTC消息");
                return;
            }
            
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
        } catch (Exception e) {
            log.error("处理WebRTC消息时发生错误，消息: {}", message, e);
        }
    }
    
    /**
     * 处理Offer消息
     * @param message Offer消息
     */
    private void handleOffer(WebrtcMessage message) {
        try {
            if (message == null) {
                log.warn("尝试处理空的Offer消息");
                return;
            }
            
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
        } catch (Exception e) {
            log.error("处理Offer消息时发生错误，消息: {}", message, e);
        }
    }
    
    /**
     * 处理Answer消息
     * @param message Answer消息
     */
    private void handleAnswer(WebrtcMessage message) {
        try {
            if (message == null) {
                log.warn("尝试处理空的Answer消息");
                return;
            }
            
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
        } catch (Exception e) {
            log.error("处理Answer消息时发生错误，消息: {}", message, e);
        }
    }
    
    /**
     * 处理ICE候选消息
     * @param message ICE候选消息
     */
    private void handleIceCandidate(WebrtcMessage message) {
        try {
            if (message == null) {
                log.warn("尝试处理空的ICE候选消息");
                return;
            }
            
            String sessionId = userSessions.get(message.getFrom());
            if (sessionId != null) {
                WebrtcSession session = sessions.get(sessionId);
                if (session != null) {
                    session.getIceCandidates().add(message.getPayload());
                    session.setUpdatedAt(java.time.LocalDateTime.now());
                    log.debug("会话{}收到ICE候选", sessionId);
                }
            }
        } catch (Exception e) {
            log.error("处理ICE候选消息时发生错误，消息: {}", message, e);
        }
    }
    
    /**
     * 处理挂断消息
     * @param message 挂断消息
     */
    private void handleHangup(WebrtcMessage message) {
        try {
            if (message == null) {
                log.warn("尝试处理空的挂断消息");
                return;
            }
            
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
        } catch (Exception e) {
            log.error("处理挂断消息时发生错误，消息: {}", message, e);
        }
    }
    
    /**
     * 处理拒绝消息
     * @param message 拒绝消息
     */
    private void handleReject(WebrtcMessage message) {
        try {
            if (message == null) {
                log.warn("尝试处理空的拒绝消息");
                return;
            }
            
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
        } catch (Exception e) {
            log.error("处理拒绝消息时发生错误，消息: {}", message, e);
        }
    }
    
    /**
     * 清理会话
     * @param sessionId 会话ID
     */
    private void cleanupSession(String sessionId) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试清理空会话ID");
                return;
            }
            
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                userSessions.remove(session.getCallerId().toString());
                userSessions.remove(session.getCalleeId().toString());
                sessions.remove(sessionId);
                log.info("清理会话: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("清理会话时发生错误，会话ID: {}", sessionId, e);
        }
    }
    
    /**
     * 发送offer给被叫方
     * @param sessionId 会话ID
     * @param offer SDP Offer
     */
    public void sendOffer(String sessionId, String offer) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试发送Offer到空会话ID");
                return;
            }
            
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.setOffer(offer);
                session.setStatus(WebrtcSession.SessionStatus.OFFER_SENT);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                log.info("会话{} Offer已发送", sessionId);
            }
        } catch (Exception e) {
            log.error("发送Offer时发生错误，会话ID: {}", sessionId, e);
        }
    }
    
    /**
     * 发送answer给发起方
     * @param sessionId 会话ID
     * @param answer SDP Answer
     */
    public void sendAnswer(String sessionId, String answer) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试发送Answer到空会话ID");
                return;
            }
            
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.setAnswer(answer);
                session.setStatus(WebrtcSession.SessionStatus.ANSWER_SENT);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                log.info("会话{} Answer已发送", sessionId);
            }
        } catch (Exception e) {
            log.error("发送Answer时发生错误，会话ID: {}", sessionId, e);
        }
    }
    
    /**
     * 添加ICE候选
     * @param sessionId 会话ID
     * @param candidate ICE候选
     */
    public void addIceCandidate(String sessionId, String candidate) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试添加ICE候选到空会话ID");
                return;
            }
            
            WebrtcSession session = sessions.get(sessionId);
            if (session != null) {
                session.getIceCandidates().add(candidate);
                session.setUpdatedAt(java.time.LocalDateTime.now());
                log.debug("会话{} 添加ICE候选", sessionId);
            }
        } catch (Exception e) {
            log.error("添加ICE候选时发生错误，会话ID: {}", sessionId, e);
        }
    }
    
    /**
     * 获取会话信息
     * @param sessionId 会话ID
     * @return 会话对象
     */
    public WebrtcSession getSession(String sessionId) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试获取空会话ID的信息");
                return null;
            }
            
            return sessions.get(sessionId);
        } catch (Exception e) {
            log.error("获取会话信息时发生错误，会话ID: {}", sessionId, e);
            return null;
        }
    }
    
    /**
     * 检查用户是否有活动会话
     * @param userId 用户ID
     * @return 是否有活动会话
     */
    public boolean hasActiveSession(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("尝试检查空用户ID的活动会话");
                return false;
            }
            
            String sessionId = userSessions.get(userId);
            if (sessionId != null) {
                WebrtcSession session = sessions.get(sessionId);
                return session != null && 
                       session.getStatus() != WebrtcSession.SessionStatus.DISCONNECTED &&
                       session.getStatus() != WebrtcSession.SessionStatus.REJECTED &&
                       session.getStatus() != WebrtcSession.SessionStatus.TIMEOUT;
            }
            return false;
        } catch (Exception e) {
            log.error("检查用户活动会话时发生错误，用户ID: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 获取用户的会话ID
     * @param userId 用户ID
     * @return 会话ID
     */
    public String getUserSession(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("尝试获取空用户ID的会话");
                return null;
            }
            
            return userSessions.get(userId);
        } catch (Exception e) {
            log.error("获取用户会话时发生错误，用户ID: {}", userId, e);
            return null;
        }
    }

    /**
     * 初始化屏幕共享会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    public void initScreenShareSession(String sessionId, String userId) {
        try {
            if (sessionId == null || sessionId.isEmpty() || userId == null || userId.isEmpty()) {
                log.warn("尝试初始化屏幕共享会话，但会话ID或用户ID为空");
                return;
            }
            
            screenShareSessions.put(sessionId, false); // 初始状态为未开始共享
            userSessions.put(userId, sessionId);
            log.info("初始化屏幕共享会话: {}，用户: {}", sessionId, userId);
        } catch (Exception e) {
            log.error("初始化屏幕共享会话时发生错误，会话ID: {}，用户ID: {}", sessionId, userId, e);
        }
    }
    
    /**
     * 启动屏幕共享会话
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    public void startScreenShare(String sessionId, String userId) {
        try {
            if (sessionId == null || sessionId.isEmpty() || userId == null || userId.isEmpty()) {
                log.warn("尝试启动屏幕共享会话，但会话ID或用户ID为空");
                return;
            }
            
            screenShareSessions.put(sessionId, true);
            userSessions.put(userId, sessionId);
            log.info("用户{}开始屏幕共享，会话ID: {}", userId, sessionId);
        } catch (Exception e) {
            log.error("启动屏幕共享会话时发生错误，会话ID: {}，用户ID: {}", sessionId, userId, e);
        }
    }
    
    /**
     * 检查用户是否正在进行屏幕共享
     * @param userId 用户ID
     * @return 是否正在进行屏幕共享
     */
    public boolean isUserScreenSharing(String userId) {
        try {
            if (userId == null || userId.isEmpty()) {
                log.warn("尝试检查空用户ID的屏幕共享状态");
                return false;
            }
            
            // 检查该用户是否有活跃的屏幕共享会话
            String sessionId = userSessions.get(userId);
            if (sessionId != null) {
                Boolean isScreenSharing = screenShareSessions.get(sessionId);
                if (Boolean.TRUE.equals(isScreenSharing)) {
                    WebrtcSession session = sessions.get(sessionId);
                    if (session != null && 
                        session.getStatus() != WebrtcSession.SessionStatus.DISCONNECTED &&
                        session.getStatus() != WebrtcSession.SessionStatus.REJECTED &&
                        session.getStatus() != WebrtcSession.SessionStatus.TIMEOUT) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("检查用户屏幕共享状态时发生错误，用户ID: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 检查指定会话是否正在进行屏幕共享
     * @param sessionId 会话ID
     * @return 是否正在进行屏幕共享
     */
    public boolean isScreenSharing(String sessionId) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试检查空会话ID的屏幕共享状态");
                return false;
            }
            
            return Boolean.TRUE.equals(screenShareSessions.get(sessionId));
        } catch (Exception e) {
            log.error("检查会话屏幕共享状态时发生错误，会话ID: {}", sessionId, e);
            return false;
        }
    }

    /**
     * 停止屏幕共享会话
     * @param sessionId 会话ID
     */
    public void stopScreenShare(String sessionId) {
        try {
            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("尝试停止空会话ID的屏幕共享");
                return;
            }
            
            screenShareSessions.put(sessionId, false);
            log.info("会话{}的屏幕共享已停止", sessionId);
        } catch (Exception e) {
            log.error("停止屏幕共享会话时发生错误，会话ID: {}", sessionId, e);
        }
    }

}