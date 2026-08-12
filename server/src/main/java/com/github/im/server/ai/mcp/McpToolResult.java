package com.github.im.server.ai.mcp;

import java.util.Map;

/** Structured result kept separate from safe display text sent back to the chat. */
public record McpToolResult(
        Map<String, Object> structuredContent,
        String displayText,
        String errorCode
) {
    public McpToolResult {
        structuredContent = structuredContent == null ? Map.of() : Map.copyOf(structuredContent);
        displayText = displayText == null ? "" : displayText;
    }

    public static McpToolResult success(Map<String, Object> content, String displayText) {
        return new McpToolResult(content, displayText, null);
    }

    public static McpToolResult error(String errorCode, String displayText) {
        return new McpToolResult(Map.of(), displayText, errorCode);
    }

    public boolean isSuccess() {
        return errorCode == null || errorCode.isBlank();
    }
}
