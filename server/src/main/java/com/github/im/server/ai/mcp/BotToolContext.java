package com.github.im.server.ai.mcp;

/**
 * Server-derived identity and scope for a tool call. Never populate this from
 * model output or an untrusted webhook payload.
 */
public record BotToolContext(
        Long actorUserId,
        Long conversationId,
        String tenantId,
        String requestId,
        boolean confirmationGranted
) {
    public boolean hasAuthenticatedActor() {
        return actorUserId != null && actorUserId > 0;
    }
}
