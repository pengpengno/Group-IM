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
     * 检查用户状态
     * @param userId 用户ID
     * @return 用户状态
     */
    @GetMapping("/user/{userId}/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable Long userId) {
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