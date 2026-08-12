package com.github.im.server.controller;
import com.github.im.server.model.ConversationBotConfig;
import com.github.im.server.model.User;
import com.github.im.server.service.ConversationBotConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/bots/conversations") @RequiredArgsConstructor
public class ConversationBotConfigController {
    private final ConversationBotConfigService service;
    @GetMapping("/{conversationId}/config") public ResponseEntity<Response> get(@PathVariable Long conversationId) { return ResponseEntity.ok(toResponse(service.getOrCreate(conversationId))); }
    @PutMapping("/{conversationId}/config") public ResponseEntity<Response> update(@PathVariable Long conversationId, @RequestBody Request request, @AuthenticationPrincipal User user) { return ResponseEntity.ok(toResponse(service.update(user.getUserId(), conversationId, request.enabled(), request.promptTemplate()))); }
    private Response toResponse(ConversationBotConfig config) { return new Response(String.valueOf(config.getConversation().getConversationId()), config.isEnabled(), config.getPromptTemplate()); }
    public record Request(boolean enabled, String promptTemplate) { } public record Response(String conversationId, boolean enabled, String promptTemplate) { }
}
