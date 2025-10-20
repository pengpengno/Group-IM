package com.github.im.server.controller;

import com.github.im.server.model.WebrtcSession;
import com.github.im.server.service.WebrtcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webrtc")
public class WebrtcController {

    @Autowired
    private WebrtcService webrtcService;
    
    /**
     * 通过用户ID发起呼叫
     * @param calleeId 被叫方用户ID
     * @param principal 当前用户主体
     * @return 呼叫结果
     */
    @PostMapping("/call-user/{calleeId}")
    public ResponseEntity<Map<String, Object>> callUser(
            @PathVariable String calleeId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String callerId = principal.getName();
            
            // 检查被叫方是否在线并且没有活动会话
            if (!webrtcService.hasActiveSession(calleeId)) {
                response.put("success", false);
                response.put("message", "用户不在线或不可用");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建会话
            String sessionId = webrtcService.createSession(callerId, calleeId);
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("message", "呼叫已发起，请等待对方接听");
            
            log.info("用户{}向用户{}发起呼叫，会话ID: {}", callerId, calleeId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发起呼叫时出错", e);
            response.put("success", false);
            response.put("message", "发起呼叫失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 初始化呼叫
     * @param calleeId 被叫方用户ID
     * @param principal 当前用户主体
     * @return 呼叫结果
     */
    @PostMapping("/call/{calleeId}")
    public ResponseEntity<Map<String, Object>> initiateCall(
            @PathVariable Long calleeId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Long callerId = Long.valueOf(principal.getName());
            
            // 检查被叫方是否已经有活动会话
            if (webrtcService.hasActiveSession(calleeId.toString())) {
                response.put("success", false);
                response.put("message", "用户正在通话中");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建会话
            String sessionId = webrtcService.createSession(callerId, calleeId);
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("message", "呼叫已发起");
            
            log.info("用户{}向用户{}发起呼叫，会话ID: {}", callerId, calleeId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("初始化呼叫时出错", e);
            response.put("success", false);
            response.put("message", "呼叫初始化失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取会话信息
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionInfo(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在");
                return ResponseEntity.badRequest().body( response);
            }
            
            response.put("success", true);
            response.put("session", session);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取会话信息时出错", e);
            response.put("success", false);
            response.put("message", "获取会话信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 接受呼叫
     * @param sessionId 会话ID
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/session/{sessionId}/accept")
    public ResponseEntity<Map<String, Object>> acceptCall(
            @PathVariable String sessionId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证用户权限（只有被叫方可以接受呼叫）
            String userId = principal.getName();
            if (!session.getCalleeId().toString().equals(userId)) {
                response.put("success", false);
                response.put("message", "无权限操作此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新会话状态为已连接
            session.setStatus(WebrtcSession.SessionStatus.CONNECTED);
            
            response.put("success", true);
            response.put("message", "呼叫已接受，可以开始通信");
            
            log.info("用户{}接受了来自用户{}的呼叫，会话ID: {}", userId, session.getCallerId(), sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("接受呼叫时出错", e);
            response.put("success", false);
            response.put("message", "接受呼叫失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 拒绝呼叫
     * @param sessionId 会话ID
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/session/{sessionId}/reject")
    public ResponseEntity<Map<String, Object>> rejectCall(
            @PathVariable String sessionId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证用户权限（只有被叫方可以拒绝呼叫）
            String userId = principal.getName();
            if (!session.getCalleeId().toString().equals(userId)) {
                response.put("success", false);
                response.put("message", "无权限操作此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新会话状态为已拒绝
            session.setStatus(WebrtcSession.SessionStatus.REJECTED);
            
            response.put("success", true);
            response.put("message", "呼叫已拒绝");
            
            log.info("用户{}拒绝了来自用户{}的呼叫，会话ID: {}", userId, session.getCallerId(), sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("拒绝呼叫时出错", e);
            response.put("success", false);
            response.put("message", "拒绝呼叫失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 结束会话
     * @param sessionId 会话ID
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/session/{sessionId}/end")
    public ResponseEntity<Map<String, Object>> endSession(
            @PathVariable String sessionId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证用户权限
            Long userId = Long.valueOf(principal.getName());
            if (!session.getCallerId().equals(userId) && !session.getCalleeId().equals(userId)) {
                response.put("success", false);
                response.put("message", "无权限操作此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 更新会话状态
            session.setStatus(WebrtcSession.SessionStatus.DISCONNECTED);
            
            response.put("success", true);
            response.put("message", "会话已结束");
            
            log.info("用户{}结束会话{}", userId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("结束会话时出错", e);
            response.put("success", false);
            response.put("message", "结束会话失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 发送WebRTC Offer
     * @param sessionId 会话ID
     * @param offer SDP Offer
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/session/{sessionId}/offer")
    public ResponseEntity<Map<String, Object>> sendOffer(
            @PathVariable String sessionId,
            @RequestBody String offer,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证用户权限
            String userId = principal.getName();
            if (!session.getCallerId().toString().equals(userId)) {
                response.put("success", false);
                response.put("message", "无权限操作此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 发送offer
            webrtcService.sendOffer(sessionId, offer);
            
            response.put("success", true);
            response.put("message", "Offer已发送");
            
            log.info("用户{}在会话{}中发送了Offer", userId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送Offer时出错", e);
            response.put("success", false);
            response.put("message", "发送Offer失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 发送WebRTC Answer
     * @param sessionId 会话ID
     * @param answer SDP Answer
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/session/{sessionId}/answer")
    public ResponseEntity<Map<String, Object>> sendAnswer(
            @PathVariable String sessionId,
            @RequestBody String answer,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 验证用户权限
            String userId = principal.getName();
            if (!session.getCalleeId().toString().equals(userId)) {
                response.put("success", false);
                response.put("message", "无权限操作此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 发送answer
            webrtcService.sendAnswer(sessionId, answer);
            
            response.put("success", true);
            response.put("message", "Answer已发送");
            
            log.info("用户{}在会话{}中发送了Answer", userId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("发送Answer时出错", e);
            response.put("success", false);
            response.put("message", "发送Answer失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 添加ICE候选
     * @param sessionId 会话ID
     * @param candidate ICE候选
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/session/{sessionId}/ice-candidate")
    public ResponseEntity<Map<String, Object>> addIceCandidate(
            @PathVariable String sessionId,
            @RequestBody String candidate,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            WebrtcSession session = webrtcService.getSession(sessionId);
            if (session == null) {
                response.put("success", false);
                response.put("message", "会话不存在或已过期");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 添加ICE候选（任何一方都可以）
            webrtcService.addIceCandidate(sessionId, candidate);
            
            response.put("success", true);
            response.put("message", "ICE候选已添加");
            
            log.debug("在会话{}中添加了ICE候选", sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("添加ICE候选时出错", e);
            response.put("success", false);
            response.put("message", "添加ICE候选失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 初始化屏幕共享
     * @param principal 当前用户主体
     * @return 屏幕共享会话信息
     */
    @PostMapping("/screen-share/init")
    public ResponseEntity<Map<String, Object>> initScreenShare(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = principal.getName();
            
            // 创建一个专门用于屏幕共享的会话
            String sessionId = "screen_" + java.util.UUID.randomUUID().toString();
            
            // 在服务中注册屏幕共享会话
            webrtcService.initScreenShareSession(sessionId, userId);
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("userId", userId);
            response.put("message", "屏幕共享已初始化");
            
            log.info("用户{}初始化了屏幕共享，会话ID: {}", userId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("初始化屏幕共享时出错", e);
            response.put("success", false);
            response.put("message", "初始化屏幕共享失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 开始屏幕共享
     * @param sessionId 会话ID
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/screen-share/{sessionId}/start")
    public ResponseEntity<Map<String, Object>> startScreenShare(
            @PathVariable String sessionId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = principal.getName();
            
            // 检查是否是屏幕共享会话
            if (!sessionId.startsWith("screen_")) {
                response.put("success", false);
                response.put("message", "无效的屏幕共享会话ID");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 启动屏幕共享
            webrtcService.startScreenShare(sessionId, userId);
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("message", "屏幕共享已开始");
            
            log.info("用户{}开始屏幕共享，会话ID: {}", userId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("开始屏幕共享时出错", e);
            response.put("success", false);
            response.put("message", "开始屏幕共享失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 停止屏幕共享
     * @param sessionId 会话ID
     * @param principal 当前用户主体
     * @return 结果
     */
    @PostMapping("/screen-share/{sessionId}/stop")
    public ResponseEntity<Map<String, Object>> stopScreenShare(
            @PathVariable String sessionId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = principal.getName();
            
            // 检查会话是否属于当前用户
            String userSessionId = webrtcService.getUserSession(userId);
            if (userSessionId == null || !userSessionId.equals(sessionId)) {
                response.put("success", false);
                response.put("message", "无权限操作此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 停止屏幕共享
            webrtcService.stopScreenShare(sessionId);
            
            response.put("success", true);
            response.put("message", "屏幕共享已停止");
            
            log.info("用户{}停止了屏幕共享，会话ID: {}", userId, sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("停止屏幕共享时出错", e);
            response.put("success", false);
            response.put("message", "停止屏幕共享失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取屏幕共享状态
     * @param sessionId 会话ID
     * @param principal 当前用户主体
     * @return 状态信息
     */
    @GetMapping("/screen-share/{sessionId}/status")
    public ResponseEntity<Map<String, Object>> getScreenShareStatus(
            @PathVariable String sessionId,
            Principal principal) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = principal.getName();
            
            // 检查会话是否属于当前用户
            String userSessionId = webrtcService.getUserSession(userId);
            if (userSessionId == null || !userSessionId.equals(sessionId)) {
                response.put("success", false);
                response.put("message", "无权限查看此会话");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 获取会话状态
            boolean isScreenSharing = Boolean.TRUE.equals(webrtcService.isScreenSharing(sessionId));
            
            response.put("success", true);
            response.put("sessionId", sessionId);
            response.put("isSharing", isScreenSharing);
            response.put("message", isScreenSharing ? "正在共享屏幕" : "屏幕共享已停止");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取屏幕共享状态时出错", e);
            response.put("success", false);
            response.put("message", "获取状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 检查屏幕共享权限
     * @param principal 当前用户主体
     * @return 权限检查结果
     */
    @GetMapping("/screen-share/check-permission")
    public ResponseEntity<Map<String, Object>> checkScreenSharePermission(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String userId = principal.getName();
            
            // 检查用户是否已经在进行屏幕共享
            boolean isScreenSharing = webrtcService.isUserScreenSharing(userId);
            
            response.put("success", true);
            response.put("userId", userId);
            response.put("canShareScreen", !isScreenSharing);
            response.put("message", isScreenSharing ? "您正在屏幕共享中" : "可以开始屏幕共享");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查屏幕共享权限时出错", e);
            response.put("success", false);
            response.put("message", "检查权限失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 检查用户状态
     * @param userId 用户ID
     * @return 用户状态
     */
    @GetMapping("/user/{userId}/status")
    public ResponseEntity<Map<String, Object>> getOnlineUserStatus(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean hasActiveSession = webrtcService.hasActiveSession(userId.toString());
            
            response.put("success", true);
            response.put("userId", userId);
            response.put("available", !hasActiveSession);
            response.put("status", hasActiveSession ? "BUSY" : "AVAILABLE");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查用户状态时出错", e);
            response.put("success", false);
            response.put("message", "检查用户状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}