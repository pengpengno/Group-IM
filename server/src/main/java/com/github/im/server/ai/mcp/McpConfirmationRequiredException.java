package com.github.im.server.ai.mcp;

public class McpConfirmationRequiredException extends RuntimeException {
    public McpConfirmationRequiredException(String toolName) {
        super("Confirmation is required before invoking: " + toolName);
    }
}
