package com.github.im.server.controller;

import com.github.im.dto.notification.PushEndpointDTO;
import com.github.im.dto.notification.PushEndpointUpsertRequest;
import com.github.im.server.model.User;
import com.github.im.server.service.notification.PushEndpointService;
import com.github.im.server.web.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/push/endpoints")
@RequiredArgsConstructor
public class PushEndpointController {

    private final PushEndpointService pushEndpointService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PushEndpointDTO>>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(pushEndpointService.list(user.getUserId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PushEndpointDTO>> upsert(
            @RequestBody PushEndpointUpsertRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success(pushEndpointService.register(user.getUserId(), request)));
    }

    @DeleteMapping("/{endpointId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String endpointId,
            @AuthenticationPrincipal User user
    ) {
        pushEndpointService.delete(user.getUserId(), endpointId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
