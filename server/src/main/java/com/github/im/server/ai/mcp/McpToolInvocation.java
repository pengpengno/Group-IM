package com.github.im.server.ai.mcp;

import java.util.Map;

public record McpToolInvocation(
        String toolName,
        Map<String, Object> arguments,
        BotToolContext context
) {
    public McpToolInvocation {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("MCP tool name is required");
        }
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        if (context == null || !context.hasAuthenticatedActor()) {
            throw new IllegalArgumentException("An authenticated bot tool context is required");
        }
    }
}
