package com.github.im.server.ai.mcp;

import java.util.Map;

/** Immutable public contract for a robot-callable capability. */
public record McpToolDescriptor(
        String name,
        String description,
        McpToolRisk risk,
        Map<String, Object> inputSchema
) {
    public McpToolDescriptor {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("MCP tool name is required");
        }
        risk = risk == null ? McpToolRisk.READ_ONLY : risk;
        inputSchema = inputSchema == null ? Map.of() : Map.copyOf(inputSchema);
    }
}
