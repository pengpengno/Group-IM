package com.github.im.server.controller;

import com.github.im.server.service.WebrtcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webrtc/users")
public class RtcUserController {

    @Autowired
    private WebrtcService webrtcService;

    /**
     * 检查用户是否在线和可用
     * @param userId 用户ID
     * @return 用户状态信息
     */
    @GetMapping("/{userId}/status")
    public ResponseEntity<Map<String, Object>> getUserStatus(@PathVariable String userId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean hasActiveSession = webrtcService.hasActiveSession(userId);

            response.put("success", true);
            response.put("userId", userId);
            response.put("online", true); // 假设能连接到这个接口的用户都是在线的
            response.put("available", !hasActiveSession);
            response.put("status", hasActiveSession ? "BUSY" : "AVAILABLE");

            log.debug("检查用户{}状态: 可用={}", userId, !hasActiveSession);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("检查用户状态时出错", e);
            response.put("success", false);
            response.put("message", "检查用户状态失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取用户列表（简化版）
     * @return 用户列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getUserList() {
        Map<String, Object> response = new HashMap<>();

        try {
            // 这里应该从UserService获取实际的用户列表
            // 现在我们返回一个示例列表
            response.put("success", true);
            response.put("users", new String[]{"user1", "user2", "user3"});

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取用户列表时出错", e);
            response.put("success", false);
            response.put("message", "获取用户列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 获取在线用户列表
     * @return 在线用户列表
     */
    @GetMapping("/online")
    public ResponseEntity<Map<String, Object>> getOnlineUsers() {
        Map<String, Object> response = new HashMap<>();

        try {
            java.util.Set<String> onlineUsers = webrtcService.getOnlineUsers();

            response.put("success", true);
            response.put("onlineUsers", onlineUsers);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取在线用户列表时出错", e);
            response.put("success", false);
            response.put("message", "获取在线用户列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}