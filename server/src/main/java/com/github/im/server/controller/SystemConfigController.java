package com.github.im.server.controller;

import com.github.im.server.model.User;
import com.github.im.server.service.SystemConfigService;
import com.github.im.server.web.ApiResponse;
import com.github.im.server.web.ResponseUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system-config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping("/media-policy")
    public ResponseEntity<ApiResponse<SystemConfigService.MediaRuntimePolicy>> getMediaPolicy() {
        return ResponseUtil.success("Media policy fetched successfully", systemConfigService.getMediaRuntimePolicy());
    }

    @GetMapping("/admin/groups")
    public ResponseEntity<ApiResponse<Object>> getAdminConfigGroups(@AuthenticationPrincipal User user) {
        if (!isAdminUser(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Only admin can access system configuration"));
        }
        return ResponseUtil.success("System config loaded", systemConfigService.getAdminConfigGroups());
    }

    @PutMapping("/admin/media")
    public ResponseEntity<ApiResponse<Object>> updateMediaConfig(@AuthenticationPrincipal User user,
                                                                 @RequestBody UpdateSystemConfigRequest request) {
        if (!isAdminUser(user)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(HttpStatus.FORBIDDEN.value(), "Only admin can update system configuration"));
        }
        try {
            systemConfigService.updateMediaConfig(request.getValues());
            return ResponseUtil.success("Media config updated", systemConfigService.getAdminConfigGroups());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
        }
    }

    private boolean isAdminUser(User user) {
        return user != null && "admin".equals(user.getUsername());
    }

    @Data
    public static class UpdateSystemConfigRequest {
        private Map<String, String> values;
    }
}
